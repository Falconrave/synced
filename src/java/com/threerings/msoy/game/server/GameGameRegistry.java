//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.crowd.server.PlaceManager;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultListenerList;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.whirled.game.data.GameData;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserActionDetails;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MsoyBaseServer;
import com.threerings.msoy.server.MsoyEventLogger;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemPackRecord;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.LevelPackRecord;
import com.threerings.msoy.item.server.persist.LevelPackRepository;
import com.threerings.msoy.item.server.persist.PrizeRecord;
import com.threerings.msoy.item.server.persist.PrizeRepository;
import com.threerings.msoy.item.server.persist.TrophySourceRecord;
import com.threerings.msoy.item.server.persist.TrophySourceRepository;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.server.AVRDispatcher;
import com.threerings.msoy.avrg.server.AVRGameManager;
import com.threerings.msoy.avrg.server.AVRProvider;
import com.threerings.msoy.avrg.server.persist.AVRGameRepository;
import com.threerings.msoy.avrg.server.persist.GameStateRecord;
import com.threerings.msoy.avrg.server.persist.PlayerGameStateRecord;
import com.threerings.msoy.avrg.server.persist.QuestStateRecord;

import com.threerings.msoy.game.data.GameContentOwnership;
import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.server.persist.TrophyRecord;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manages the lobbies active on this server.
 */
