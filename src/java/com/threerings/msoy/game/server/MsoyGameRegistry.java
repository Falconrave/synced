//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsDObjectMgr;
import com.threerings.presents.util.PersistingUnit;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.parlor.game.data.GameCodes;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.person.data.GameAwardPayload;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.StaticMediaDesc;
import com.threerings.msoy.item.server.ItemManager.ItemUpdateListener;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;

import com.threerings.msoy.peer.client.PeerGameService;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.data.PeerGameMarshaller;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.peer.server.PeerGameDispatcher;
import com.threerings.msoy.peer.server.PeerGameProvider;

import com.threerings.msoy.game.client.GameServerService;
import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.all.Trophy;

import static com.threerings.msoy.Log.log;

/**
 * Manages the process of starting up external game server processes and coordinating with them as
 * they host lobbies and games.
 */
public class MsoyGameRegistry
    implements MsoyGameProvider, GameServerProvider, MsoyServer.Shutdowner, PeerGameProvider
{
    /** The invocation services group for game server services. */
    public static final String GAME_SERVER_GROUP = "game_server";

    /** A predefined game record for our tutorial game. */
    public static final Game TUTORIAL_GAME = new Game() {
        /* implicit constructor */ {
            this.gameId = TUTORIAL_GAME_ID;
            this.name = "Whirled Tutorial";
            this.config = "<avrg/>";
            this.gameMedia = new StaticMediaDesc(
                MediaDesc.APPLICATION_SHOCKWAVE_FLASH, Item.GAME, "tutorial");
            // TODO: if we end up using these for AVRG's we'll want hand-crafted stuffs here
            this.thumbMedia = getDefaultThumbnailMediaFor(GAME);
            this.furniMedia = getDefaultFurniMediaFor(GAME);
        }
    };

    /**
     * Initializes this registry.
     */
    public void init (InvocationManager invmgr, GameRepository gameRepo)
        throws PersistenceException
    {
        _gameRepo = gameRepo;
        invmgr.registerDispatcher(new MsoyGameDispatcher(this), MsoyCodes.GAME_GROUP);
        invmgr.registerDispatcher(new GameServerDispatcher(this), GAME_SERVER_GROUP);

        // register to hear when the server is shutdown
        MsoyServer.registerShutdowner(this);

        // register and initialize our peer game service
        ((MsoyNodeObject)MsoyServer.peerMan.getNodeObject()).setPeerGameService(
            (PeerGameMarshaller)invmgr.registerDispatcher(new PeerGameDispatcher(this)));

        // start up our servers after the rest of server initialization is completed (and we know
        // that we're listening for client connections)
        MsoyServer.omgr.postRunnable(new PresentsDObjectMgr.LongRunnable() {
            public void run () {
                // start up our game server handlers (and hence our game servers)
                for (int ii = 0; ii < _handlers.length; ii++) {
                    int port = ServerConfig.gameServerPort + ii;
                    try {
                        _handlers[ii] = new GameServerHandler(port);
                    } catch (Exception e) {
                        log.log(Level.WARNING, "Failed to start up game server " +
                                "[port=" + port + "].", e);
                    }
                }
            }
        });

        // tell the item manager we want to know about game updates
        MsoyServer.itemMan.registerItemUpdateListener(GameRecord.class, new GameUpdateListener());
    }

    // from interface MsoyGameProvider
    public void locateGame (ClientObject caller, final int gameId,
                            MsoyGameService.LocationListener listener)
        throws InvocationException
    {
        // if we're already hosting this game, then report back immediately
        GameServerHandler handler = _handmap.get(gameId);
        if (handler != null) {
            listener.gameLocated(ServerConfig.serverHost, handler.port);
            return;
        }

        // otherwise check to see if someone else is hosting this game
        if (checkAndSendToNode(gameId, listener)) {
            return;
        }

        // we're going to need the Game item to finish resolution
        MsoyServer.invoker.postUnit(new PersistingUnit("locateGame", listener) {
            public void invokePersistent () throws PersistenceException {
                if (gameId == Game.TUTORIAL_GAME_ID) {
                    _game = TUTORIAL_GAME;
                } else {
                    GameRecord grec = _gameRepo.loadGameRecord(gameId);
                    _game = (grec == null) ? null : (Game)grec.toItem();
                }
            }
            public void handleSuccess () {
                if (_game == null) {
                    log.warning("Requested to locate unknown game [id=" + gameId + "].");
                    _listener.requestFailed(GameCodes.INTERNAL_ERROR);
                } else {
                    lockGame(_game, (MsoyGameService.LocationListener)_listener);
                }
            }
            protected Game _game;
        });
    }

    // from interface MsoyGameProvider
    public void inviteFriends (ClientObject caller, final int gameId, final int[] friendIds)
    {
        final MemberObject memobj = (MemberObject)caller;

        // sanity check; if this breaks some day in real usage, I will be amused
        if (friendIds.length > 255) {
            log.warning("Received crazy invite friends request [from=" + memobj.who() +
                        ", gameId=" + gameId + ", friendCount=" + friendIds.length + "].");
            return;
        }

        // check that friends are caller's friends? do we care? maybe we want to allow invites from
        // anyone not just friends...

        // load up the game's name; hello database!
        String name = "inviteFriends(" + gameId + ", " + StringUtil.toString(friendIds) + ")";
        MsoyServer.invoker.postUnit(new RepositoryUnit(name) {
            public void invokePersist () throws Exception {
                GameRecord grec = MsoyServer.itemMan.getGameRepository().loadGameRecord(gameId);
                if (grec == null) {
                    throw new Exception("No record for game."); // the standard logging is good
                }
                _game = grec.name;
            }
            public void handleSuccess () {
                for (int friendId : friendIds) {
                    MsoyServer.peerMan.invokeNodeAction(
                        new InviteNodeAction(friendId, memobj.memberName, gameId, _game));
                }
            }
            protected String _game;
        });
    }

    // from interface GameServerProvider
    public void sayHello (ClientObject caller, int port)
    {
        if (!checkCallerAccess(caller, "sayHello(" + port + ")")) {
            return;
        }

        for (GameServerHandler handler : _handlers) {
            if (handler != null && handler.port == port) {
                handler.setClientObject(caller);
                return;
            }
        }

        log.warning("Got hello from unknown game server [port=" + port + "].");
    }

    // from interface GameServerProvider
    public void leaveAVRGame (ClientObject caller, final int playerId)
    {
        if (!checkCallerAccess(caller, "leaveAVRGame(" + playerId + ")")) {
            return;
        }

        MemberObject memobj = MsoyServer.lookupMember(playerId);
        if (memobj != null) {
            peerLeaveAVRGame(null, playerId);
        }

        applyToNodes(playerId, new GameServiceOperation() {
            public void execute (Client client, PeerGameService service) {
                service.peerLeaveAVRGame(client, playerId);
            }
        });
    }

    // from interface GameServerProvider
    public void updatePlayer (ClientObject caller, final int playerId, final GameSummary game)
    {
        if (!checkCallerAccess(caller, "updatePlayer(" + playerId + ")")) {
            return;
        }

        MemberObject memobj = MsoyServer.lookupMember(playerId);
        if (memobj != null) {
            peerUpdatePlayer(null, playerId, game);
        }

        applyToNodes(playerId, new GameServiceOperation() {
            public void execute (Client client, PeerGameService service) {
                service.peerUpdatePlayer(client, playerId, game);
            }
        });
    }

    // from interface GameServerProvider
    public void reportFlowAward (ClientObject caller, final int memberId, final int deltaFlow)
    {
        if (!checkCallerAccess(caller, "reportFlowAward(" + memberId + ", " + deltaFlow + ")")) {
            return;
        }

        MemberObject mobj = MsoyServer.lookupMember(memberId);
        if (mobj != null) {
            peerReportFlowAward(null, memberId, deltaFlow);
        }

        applyToNodes(memberId, new GameServiceOperation() {
            public void execute (Client client, PeerGameService service) {
                service.peerReportFlowAward(client, memberId, deltaFlow);
            }
        });
    }

    // from interface GameServerProvider
    public void clearGameHost (ClientObject caller, int port, int gameId)
    {
        if (!checkCallerAccess(caller, "clearGameHost(" + port + ", " + gameId + ")")) {
            return;
        }

        GameServerHandler handler = _handmap.remove(gameId);
        if (handler != null) {
            handler.clearGame(gameId);
        } else {
            log.warning("Game cleared by unknown handler? [port=" + port + ", id=" + gameId + "].");
        }
    }

    // from interface PeerGameProvider
    public void peerUpdatePlayer (ClientObject caller, int playerId, GameSummary game)
    {
        if (caller != null && !checkCallerAccess(caller, "updatePlayer(" + playerId + ")")) {
            return;
        }

        MemberObject memobj = MsoyServer.lookupMember(playerId);
        if (memobj == null) {
            // TODO: this method gets called with game == null and memobj == null constantly;
            // TODO: my suspicion is this happens when you log out and the AVRG clears as part
            // TODO: of your logging out -- but I am turning off logging in the specific case
            // TODO: of game == null until I can investigate.
            if (game != null) {
                log.warning("Player vanished, dropping playerUpdate [caller=" + caller +
                            ", player=" + playerId + ", game=" + game + "]");
            }
            return;
        }

        // note that they're now playing an AVRG
        memobj.setGame(game);
        if (game != null && game.avrGame) {
            memobj.setAvrGameId(game.gameId);

            // immediately let the room manager give us of control, if needed
            PlaceManager pmgr = MsoyServer.plreg.getPlaceManager(memobj.getPlaceOid());
            if (pmgr instanceof RoomManager) {
                ((RoomManager) pmgr).occupantEnteredAVRGame(memobj);
            }
        }

        // update their occupant info if they're in a scene
        MsoyServer.memberMan.updateOccupantInfo(memobj);

        // update their published location in our peer object
        MsoyServer.peerMan.updateMemberLocation(memobj);
    }

    // from interface PeerGameProvider
    public void peerLeaveAVRGame (ClientObject caller, int playerId)
    {
        if (!checkCallerAccess(caller, "peerLeaveAVRGame(" + playerId + ")")) {
            return;
        }

        MemberObject memobj = MsoyServer.lookupMember(playerId);
        if (memobj == null) {
            log.warning("Player vanished, dropping AVRG departure [caller=" + caller +
                        ", player=" + playerId + "]");
            return;
        }

        // clear their AVRG affiliation
        memobj.setAvrGameId(0);

        // immediately let the room manager relieve us of control, if needed
        PlaceManager pmgr = MsoyServer.plreg.getPlaceManager(memobj.getPlaceOid());
        if (pmgr instanceof RoomManager) {
            ((RoomManager) pmgr).occupantLeftAVRGame(memobj);
        }
    }

    // from interface PeerGameProvider
    public void peerReportFlowAward (ClientObject caller, int playerId, int deltaFlow)
    {
        if (!checkCallerAccess(caller, "peerReportFlowAward(" + playerId + ", " + deltaFlow + ")")) {
            return;
        }

        MemberObject mobj = MsoyServer.lookupMember(playerId);
        if (mobj == null) {
            log.warning("Player vanished, dropping flow award [caller=" + caller +
                        ", player=" + playerId + ", deltaFlow=" + deltaFlow + "]");
            return;
        }

        mobj.setFlow(mobj.flow + deltaFlow);
        mobj.setAccFlow(mobj.accFlow + deltaFlow);
    }

    // from interface PeerGameProvider
    public void gameRecordUpdated (ClientObject caller, final int gameId)
    {
        GameServerHandler handler = _handmap.get(gameId);
        if (handler == null) {
            log.info("Eek, handler vanished [gameId=" + gameId + "]");
            return;
        }
        handler.gameRecordUpdated(gameId);
    }

    // from interface GameServerProvider
    public void reportTrophyAward (
        ClientObject caller, int memberId, String gameName, Trophy trophy)
    {
        if (!checkCallerAccess(caller, "reportTrophyAward(" + memberId + ", " + gameName + ")")) {
            return;
        }

        // send them a mail message as well
        String subject = MsoyServer.msgMan.getBundle("server").get(
            "m.got_trophy_subject", trophy.name);
        String body = MsoyServer.msgMan.getBundle("server").get(
            "m.got_trophy_body", trophy.description);
        MsoyServer.mailMan.deliverMessage(
            // TODO: sender should be special system id
            memberId, memberId, subject, body, new GameAwardPayload(
                trophy.gameId, gameName, GameAwardPayload.TROPHY, trophy.name, trophy.trophyMedia),
            true, new ResultListener.NOOP<Void>());
    }

    // from interface GameServerProvider
    public void awardPrize (ClientObject caller, int memberId, int gameId, String gameName,
                            Prize prize, GameServerService.ResultListener listener)
        throws InvocationException
    {
        if (!checkCallerAccess(caller, "awardPrize(" + memberId + ", " + prize.ident + ")")) {
            return;
        }
        // pass the buck to the item manager
        MsoyServer.itemMan.awardPrize(
            memberId, gameId, gameName, prize, new ResultAdapter<Item>(listener));
    }

    // from interface MsoyServer.Shutdowner
    public void shutdown ()
    {
        // shutdown our game server handlers
        for (GameServerHandler handler : _handlers) {
            if (handler != null) {
                handler.shutdown();
            }
        }
    }

    protected void applyToNodes (int memberId, final GameServiceOperation op)
    {
        // locate the peer that is hosting this member and forward the request there
        final MemberName memkey = new MemberName(null, memberId);
        MsoyServer.peerMan.invokeOnNodes(new MsoyPeerManager.Function() {
            public void invoke (Client client, NodeObject nodeobj) {
                MsoyNodeObject msnobj = (MsoyNodeObject)nodeobj;
                if (msnobj.clients.containsKey(memkey)) {
                    op.execute(client, msnobj.peerGameService);
                }
            }
        });
    }

    protected boolean checkAndSendToNode (int gameId, MsoyGameService.LocationListener listener)
    {
        Tuple<String, Integer> rhost = MsoyServer.peerMan.getGameHost(gameId);
        if (rhost == null) {
            return false;
        }

        String hostname = MsoyServer.peerMan.getPeerPublicHostName(rhost.left);
        log.info("Sending game player to " + rhost.left + ":" + rhost.right + ".");
        listener.gameLocated(hostname, rhost.right);
        return true;
    }

    protected void lockGame (final Game game, final MsoyGameService.LocationListener listener)
    {
        // otherwise obtain a lock and resolve the game ourselves
        MsoyServer.peerMan.acquireLock(
            MsoyPeerManager.getGameLock(game.gameId), new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (MsoyServer.peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.info("Got lock, resolving " + game.name + ".");
                    hostGame(game, listener);

                } else if (nodeName != null) {
                    // some other peer got the lock before we could; send them there
                    log.info("Didn't get lock, going remote " + game.gameId + "@" + nodeName + ".");
                    if (!checkAndSendToNode(game.gameId, listener)) {
                        log.warning("Failed to acquire lock but no registered host for game!? " +
                                    "[id=" + game.gameId + "].");
                        listener.requestFailed(GameCodes.INTERNAL_ERROR);
                    }

                } else {
                    log.warning("Game lock acquired by null? [id=" + game.gameId + "].");
                    listener.requestFailed(GameCodes.INTERNAL_ERROR);
                }
            }

            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to acquire game resolution lock " +
                        "[id=" + game.gameId + "].", cause);
                listener.requestFailed(GameCodes.INTERNAL_ERROR);
            }
        });
    }

    protected void hostGame (Game game, MsoyGameService.LocationListener listener)
    {
        // TODO: load balance across our handlers if we ever have more than one
        GameServerHandler handler = _handlers[0];
        if (handler == null) {
            log.warning("Have no game servers, cannot handle game [id=" + game.gameId + "].");
            listener.requestFailed(GameCodes.INTERNAL_ERROR);

            // releases our lock on this game as we didn't end up hosting it
            MsoyServer.peerMan.releaseLock(MsoyPeerManager.getGameLock(game.gameId),
                                           new ResultListener.NOOP<String>());
            return;
        }

        // register this handler as handling this game
        handler.hostGame(game);
        _handmap.put(game.gameId, handler);

        listener.gameLocated(ServerConfig.serverHost, handler.port);
    }

    protected boolean checkCallerAccess (ClientObject caller, String method)
    {
        // peers will not have member objects and server local calls will be a null caller
        if (caller instanceof MemberObject) {
            log.warning("Rejecting non-peer caller of " + method +
                        " [who=" + ((MemberObject)caller).who() + "].");
            return false;
        }
        return true;
    }

    protected class GameUpdateListener implements ItemUpdateListener
    {
        public void itemUpdated (ItemRecord item)
        {
            // we're only interested in mutable games
            if (item.sourceId != 0) {
                return;
            }

            // alright, let's find out where this updated game might be hosted...
            final int gameId = ((GameRecord) item).gameId;

            // maybe locally? that'd be great.
            if (_handmap.containsKey(gameId)) {
                gameRecordUpdated(null, gameId);
                return;
            }
            // otherwise is it hosted on any world server's game peer(s)?
            MsoyServer.peerMan.invokeOnNodes(new MsoyPeerManager.Function() {
                public void invoke (Client client, NodeObject nodeobj) {
                    MsoyNodeObject msnobj = (MsoyNodeObject)nodeobj;
                    if (msnobj.hostedGames.containsKey(gameId)) {
                        // great, tell the world server what's up
                        msnobj.peerGameService.gameRecordUpdated(client, gameId);
                    }
                }
            });
        }
    }

    /** Used by {@link #applyToNodes}. */
    protected static interface GameServiceOperation
    {
        public void execute (Client client, PeerGameService service);
    }

    /** Handles communications with a delegate game server. */
    protected static class GameServerHandler
    {
        public int port;

        public GameServerHandler (int port) throws Exception {
            // make a note of our port
            this.port = port;

            // the rungame script explicitly redirects all output, so we don't need to worry about
            // this process's input or output streams
            Runtime.getRuntime().exec(new String[] {
                ServerConfig.serverRoot + "/bin/rungame",
                String.valueOf(port),
                // have the game server connect to us on our first port
                String.valueOf(ServerConfig.serverPorts[0]),
            });
        }

        public void setClientObject (ClientObject clobj) {
            _clobj = clobj;
            // TODO: if our client object is destroyed and we aren't shutting down, restart the
            // game server?
        }

        public void hostGame (Game game) {
            if (!_games.add(game.gameId)) {
                log.warning("Requested to host game that we're already hosting? [port=" + port +
                            ", game=" + game.gameId + "].");
            } else {
                MsoyServer.peerMan.gameDidStartup(game.gameId, game.name, port);
            }
        }

        public void clearGame (int gameId) {
            if (!_games.remove(gameId)) {
                log.warning("Requested to clear game that we're not hosting? [port=" + port +
                            ", game=" + gameId + "].");
            } else {
                MsoyServer.peerMan.gameDidShutdown(gameId);
            }
        }

        public void shutdown () {
            if (_clobj != null && _clobj.isActive()) {
                log.info("Shutting down game server " + port + "...");
                _clobj.postMessage(WorldServerClient.SHUTDOWN_MESSAGE);
            } else {
                log.info("Not shutting down game server " + port + "...");
            }
        }

        public void gameRecordUpdated (int gameId)
        {
            _clobj.postMessage(WorldServerClient.GAME_RECORD_UPDATED, gameId);
        }

        protected ClientObject _clobj;
        protected ArrayIntSet _games = new ArrayIntSet();
    }

    /** Handles dispatching invitations to users wherever they may be. */
    protected static class InviteNodeAction extends MemberNodeAction
    {
        public InviteNodeAction (int memberId, MemberName inviter, int gameId, String game) {
            super(memberId);
            _inviterId = inviter.getMemberId();
            _inviter = inviter.toString();
            _gameId = gameId;
            _game = game;
        }

        protected void execute (MemberObject tgtobj) {
            MsoyServer.notifyMan.notifyGameInvite(tgtobj, _inviter, _inviterId, _game, _gameId);
        }

        protected int _inviterId, _gameId;
        protected String _inviter, _game;
    }

    /** Used to load metadata for games. */
    protected GameRepository _gameRepo;

    /** Handlers for our delegate game servers. */
    protected GameServerHandler[] _handlers = new GameServerHandler[DELEGATE_GAME_SERVERS];

    /** Contains a mapping from gameId to handler for all game servers hosted on this machine. */
    protected HashIntMap<GameServerHandler> _handmap = new HashIntMap<GameServerHandler>();

    /** The number of delegate game servers to be started. */
    protected static final int DELEGATE_GAME_SERVERS = 1;
}
