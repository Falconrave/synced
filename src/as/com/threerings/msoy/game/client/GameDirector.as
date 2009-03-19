//
// $Id$

package com.threerings.msoy.game.client {

import mx.styles.StyleManager;

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;

import com.threerings.crowd.client.PlaceController;

import com.threerings.flex.CommandMenu;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.utils.Args;
import com.threerings.msoy.utils.Base64Encoder;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.avrg.client.AVRGameBackend;
import com.threerings.msoy.avrg.client.AVRGameController;
import com.threerings.msoy.avrg.client.AVRGameLiaison;

import com.threerings.msoy.game.data.GameGameMarshaller;
import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.WorldGameMarshaller;

/**
 * A director that manages game related bits.
 */
public class GameDirector extends BasicDirector
{
    public const log :Log = Log.getLog(this);

    // statically reference classes we require
    MsoyGameDefinition;
    WorldGameMarshaller;
    GameGameMarshaller;

    public function GameDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Returns true if we're in a lobby or in a game.
     */
    public function isGaming () :Boolean
    {
        return (_liaison != null);
    }

    /**
     * Returns the gameId of the game we're currently connected to, or zero if we're not.
     */
    public function getGameId () :int
    {
        return (_liaison != null) ? _liaison.gameId : 0;
    }

    /**
     * Returns the name of the game we're currently connected to, or null if we're not.
     */
    public function getGameName () :String
    {
        return (_liaison != null) ? _liaison.gameName : null;
    }

    /**
     * Returns the currently active GameContext or null if no game is active.
     */
    public function getGameContext () :GameContext
    {
        return (_liaison != null) ? _liaison.getGameContext() : null;
    }

    /**
     * Returns the configuration of the (non-world) game we currently occupy if we're in a game.
     * Returns null otherwise.
     */
    public function getGameConfig () :MsoyGameConfig
    {
        if (_liaison != null) {
            return _liaison.gameConfig as MsoyGameConfig;
        }
        return null;
    }

    /**
     * Returns the currently active GameController or null if no game is active.
     */
    public function getGameController () :PlaceController
    {
        var gctx :GameContext = getGameContext();
        return (gctx != null) ? gctx.getLocationDirector().getPlaceController() : null;
    }

    /**
     * Populates the supplied array with data to create the game control bar menu.
     *
     * @return true if the menu was populated, false if we're not in a game.
     */
    public function populateGameMenu (menuData :Array) :Boolean
    {
        if (_liaison == null) {
            return false;
        }

        var config :MsoyGameConfig = getGameConfig();

        menuData.push({label: _liaison.gameName});
        CommandMenu.addSeparator(menuData);
        menuData.push({label: Msgs.GAME.get("b.gameInstructions"), command: viewGameInstructions,
            icon: StyleManager.getStyleDeclaration(".controlBarGameButton").getStyle("image")});
        if (_liaison.gameGroupId != Game.NO_GROUP) {
            menuData.push({label: Msgs.GAME.get("b.gameGroup"),
                           command: MsoyController.VIEW_GROUP, arg: _liaison.gameGroupId });
        }
        menuData.push({label: Msgs.GAME.get("b.gameShop"), command: viewGameShop });
        if (_liaison is LobbyGameLiaison && config != null &&
            config.getGameDefinition().match.getMaximumPlayers() > 1) {
            menuData.push({label: Msgs.GAME.get("b.gameLobby"), command: displayCurrentLobby});
        }
        menuData.push({label: Msgs.GAME.get("b.gameComment"), command: viewGameComments,
            icon: StyleManager.getStyleDeclaration(".controlBarButtonComment").getStyle("image")});
        menuData.push({label: Msgs.GAME.get("b.gameInvite"), command: viewDefaultInvitePage});
        menuData.push({label: Msgs.GAME.get("b.gameTrophies"), command: viewGameTrophies});
        if (Game.isDevelopmentVersion(_liaison.gameId) && !(_liaison is AVRGameLiaison)) {
            menuData.push({label: Msgs.GAME.get("b.gameRemoveTrophies"), command: removeTrophies});
        }
        if (_liaison is AVRGameLiaison) {
            menuData.push({label: Msgs.GAME.get("b.gameExit"), command: leaveAVRGame});
            if (DeploymentConfig.devDeployment) {
                menuData.push({label :"Debug Coordinates", command: showCoordinateDebugPanel});
            }
        }

        return true;
    }

    /**
     * Requests that the lobby for the specified game be joined and displayed.
     */
    public function enterLobby (gameId :int, ghost :String, gport :int) :void
    {
        resolveLobbyLiaison(gameId, ghost, gport);
        LobbyGameLiaison(_liaison).showLobby();
    }

    /**
     * Requests that the lobby for the current game be displayed. Returns true when successful, or
     * false if there is no existing game or existing lobby (eg. the game is an AVRG) or other
     * problems were encountered.
     */
    public function displayCurrentLobby () :Boolean
    {
        if (_liaison is LobbyGameLiaison) {
            LobbyGameLiaison(_liaison).showLobby();
            return true;
        }
        return false;
    }

    /**
     * Displays the instructions page for the currently active game.
     */
    public function viewGameInstructions () :void
    {
        _wctx.getWorldController().displayPage("games", "d_" + getGameId() + "_i");
    }

    /**
     * Displays the comments page for the currently active game.
     */
    public function viewGameComments () :void
    {
        _wctx.getWorldController().displayPage("games", "d_" + getGameId() + "_c");
    }

    /**
     * Displays the trophies page for the currently active game.
     */
    public function viewGameTrophies () :void
    {
        TrophyPanel.show(getGameContext(), getGameId(), getGameName());
    }

    /**
     * Removes the trophies that this player has earned in an in-development game copy.
     */
    public function removeTrophies () :void
    {
        if (!Game.isDevelopmentVersion(_liaison.gameId)) {
            log.warning("Asked to remove copies from a non-development game", "gameId",
                _liaison.gameId);
            return;
        }

        var svc :GameGameService =
            getGameContext().getClient().requireService(GameGameService) as GameGameService;
        svc.removeDevelopmentTrophies(getGameContext().getClient(), _liaison.gameId,
            getGameContext().getMsoyContext().confirmListener(
            "m.trophies_removed", MsoyCodes.GAME_MSGS));
    }

    /**
     * Displays the shop page for the currently active game.
     */
    public function viewGameShop (itemType :int = 0, catalogId :int = 0) :void
    {
        var args :String;
        if (catalogId != 0) {
            args = "l_" + itemType + "_" + catalogId;
        } else {
            args = "g_" + getGameId();
            if (itemType != 0) {
                args += "_" + itemType;
            }
        }
        _wctx.getWorldController().displayPage("shop", args);
    }

    /**
     * Brings up the game invite page in our shell, as requested by the game the user is playing.
     */
    public function viewInvitePage (defmsg :String, token :String = "", roomId :int = 0) :void
    {
        var encodedToken :String = encodeBase64(token);
        log.debug("Viewing invite", "token", token, "encoded", encodedToken);
        // we encode the strings so they are valid as part of the URL and the user cannot trivially
        // see them
        _wctx.getWorldController().displayPage("people",
            Args.join("invites", "game", getGameId(), defmsg, encodedToken,
                _liaison is AVRGameLiaison ? 1 : 0, roomId));
    }

    /**
     * Brings up a default game invite page.
     */
    public function viewDefaultInvitePage () :void
    {
        _wctx.getWorldController().displayPage("people",
            Args.join("invites", "game", getGameId(), "", "", _liaison is AVRGameLiaison ? 1 : 0,
                _wctx.getWorldController().getCurrentSceneId()));
    }

    /**
     * Requests that we immediately start playing the specified game id. If a playerId is
     * specified, we will attempt to join that player's game or lobby table.
     */
    public function playNow (gameId :int, playerId :int, ghost :String = null, gport :int = 0,
        inviteToken :String = null, inviterId :int = 0) :void
    {
        log.debug("Resolving liaison", "host", ghost, "port", gport);
        resolveLobbyLiaison(gameId, ghost, gport);
//         log.debug("Setting invite data", "token", inviteToken, "invId", inviterId);
        _liaison.setInviteData(inviteToken, inviterId);
        log.debug("Playing now", "gameId", gameId, "playerId", playerId);
        LobbyGameLiaison(_liaison).playNow(playerId);
    }

    /** Forwards idleness status updates to any AVRG we may be playing. */
    public function setIdle (nowIdle :Boolean) :void
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            var ctrl :AVRGameController = AVRGameLiaison(_liaison).getAVRGameController();
            if (ctrl != null) {
                ctrl.setIdle(nowIdle);
            }
        }
    }

    /**
     * Retrieve the backend of the AVRG currently in progress, or null.
     */
    public function getAVRGameBackend () :AVRGameBackend
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            return AVRGameLiaison(_liaison).getAVRGameBackend();
        }
        return null;
    }

    /**
     * Called when we first login and then every time we leave a game or a lobby;
     * checks to see if we have a persistent AVRG we should (re-)activate.
     */
    public function checkMemberAVRGame () :void
    {
        // Re-entering an AVRG after leaving a game lobby is a bit strange but was thought to be a
        // cool feature at one point. TODO: get rid of this completely if it isn't needed anymore.
        if (false && _liaison == null) {
            var memberObj :MemberObject = _wctx.getMemberObject();
            // we might not yet be logged onto our world server; freak not out if so
            if (memberObj != null && memberObj.avrGameId != 0) {
                _liaison = new AVRGameLiaison(_wctx, memberObj.avrGameId);
                _liaison.start();
            }
        }
    }

    /**
     * Activates the specified AVR game, connecting to the appropriate game server and clearing any
     * existing game server connection.
     */
    public function activateAVRGame (
        gameId :int, inviteToken :String = "", inviterMemberId :int = 0) :void
    {
        log.debug("Activating AVR game", "token", inviteToken, "inviter", inviterMemberId);

        if (_liaison != null) {
            if (_liaison is LobbyGameLiaison) {
                log.warning("Eek, asked to join an AVRG while in a lobbied game.");
                return;
            }
            if (_liaison.gameId == gameId) {
                if (_liaison.gameConfig == null) {
                    displayFeedback("m.locating_game");
                }
                return;
            }
            // TODO: implement proper leaving, this should only be the fallback
            _liaison.shutdown();
        }

        displayFeedback("m.locating_game");

        _liaison = new AVRGameLiaison(_wctx, gameId);
        _liaison.setInviteData(inviteToken, inviterMemberId);
        _liaison.start();
    }

    public function leaveAVRGame () :void
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            AVRGameLiaison(_liaison).leaveAVRGame();
        }
    }

    /**
     * Requests that we enter the specified in-progress game.
     */
    public function enterGame (gameId :int, gameOid :int) :void
    {
        log.debug("Entering game", "gameId", gameId, "gameOid", gameOid);

        if (_liaison == null) {
            log.warning("Requested to enter game but have no liaison?! [oid=" + gameOid + "].");
            // probably we're following a URL that is super-double-plus out of date; fall back
            // Note we lose the share data here. It might not make sense to send this data
            // back into a new game
            _wctx.getWorldController().handlePlayGame(gameId);

        } else if (!(_liaison is LobbyGameLiaison)) {
            log.warning("Requested to enter game but have AVRG liaison?! [oid=" + gameOid + "].");

        } else {
            LobbyGameLiaison(_liaison).enterGame(gameOid);
        }
    }

    /**
     * Leaves our current parlor game and either returns to the Whirled or displays the lobby.
     */
    public function backToWhirled (showLobby :Boolean) :void
    {
        if (showLobby) {
            LobbyGameLiaison(_liaison).clearGame();
            _wctx.getWorldController().handlePlayGame(getGameId());
        } else {
            // go back to our previous location
            _wctx.getMsoyController().handleMoveBack(true);
        }
    }

    /**
     * Called by the GameLiaison when it has shutdown and gone away.
     */
    public function liaisonCleared (liaison :GameLiaison) : void
    {
        // we could get the "wrong" liaison here, if we were asked to load up a new lobby while
        // another one was active.
        if (_liaison == liaison) {
            _liaison = null;
            // if this was a lobbied game, see about restarting the AVRG
            if (liaison is LobbyGameLiaison) {
                checkMemberAVRGame();
            }
        }
    }

    /**
     * Lets the world controller know that our game is now ready to enter, possibly resulting
     * in a browser url change.
     */
    public function dispatchGameReady (gameId :int, gameOid :int) :void
    {
        _wctx.getWorldController().handleGoGame(gameId, gameOid);
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // if we're actually logging off, rather than just switching servers, then shutdown any
        // active game connection
        if (!event.isSwitchingServers() && _liaison != null) {
            _liaison.shutdown();
        }
    }

    public function getInviteToken () :String
    {
        if (_liaison != null) {
            return _liaison.inviteToken;
        }
        return "";
    }

    public function getInviterMemberId () :int
    {
        if (_liaison != null) {
            return _liaison.inviterMemberId;
        }
    	return 0;
    }

    /**
     * A convenience method to display feedback using the game bundle.
     */
    protected function displayFeedback (msg :String) :void
    {
        _wctx.displayFeedback(MsoyGameCodes.GAME_BUNDLE, msg);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.GAME_GROUP);
    }

    /**
     * Ensures that we have a game liaison for the specified game.
     */
    protected function resolveLobbyLiaison (gameId :int, ghost :String = null, gport :int = 0) :void
    {
        if (_liaison is LobbyGameLiaison && _liaison.gameId == gameId) {
            return;
        }
        if (_liaison != null) {
            _liaison.shutdown();
        }
        _liaison = new LobbyGameLiaison(_wctx, gameId);
        _liaison.start(ghost, gport);
    }

    protected function showCoordinateDebugPanel () :void
    {
        var ctrl :AVRGameController = AVRGameLiaison(_liaison).getAVRGameController();
        ctrl.showCoordinateDebugPanel();
    }

    protected static function encodeBase64 (str :String) :String
    {
        if (str == null || str == "") {
            return "";
        }
        var encoder :Base64Encoder = new Base64Encoder();
        encoder.encodeUTFBytes(str);
        return encoder.toString();
    }

    /** A casted ref to the msoy context. */
    protected var _wctx :WorldContext;

    /** Handles our connection to the game server. */
    protected var _liaison :GameLiaison;
}
}