public class GameGameRegistry
    implements LobbyProvider, AVRProvider,
               MsoyGameServer.Shutdowner, LobbyManager.ShutdownObserver
{
    /**
     * Initializes this registry.
     */
    public void init (RootDObjectManager omgr, InvocationManager invmgr, PersistenceContext perCtx,
                      RatingRepository ratingRepo, MsoyEventLogger eventLog)
    {
        _omgr = omgr;
        _eventLog = eventLog;

        // create our various game-related repositories
        _gameRepo = new GameRepository(perCtx);
        _avrgRepo = new AVRGameRepository(perCtx);
        _trophyRepo = new TrophyRepository(perCtx);
        _ratingRepo = ratingRepo;

        _lpackRepo = new LevelPackRepository(perCtx);
        _ipackRepo = new ItemPackRepository(perCtx);
        _tsourceRepo = new TrophySourceRepository(perCtx);
        _prizeRepo = new PrizeRepository(perCtx);

        // register game-related bootstrap services
        invmgr.registerDispatcher(new LobbyDispatcher(this), MsoyCodes.GAME_GROUP);
        invmgr.registerDispatcher(new AVRDispatcher(this), MsoyCodes.WORLD_GROUP);

        // register to hear when the server is shutdown
        MsoyGameServer.registerShutdowner(this);
    }

    /**
     * Returns the game repository used to maintain our persistent data.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Returns an enumeration of all of the registered lobby managers.  This should only be
     * accessed on the dobjmgr thread and shouldn't be kept around across event dispatches.
     */
    public Iterator<LobbyManager> enumerateLobbyManagers ()
    {
        return _lobbies.values().iterator();
    }

    /**
     * Returns the percentiler for the specified game and score distribution. The percentiler may
     * be modified and when the lobby for the game in question is finally unloaded, the percentiler
     * will be written back out to the database.
     */
    public Percentiler getScoreDistribution (int gameId, boolean multiplayer)
    {
        return _distribs.get(multiplayer ? Math.abs(gameId) : -Math.abs(gameId));
    }

    /**
     * Resolves the item and level packs owned by the player in question for the specified game, as
     * well as trophies they have earned. This information will show up asynchronously, once the
     */
    public void resolveOwnedContent (final int gameId, final PlayerObject plobj)
    {
        // if we've already resolved content for this player, we are done
        if (plobj.isContentResolved(gameId)) {
            return;
        }

        // add our "already resolved" marker and then start resolving
        plobj.addToGameContent(new GameContentOwnership(gameId, GameData.RESOLVED_MARKER, ""));
        MsoyGameServer.invoker.postUnit(new RepositoryUnit("resolveOwnedContent") {
            public void invokePersist () throws Exception {
                // TODO: load level and item pack ownership
                _trophies = _trophyRepo.loadTrophyOwnership(gameId, plobj.getMemberId());
            }
            public void handleSuccess () {
                plobj.startTransaction();
                try {
                    addContent(GameData.LEVEL_DATA, _lpacks);
                    addContent(GameData.ITEM_DATA, _ipacks);
                    addContent(GameData.TROPHY_DATA, _trophies);
                } finally {
                    plobj.commitTransaction();
                }
            }
            protected void addContent (byte type, List<String> idents) {
                for (String ident : idents) {
                    plobj.addToGameContent(new GameContentOwnership(gameId, type, ident));
                }
            }
            protected String getFailureMessage () {
                return "Failed to resolve content [game=" + gameId + ", who=" + plobj.who() + "].";
            }

            protected List<String> _lpacks = new ArrayList<String>();
            protected List<String> _ipacks = new ArrayList<String>();
            protected List<String> _trophies;
        });
    }

    /**
     * On some world server, somebody updated a game record, and eventually it was determined
     * that this game server is hosting that game. Do what we can to refresh our notion of the
     * game's definition.
     */
    public void gameRecordUpdated (final int gameId)
    {
        // is this a lobbied game?
        final LobbyManager lmgr = _lobbies.get(gameId);
        if (lmgr != null) {
            MsoyGameServer.invoker.postUnit(new RepositoryUnit("reloadLobby") {
                public void invokePersist () throws PersistenceException {
                    // if so, recompile the game content from all its various sources
                    _content = assembleGameContent(gameId);
                }

                public void handleSuccess () {
                    // and then update the lobby with the content
                    lmgr.setGameContent(_content);
                    log.info("Reloaded lobbied game configuration [id=" + gameId + "]");
                }

                public void handleFailure (Exception e) {
                    // if anything goes wrong, we can just fall back on what was already there
                    log.warning("Failed to resolve game [id=" + gameId + "].", e);
                }

                protected GameContent _content;
            });

            return;
        }

        // see if it's a lobby game we're currently loading...
        ResultListenerList list = _loadingLobbies.get(gameId);
        if (list != null) {
            list.add(new InvocationService.ResultListener() {
                public void requestProcessed (Object result) {
                    gameRecordUpdated(gameId);
                }

                public void requestFailed (String cause) {/*ignore*/}
            });
            return;
        }

        // else is it an AVRG?
        final AVRGameManager amgr = _avrgManagers.get(gameId);
        if (amgr != null) {
            MsoyGameServer.invoker.postUnit(new RepositoryUnit("reloadAVRGame") {
                public void invokePersist () throws Exception {
                    if (gameId == Game.TUTORIAL_GAME_ID) {
                        log.warning("Asked to reload the tutorial. That makes no sense.");
                        return;
                    }
                    GameRecord gRec = _gameRepo.loadGameRecord(gameId);
                    if (gRec == null) {
                        throw new PersistenceException(
                            "Failed to find GameRecord [gameId=" + gameId + "]");
                    }
                    _game = (Game) gRec.toItem();
                }

                public void handleSuccess () {
                    amgr.updateGame(_game);
                }

                public void handleFailure (Exception pe) {
                    log.warning("Failed to resolve AVRGame [id=" + gameId + "].", pe);
                }

                protected Game _game;
            });
            return;
        }

        log.warning("Updated game record not, in the end, hosted by us [gameId=" + gameId + "]");
    }

    /**
     * Resets our in-memory percentiler for the specified game. This is triggered by a request from
     * our world server.
     */
    public void resetScorePercentiler (int gameId, boolean single)
    {
        log.info("Resetting in-memory percentiler [gameId=" + gameId + ", single=" + single + "].");
        _distribs.put(single ? -gameId : gameId, new Percentiler());
    }

    /**
     * Awards the supplied trophy and provides a {@link Trophy} instance on success to the supplied
     * listener or failure.
     */
    public void awardTrophy (final String gameName, final TrophyRecord trophy, String description,
                             InvocationService.ResultListener listener)
    {
        // create the trophy record we'll use to notify them of their award
        final Trophy trec = trophy.toTrophy();
        // fill in the description so that we can report that in the award email
        trec.description = description;

        MsoyGameServer.invoker.postUnit(new PersistingUnit("awardTrophy", listener) {
            public void invokePersistent () throws PersistenceException {
                // store the trophy in the database
                _trophyRepo.storeTrophy(trophy);
                // publish the trophy earning event to the member's feed
                MsoyGameServer.feedRepo.publishMemberMessage(
                    trophy.memberId, FeedMessageType.FRIEND_WON_TROPHY,
                    trophy.name + "\t" + trophy.gameId +
                    "\t" + MediaDesc.mdToString(trec.trophyMedia));
            }
            public void handleSuccess () {
                MsoyGameServer.worldClient.reportTrophyAward(trophy.memberId, gameName, trec);
                _eventLog.trophyEarned(trophy.memberId, trophy.gameId, trophy.ident);
                ((InvocationService.ResultListener)_listener).requestProcessed(trec);
            }
            protected String getFailureMessage () {
                return "Failed to store trophy " + trophy + ".";
            }
        });
    }

    /**
     * Called when a game was successfully finished with a payout. 
     * Right now just logs the results for posterity.
     */
    public void gamePayout (UserActionDetails info, Game game, int payout, int secondsPlayed)
    {
        _eventLog.gamePlayed(
            game.genre, game.gameId, game.itemId, payout, secondsPlayed, info.memberId);
    }
    
    // from AVRProvider
    public void activateGame (ClientObject caller, final int gameId,
                              final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final PlayerObject player = (PlayerObject) caller;

        InvocationService.ResultListener joinListener =
            new AVRGameJoinListener(player.getMemberId(), listener);

        ResultListenerList list = _loadingAVRGames.get(gameId);
        if (list != null) {
            list.add(joinListener);
            return;
        }

        AVRGameManager mgr = _avrgManagers.get(gameId);
        if (mgr != null) {
            joinListener.requestProcessed(mgr);
            return;
        }

        _loadingAVRGames.put(gameId, list = new ResultListenerList());
        list.add(joinListener);

        final AVRGameManager fmgr = new AVRGameManager(gameId, _avrgRepo);
        final AVRGameObject gameObj = fmgr.createGameObject();

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("activateAVRGame") {
            public void invokePersist () throws Exception {
                if (gameId == Game.TUTORIAL_GAME_ID) {
                    _content = new GameContent();
                    _content.game = MsoyGameRegistry.TUTORIAL_GAME;

                } else {
                    _content = assembleGameContent(gameId);
                }
                if (_content.game != null) {
                    _recs = _avrgRepo.getGameState(gameId);
                }
            }

            public void handleSuccess () {
                if (_content.game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                _omgr.registerObject(gameObj);
                fmgr.startup(gameObj, _content, _recs);

                _avrgManagers.put(gameId, fmgr);

                ResultListenerList list = _loadingAVRGames.remove(gameId);
                if (list != null) {
                    list.requestProcessed(fmgr);
                } else {
                    log.warning(
                        "No listeners when done activating AVRGame [gameId=" + gameId + "]");
                }
            }

            public void handleFailure (Exception pe) {
                log.warning("Failed to resolve AVRGame [id=" + gameId + "].", pe);
                reportFailure(pe.getMessage());
            }

            protected void reportFailure (String reason) {
                ResultListenerList list = _loadingAVRGames.remove(gameId);
                if (list != null) {
                    list.requestFailed(reason);
                } else {
                    log.warning(
                        "No listeners when failing AVRGame [gameId=" + gameId + "]");
                }
            }

            protected GameContent _content;
            protected List<GameStateRecord> _recs;
        });
    }

    // from AVRProvider
    public void deactivateGame (ClientObject caller, int gameId,
                                final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        PlayerObject player = (PlayerObject) caller;
        int playerId = player.getMemberId();

        // see if we are still just resolving the game
        ResultListenerList list = _loadingAVRGames.get(gameId);
        if (list != null) {
            // yep, so just remove our associated listener, and we're done
            for (InvocationService.ResultListener gameListener : list) {
                if (((AVRGameJoinListener) gameListener).getPlayerId() == playerId) {
                    list.remove(gameListener);
                    listener.requestProcessed();
                    return;
                }
            }
        }

        // get the corresponding manager
        AVRGameManager mgr = _avrgManagers.get(gameId);
        if (mgr != null) {
            mgr.removePlayer(player);
            MsoyGameServer.worldClient.leaveAVRGame(playerId);

        } else {
            log.warning("Tried to deactivate AVRG without manager [gameId=" + gameId + "]");
        }

        listener.requestProcessed();
    }

    // from LobbyProvider
    public void identifyLobby (ClientObject caller, final int gameId,
                               InvocationService.ResultListener listener)
        throws InvocationException
    {
        // if we're already resolving this lobby, add this listener to the list of those interested
        // in the outcome
        ResultListenerList list = _loadingLobbies.get(gameId);
        if (list != null) {
            list.add(listener);
            return;
        }

        // if the lobby is already resolved, we're good
        LobbyManager mgr = _lobbies.get(gameId);
        if (mgr != null) {
            listener.requestProcessed(mgr.getLobbyObject().getOid());
            return;
        }

        // otherwise we need to do the resolving
        _loadingLobbies.put(gameId, list = new ResultListenerList());
        list.add(listener);

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("loadLobby") {
            public void invokePersist () throws PersistenceException {
                _content = assembleGameContent(gameId);
                // load up the score distribution information for this game as well
                _single = _ratingRepo.loadPercentile(-Math.abs(gameId));
                _multi = _ratingRepo.loadPercentile(Math.abs(gameId));
            }

            public void handleSuccess () {
                if (_content.game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                LobbyManager lmgr = new LobbyManager(_omgr, GameGameRegistry.this);
                lmgr.setGameContent(_content);
                _lobbies.put(gameId, lmgr);

                ResultListenerList list = _loadingLobbies.remove(gameId);
                if (list != null) {
                    list.requestProcessed(lmgr.getLobbyObject().getOid());
                }

                // map this game's score distributions
                _distribs.put(-Math.abs(gameId), _single == null ? new Percentiler() : _single);
                _distribs.put(Math.abs(gameId), _multi == null ? new Percentiler() : _multi);
            }

            public void handleFailure (Exception pe) {
                log.warning("Failed to resolve game [id=" + gameId + "].", pe);
                reportFailure(InvocationCodes.E_INTERNAL_ERROR);
            }

            protected void reportFailure (String reason) {
                ResultListenerList list = _loadingLobbies.remove(gameId);
                if (list != null) {
                    list.requestFailed(reason);
                }

                // clear out the hosting record that our world server assigned to us when it sent
                // this client our way to resolve this game
                MsoyGameServer.worldClient.stoppedHostingGame(gameId);
            }

            protected GameContent _content;
            protected Percentiler _single, _multi;
        });
    }

    // from LobbyProvider
    public void playNow (final ClientObject caller, final int gameId, final int mode,
                         final InvocationService.ResultListener listener)
        throws InvocationException
    {
        // we need to make sure the lobby is resolved, so route through identifyLobby
        identifyLobby(caller, gameId, new InvocationService.ResultListener() {
            public void requestProcessed (Object result) {
                // now the lobby should be resolved
                LobbyManager mgr = _lobbies.get(gameId);
                if (mgr == null) {
                    log.warning("identifyLobby() returned non-failure but lobby manager " +
                                "disappeared [gameId=" + gameId + "].");
                    requestFailed(InvocationCodes.E_INTERNAL_ERROR);
                    return;
                }

                // if we're able to get them right into a game, return zero otherwise return the
                // lobby manager oid which will allow the client to display the lobby
                PlayerObject plobj = (PlayerObject)caller;
                boolean gameCreated;
                switch (mode) {
                default:
                case LobbyCodes.PLAY_NOW_SINGLE:
                    gameCreated = mgr.playNowSingle(plobj);
                    break;

                case LobbyCodes.PLAY_NOW_FRIENDS:
                    gameCreated = mgr.playNowMulti(plobj, true);
                    break;

                case LobbyCodes.PLAY_NOW_ANYONE:
                    gameCreated = mgr.playNowMulti(plobj, false);
                    break;
                }
                listener.requestProcessed(gameCreated ? 0 : result);
            }
            public void requestFailed (String cause) {
                listener.requestFailed(cause);
            }
        });
    }

    // from LobbyProvider
    public void joinPlayerGame (ClientObject caller, final int playerId,
                                InvocationService.ResultListener listener)
        throws InvocationException
    {
        PlayerObject player = MsoyGameServer.lookupPlayer(playerId);
        if (player == null) {
            listener.requestFailed("e.player_not_found");
            return;
        }

        // If they're not in a location, we can send that on immediately.
        int placeOid = player.getPlaceOid();
        if (placeOid == -1) {
            listener.requestProcessed(placeOid);
            return;
        }

        // Check to make sure the game that they're in is watchable
        PlaceManager plman = MsoyBaseServer.plreg.getPlaceManager(placeOid);
        if (plman == null) {
            log.warning(
                "Fetched null PlaceManager for player's current gameOid [" + placeOid + "]");
            listener.requestFailed("e.player_not_found");
            return;
        }
        MsoyGameConfig gameConfig = (MsoyGameConfig) plman.getConfig();
        MsoyMatchConfig matchConfig = (MsoyMatchConfig) gameConfig.getGameDefinition().match;
        if (matchConfig.unwatchable) {
            listener.requestFailed("e.unwatchable_game");
            return;
        }

        // Check to make sure the game that they're in is not private
        int gameId = gameConfig.getGameId();
        LobbyManager lmgr = _lobbies.get(gameId);
        if (lmgr == null) {
            log.warning("No lobby manager found for existing game! [" + gameId + "]");
            listener.requestFailed("e.player_not_found");
            return;
        }
        LobbyObject lobj = lmgr.getLobbyObject();
        for (Table table : lobj.tables) {
            if (table.gameOid == placeOid) {
                if (table.tconfig.privateTable) {
                    listener.requestFailed("e.private_game");
                    return;
                }
                break;
            }
        }

        // finally, hand off the game oid
        listener.requestProcessed(placeOid);
    }

    // from interface PresentsServer.Shutdowner
    public void shutdown ()
    {
        // shutdown our active lobbies
        for (LobbyManager lmgr : _lobbies.values().toArray(new LobbyManager[_lobbies.size()])) {
            lobbyDidShutdown(lmgr.getGame());
        }

        for (AVRGameManager amgr : _avrgManagers.values()) {
            amgr.shutdown();
        }
    }

    // from interface LobbyManager.ShutdownObserver
    public void lobbyDidShutdown (final Game game)
    {
        // destroy our record of that lobby
        _lobbies.remove(game.gameId);
        _loadingLobbies.remove(game.gameId); // just in case

        // let our world server know we're audi
        MsoyGameServer.worldClient.stoppedHostingGame(game.gameId);

        // flush any modified percentile distributions
        flushPercentiler(-Math.abs(game.gameId)); // single-player
        flushPercentiler(Math.abs(game.gameId)); // multiplayer
    }

    protected GameContent assembleGameContent (final int gameId)
        throws PersistenceException
    {
        GameContent content = new GameContent();
        content.detail = _gameRepo.loadGameDetail(gameId);
        GameRecord rec = _gameRepo.loadGameRecord(gameId, content.detail);
        if (rec != null) {
            content.game = (Game)rec.toItem();
            // load up our level and item packs
            for (LevelPackRecord record :
                     _lpackRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.lpacks.add((LevelPack)record.toItem());
            }
            for (ItemPackRecord record :
                     _ipackRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.ipacks.add((ItemPack)record.toItem());
            }
            // load up our trophy source items
            for (TrophySourceRecord record :
                     _tsourceRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.tsources.add((TrophySource)record.toItem());
            }
            // load up our prize items
            for (PrizeRecord record :
                     _prizeRepo.loadOriginalItemsBySuite(content.game.getSuiteId())) {
                content.prizes.add((Prize)record.toItem());
            }
        }
        return content;
    }

    protected void joinAVRGame (final int playerId, final AVRGameManager mgr,
                                final InvocationService.ResultListener listener)
    {
        final PlayerObject player = MsoyGameServer.lookupPlayer(playerId);
        if (player == null) {
            // they left while we were resolving the game, oh well
            return;
        }

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("joinAVRGame") {
            public void invokePersist () throws Exception {
                _questRecs = _avrgRepo.getQuests(mgr.getGameId(), playerId);
                _stateRecs = _avrgRepo.getPlayerGameState(mgr.getGameId(), playerId);
            }
            public void handleSuccess () {
                mgr.addPlayer(player, _questRecs, _stateRecs);
                listener.requestProcessed(mgr.getGameObject().getOid());
            }
            public void handleFailure (Exception pe) {
                log.warning("Unable to resolve player state [gameId=" +
                    mgr.getGameId() + ", player=" + playerId + "]", pe);
            }
            protected List<QuestStateRecord> _questRecs;
            protected List<PlayerGameStateRecord> _stateRecs;
        });
    }

    protected void flushPercentiler (final int gameId)
    {
        final Percentiler tiler = _distribs.remove(gameId);
        if (tiler == null || !tiler.isModified()) {
            return;
        }

        MsoyGameServer.invoker.postUnit(new Invoker.Unit("flushPercentiler") {
            public boolean invoke () {
                try {
                    _ratingRepo.updatePercentile(gameId, tiler);
                } catch (PersistenceException pe) {
                    log.warning("Failed to update score distribution " +
                            "[game=" + gameId + ", tiler=" + tiler + "].", pe);
                }
                return false;
            }
        });
    }

    protected final class AVRGameJoinListener
        implements InvocationService.ResultListener
    {
        protected AVRGameJoinListener (int player, InvocationService.ResultListener listener)
        {
            _listener = listener;
            _player = player;
        }

        public int getPlayerId ()
        {
            return _player;
        }

        public void requestProcessed (Object result) {
            joinAVRGame(_player, (AVRGameManager) result, _listener);
        }

        public void requestFailed (String cause) {
            _listener.requestFailed(cause);
        }

        protected final int _player;
        protected final InvocationService.ResultListener _listener;
    }

    /** The distributed object manager that we work with. */
    protected RootDObjectManager _omgr;

    /** We record metrics events to this fellow. */
    protected MsoyEventLogger _eventLog;

    /** Maps game id -> lobby. */
    protected IntMap<LobbyManager> _lobbies = new HashIntMap<LobbyManager>();

    /** Maps game id -> a mapping of various percentile distributions. */
    protected IntMap<Percentiler> _distribs = new HashIntMap<Percentiler>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loadingLobbies = new HashIntMap<ResultListenerList>();

    /** Maps game id -> manager for AVR games. */
    protected IntMap<AVRGameManager> _avrgManagers = new HashIntMap<AVRGameManager>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loadingAVRGames = new HashIntMap<ResultListenerList>();

    // various and sundry repositories for loading persistent data
    protected GameRepository _gameRepo;
    protected AVRGameRepository _avrgRepo;
    protected RatingRepository _ratingRepo;
    protected TrophyRepository _trophyRepo;
    protected LevelPackRepository _lpackRepo;
    protected ItemPackRepository _ipackRepo;
    protected TrophySourceRepository _tsourceRepo;
    protected PrizeRepository _prizeRepo;
}
