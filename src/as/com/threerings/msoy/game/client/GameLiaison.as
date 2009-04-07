//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.net.URLLoader;
import flash.net.URLRequest;

import flash.system.Security;
import flash.text.TextField;
import flash.utils.Timer;

import caurina.transitions.Tweener;

import com.threerings.msoy.utils.Base64Decoder;

import com.threerings.io.TypedArray;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;

import com.threerings.crowd.client.PlaceController;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.util.Log;
import com.threerings.util.MultiLoader;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.GuestSessionCapture;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.all.Trophy;

/**
 * Handles all the fiddly bits relating to connecting to a separate server to match-make and
 * play a game. This class is subclassed by LobbyGameLiaison, which handles lobbied games
 * that take over the view, and AVRGameLiaison, which handles in-world games.
 */
public class GameLiaison
    implements WorldGameService_LocationListener, ClientObserver, MessageListener
{
    public static const log :Log = Log.getLog(GameLiaison);

    public function GameLiaison (wctx :WorldContext, gameId :int)
    {
        _wctx = wctx;
        _gameId = gameId;

        // create our custom context which we'll use to connect to lobby/game servers
        _gctx = new LiaisonGameContext(wctx);
        _gctx.getClient().addClientObserver(this);

        // make sure we're not touring
        _wctx.getTourDirector().endTour();
    }

    public function getGameContext () :GameContext
    {
        return _gctx;
    }

    /**
     * Returns the config of our active game if we're in an active game.
     */
    public function get gameConfig () :PlaceConfig
    {
        var ctrl :PlaceController = _gctx.getLocationDirector().getPlaceController();
        return (ctrl == null) ? null : ctrl.getPlaceConfig();
    }

    /**
     * Returns the ID of the game we're match-making for.
     */
    public function get gameId () :int
    {
        return _gameId;
    }

    /**
     * Returns the name of the game we are playing or lobbying for.
     */
    public function get gameName () :String
    {
        // Subclasses have to do this because AVRGameConfig does not share with MsoyGameConfig
        return null;
    }

    /**
     * Returns the ID of this game's group.
     */
    public function get gameGroupId () :int
    {
        return 0;
    }

    public function get inviteToken () :String
    {
        return _inviteToken;
    }

    public function set inviteToken (value :String) :void
    {
        _inviteToken = value;
    }

    public function get inviterMemberId () :int
    {
        return _inviterMemberId;
    }

    public function set inviterMemberId (value :int) :void
    {
        _inviterMemberId = value;
    }

    /**
     * Configures the invite data received from GWT.
     *
     * @param token the base64 encoded invite token.
     * @param inviterId the member id of the sender of the invitation.
     */
    public function setInviteData (token :String, inviterId :int) :void
    {
        try {
            if (token != null) {
                var decoder :Base64Decoder = new Base64Decoder();
                decoder.decode(token);
                _inviteToken = decoder.toByteArray().toString();
            }
            _inviterMemberId = inviterId;
        } catch (err :Error) {
            log.warning("Bogus invite data", "token", token, "iid", inviterId, "error", err);
        }
    }

    /**
     * Starts this game liaison. If the game host and port are supplied, they will be used
     * immediately, otherwise the liaison will first ask its world server to locate the game in
     * question.
     */
    public function start (ghost :String = null, gport :int = 0) :void
    {
        if (ghost != null && gport != 0) {
            gameLocated(ghost, gport);

        } else if (_wctx.getClient().isLoggedOn()) {
            log.info("Resolving location of game [id=" + _gameId + "].");
            var mgsvc :WorldGameService =
                (_wctx.getClient().requireService(WorldGameService) as WorldGameService);
            mgsvc.locateGame(_wctx.getClient(), gameId, this);

        } else {
            log.info("Resolving location of game via HTTP [id=" + _gameId + "].");
            var loader :URLLoader = new URLLoader();
            loader.addEventListener(Event.COMPLETE, function () :void {
                loader.removeEventListener(Event.COMPLETE, arguments.callee);
                var bits :Array = (loader.data as String).split(":");

                // now configure the world client to connect to the server that hosts our group
                // room so that when we get into the lobby, we'll be ready to roll
                _wctx.getClient().setServer(bits[2], [ int(bits[3]) ]);

                gameLocated(bits[0], int(bits[1]));
            });
            // TODO: add listeners for failure events? give feedback on failure?
            loader.load(new URLRequest(DeploymentConfig.serverURL + "embed/g" + _gameId));
        }
    }

    /**
     * Shuts down this liaison, disconnecting from the game server if we have not already.
     */
    public function shutdown () :void
    {
        _gctx.getClient().logoff(false);
    }

    // from interface WorldGameService_LocationListener
    public function gameLocated (hostname :String, port :int) :void
    {
        log.info("Game located - logging in", "gameId", _gameId, "host", hostname, "port", port);

        // grab & stash the session details (mainly only important if this is a permaguest login
        // but benign otherwise); the callback will be called after game server logon succeeds
        var gclient :Client = _gctx.getClient();
        GuestSessionCapture.capture(gclient, function () :void {
            // save the session token for future use (this copies our session token into the world
            // client's credentials)
            _wctx.saveSessionToken(gclient.getAuthResponseData());

            // now log onto the world server with those credentials (if we're not already)
            var wclient :Client = _wctx.getClient();
            if (!wclient.isLoggedOn()) {
                wclient.logon();
            }
        });

        gclient.setServer(hostname, [ port ]);
        gclient.setVersion(DeploymentConfig.version);
        gclient.logon();
    }

    // from interface WorldGameService_LocationListener
    public function requestFailed (cause :String) :void
    {
        _wctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
    }

    // from interface ClientObserver
    public function clientWillLogon (event :ClientEvent) :void
    {
        var url :String = "xmlsocket://" + _gctx.getClient().getHostname() + ":" +
            DeploymentConfig.socketPolicyPort;
        log.info("Loading security policy: " + url);
        Security.loadPolicyFile(url);
    }

    // from interface ClientObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
        // listen for message events on our player object
        _gctx.getPlayerObject().addListener(this);

        // register some code to run when the users clicks the close button
        _wctx.getWorldController().addPlaceExitHandler(onPlaceExit);
    }

    // from interface ClientObserver
    public function clientFailedToLogon (event :ClientEvent) :void
    {
        // TODO: something fancier?
        _wctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.internal_error");
        clientDidClear(null); // abandon ship
    }

    // from interface ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from interface ClientObserver
    public function clientConnectionFailed (event :ClientEvent) :void
    {
        // TODO: report an error message to the user?
        log.info("Lost connection to game server [cause=" + event.getCause() + "].");
        // we'll get a didLogoff in a second where the GameDirector will shut us down
    }

    // from interface ClientObserver
    public function clientWillLogoff (event :ClientEvent) :void
    {
        // nada
    }

    // from interface ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        log.info("Logged off of game server [id=" + _gameId + "].");
        var player :PlayerObject = _gctx.getPlayerObject();
        if (player) {
            player.removeListener(this);
        }

        // unregister our place exit code
        _wctx.getWorldController().removePlaceExitHandler(onPlaceExit);
    }

    // from interface ClientObserver
    public function clientDidClear (event :ClientEvent) :void
    {
        // tell the game director that we're audi
        _wctx.getGameDirector().liaisonCleared(this);
    }

    // from interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        const name :String = event.getName();
        const args :Array = event.getArgs();
        if (name == MsoyGameCodes.TROPHY_AWARDED || name == MsoyGameCodes.PRIZE_AWARDED) {
            // display the award via the notification director
            _wctx.getNotificationDirector().displayAward(args[0]);

            // if this was a trophy award, also pass that info on to GWT
            if (name == MsoyGameCodes.TROPHY_AWARDED) {
                var trophy :Trophy = (args[0] as Trophy);
                // store in our list of trophies so we can offer to post a facebook news story later
                _trophies.push(trophy);
            }

        } else if (name == WhirledGameObject.COINS_AWARDED_MESSAGE) {
            const coins :int = int(args[0]);
            const forReal :Boolean = Boolean(args[2]);
            const hasCookie :Boolean = Boolean(args[3]);
            const gamePanel :MsoyGamePanel = getMsoyGamePanel();
// TODO: reenable with an A/B test
//             if (forReal && _gctx.getPlayerObject().isPermaguest() &&
//                 (!MsoyGamePanel.USE_GAMEOVER_POPUP || gamePanel.getController().isParty())) {
//                 // if a guest earns flow, we want to show them the "please register" dialog
//                 displayGuestFlowEarnage(coins, hasCookie);
//             }
            // display a notification to permaguests about signing up
            if (forReal && _gctx.getPlayerObject().isPermaguest()) {
                _wctx.getNotificationDirector().displayPayoutUpsell(coins, hasCookie);
            }
            // and always pass the buck to the panel, even if it doesn't care..
            gamePanel.displayGameOverCoinAward(forReal, coins, hasCookie);
        }
    }

    protected function getMsoyGamePanel () :MsoyGamePanel
    {
        return (_wctx.getPlaceView() as MsoyGamePanel);
    }

    /**
     * Display a slide-down panel to guests in situations in which they otherwise
     * wouldn't see the standard GameOverPanel.
     */
    protected function displayGuestFlowEarnage (amount :int, hasCookie :Boolean) :void
    {
        if (_guestFlowPanel == null) {
            _guestFlowPanel = LOADING;
            _flowPanelAutoDismiss = new Timer(15000, 1);
            _flowPanelAutoDismiss.addEventListener(TimerEvent.TIMER, clearGuestFlow);
            MultiLoader.getContents(
                GUEST_FLOW_PANEL, function (result :DisplayObjectContainer) :void {
                _guestFlowPanel = result;
                displayGuestFlowEarnage(amount, hasCookie);
            });

        } else if (_guestFlowPanel == LOADING || _guestFlowPanel.stage != null) {
            return; // we're loading it or already showing it

        } else {
            var displayDefaultNote :Function = function () :void {
                populateGuestFlowEarnage(amount,
                    hasCookie ? "l.guest_flowprog_note" : "l.guest_flow_note");
            };

            // perform an A/B(/C) test on game over upsell text for guests
            _wctx.getMsoyClient().getABTestGroup(
                "2008 12 game over upsell 2", true, new ResultAdapter(
                    function (group :int) :void {
                        if (group == 2) {
                            // group 2 sees a general whirled note
                            populateGuestFlowEarnage(amount, "l.guest_flow_whirled_note");
                        } else if (group == 3) {
                            // group 3 sees one of three notes rotating in order (#0, 1 or 2)
                            populateGuestFlowEarnage(
                                amount, "l.guest_flow_rotate_note_" + _flowPanelNoteNext);
                            _flowPanelNoteNext = (_flowPanelNoteNext + 1) % 3;
                        } else {
                            // group 1 and no_group see keep your coins &/or progress note
                            displayDefaultNote();
                        }
                    },
                    function (cause :String) :void {
                        populateGuestFlowEarnage(amount, "l.guest_flow_note");
                        // if something goes wrong display default note
                        displayDefaultNote();
                    }));
        }
    }

    /**
     * Set the contents of the _guestFlowPanel.
     */
    protected function populateGuestFlowEarnage (amount :int, ifyousignMsg :String) :void
    {
        var field :TextField = (_guestFlowPanel.getChildByName("youearned") as TextField);
        field.text = Msgs.GAME.get("l.guest_flow_title", ""+amount);

        field = (_guestFlowPanel.getChildByName("ifyousign") as TextField);
        field.text = Msgs.GAME.get(ifyousignMsg);

        var later :SimpleButton = (_guestFlowPanel.getChildByName("Later") as SimpleButton);
        later.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
            clearGuestFlow();
        });

        var signUp :SimpleButton = (_guestFlowPanel.getChildByName("SignUp") as SimpleButton);
        signUp.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
            _wctx.getWorldController().handleShowSignUp();
            clearGuestFlow();
        });

        // slide the panel onto the screen, and wait for a click
        _wctx.getTopPanel().getPlaceContainer().addOverlay(
            _guestFlowPanel, PlaceBox.LAYER_TRANSIENT);
        _guestFlowPanel.x = 150;
        _guestFlowPanel.y = -_guestFlowPanel.height;
        Tweener.addTween(_guestFlowPanel, { y: 0, time: 0.75, transition: EASING_OUT });
        _flowPanelAutoDismiss.start();
    }

    protected function clearGuestFlow (... ignored) :void
    {
        _flowPanelAutoDismiss.reset();
        Tweener.addTween(_guestFlowPanel,
            {y: -_guestFlowPanel.height, time: 0.75, transition: EASING_IN,
                onComplete: function () :void {
                    _wctx.getTopPanel().getPlaceContainer().removeOverlay(_guestFlowPanel);
                }
            });
    }

    /**
     * Does our place exiting bit. Prevents exit from proceeding if we've got some trophies to
     * show.
     */
    protected function onPlaceExit () :Boolean
    {
        // show the trophy feeder; have it close the place view when it closes
        return maybeShowFeedPanel(_wctx.getWorldController().handleClosePlaceView);
    }

    /**
     * Removes all exit handlers and maybe shows the feed panel. If the feed panel is shown, the
     * given function is called when it is closed and false is returned. Otherwise, true is
     * returned.
     */
    protected function maybeShowFeedPanel (onClose :Function) :Boolean
    {
        // remove the handler, we don't want to show this twice
        _wctx.getWorldController().removePlaceExitHandler(onPlaceExit);

        // if this is a development version of the game, no trophy popup
        if (Game.isDevelopmentVersion(_gameId)) {
            return true;
        }

        return !TrophyFeederPanel.showNew(_gctx, _gameId, gameName, _trophies, onClose);
    }

    /** Provides access to main client services. */
    protected var _wctx :WorldContext;

    /** A separate context that connects to the game server. */
    protected var _gctx :GameContext;

    /** The id of the game with which we're dealing. */
    protected var _gameId :int;

    protected var _inviteToken :String;

    protected var _inviterMemberId :int;

    /** The "guest earned flow" popup. */
    protected var _guestFlowPanel :DisplayObjectContainer;

    /** Automatically dismisses the flow panel. */
    protected var _flowPanelAutoDismiss :Timer;

    /** Which one of the rotating flow panel messages will be seen next? */
    protected var _flowPanelNoteNext :int = 0;

    /** Trophies we've earned in this session. */
    protected var _trophies :TypedArray = TypedArray.create(Trophy);

    /** Used to note that we're loading an embedded SWF. */
    protected static const LOADING :Sprite = new Sprite();

    /** The Tweener easing functions used for our award and guest coin displays */
    protected static const EASING_OUT :String = "easeoutbounce";
    protected static const EASING_IN :String = "easeoutcubic";

    [Embed(source="../../../../../../../rsrc/media/award_panel.swf",
           mimeType="application/octet-stream")]
    protected static const AWARD_PANEL :Class;

    [Embed(source="../../../../../../../rsrc/media/guest_flow_panel.swf",
           mimeType="application/octet-stream")]
    protected static const GUEST_FLOW_PANEL :Class;
}
}
