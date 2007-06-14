//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.system.Capabilities;

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import mx.events.ResizeEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.client.notifications.NotificationHandler;

import com.threerings.msoy.game.client.FloatingTableDisplay;

import com.threerings.msoy.world.client.RoomView;

public class TopPanel extends Canvas 
    implements LocationObserver
{
    public static const DECORATIVE_MARGIN_HEIGHT :int = 4;

    public static const RIGHT_SIDEBAR_WIDTH :int = 300;

    /**
     * Construct the top panel.
     */
    public function TopPanel (ctx :WorldContext)
    {
        _ctx = ctx;
        _ctx.getLocationDirector().addLocationObserver(this);
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        _headerBar = new HeaderBar(ctx);
        _headerBar.includeInLayout = false;
        _headerBar.setStyle("top", 0);
        _headerBar.setStyle("left", 0);
        _headerBar.setStyle("right", 0);
        addChild(_headerBar);

        _placeBox = new PlaceBox();
        _placeBox.autoLayout = false;
        _placeBox.includeInLayout = false;
        addChild(_placeBox);

        // set up the control bar
        _controlBar = new ControlBar(ctx, this);
        _controlBar.includeInLayout = false;
        _controlBar.setStyle("bottom", 0);
        _controlBar.setStyle("left", 0);
        _controlBar.setStyle("right", 0);
        addChild(_controlBar);

        // show a subtle build-stamp
        var buildStamp :Label = new Label();
        buildStamp.includeInLayout = false;
        buildStamp.mouseEnabled = false;
        buildStamp.mouseChildren = false;
        buildStamp.text = "Build: " + DeploymentConfig.buildTime + "  " + Capabilities.version;
        buildStamp.setStyle("color", "#F7069A");
        buildStamp.setStyle("fontSize", 8);
        buildStamp.setStyle("bottom", ControlBar.HEIGHT);
        buildStamp.setStyle("right", ScrollBar.THICKNESS);
        addChild(buildStamp);

        // clear out the application and install ourselves as the only child
        var app :Application = Application(Application.application);
        app.removeAllChildren();
        app.addChild(this);
        layoutPanels();

        app.stage.addEventListener(Event.RESIZE, stageResized);

        _ctx.getClient().addEventListener(WorldClient.MINI_WILL_CHANGE, miniWillChange);

        _notificationHandler = new NotificationHandler(ctx, this);
    }

    /**
     * Ensures that we are running a sufficiently new version of Flash, returning true if so. If
     * not, it displays a message to the user indicating that they need to upgrade their Flash
     * player and returns false.
     */
    public function verifyFlashVersion () :Boolean
    {
        // the version looks like "LNX 9,0,31,0"
        try {
            var bits :Array = Capabilities.version.split(" ");
            if (bits.length < 2) {
                throw new Error("Failed to split on space");
            }
            bits = (bits[1] as String).split(",");
            if (bits.length < 3) {
                throw new Error("Failed to split on comma");
            }

            // check the major and minor version numbers
            if (int(bits[0]) >= MIN_FLASH_VERSION && int(bits[2]) >= MIN_FLASH_REVISION) {
                return true;
            }

            // display an error and fail
            var panel :DisconnectedPanel = new DisconnectedPanel(_ctx);
            panel.setMessage(MessageBundle.tcompose(
                                 "m.min_flash_version", bits[0], bits[2],
                                 MIN_FLASH_VERSION, MIN_FLASH_REVISION), true);
            setPlaceView(panel);
            return false;

        } catch (error :Error) {
            trace("Choked checking version [version=" + Capabilities.version +
                  ", error=" + error + ".");
            // ah well, whatever, let 'em in and hope for the best
        }
        return true;
    }

    // from LocationObserver
    public function locationMayChange (placeId :int) :Boolean
    {
        // currently there are no side panel types that should survive a place change
        clearLeftPanel(null);
        return true;
    }

    // from LocationObserver
    public function locationDidChange (place :PlaceObject) :void
    {
        // if we just moved to a game lobby make sure the current floating table display is cleared
        if (place is GameObject) {
            clearTableDisplay();
        }
    }

    // from LocationObserver
    public function locationChangeFailed (placeId :int, reason :String) :void
    {
        // NOOP
    }

    /**
     * Get the flex container that is holding the PlaceView. This is useful if you want to overlay
     * things over the placeview or register to receive flex-specific events.
     */
    public function getPlaceContainer () :PlaceBox
    {
        return _placeBox;
    }

    /**
     * Instructs the TopPanel to remove the PlaceView container from its UI hierarchy and return it
     * to the caller so that they can place it into some temporary special location in the UI. If
     * the place container has already been taken, the method returns null.
     */
    public function takePlaceContainer () :PlaceBox
    {
        Log.getLog(this).debug("takePlaceContainer");
        if (_placeBox.parent != this) {
            return null;
        }
        removeChild(_headerBar);
        removeChild(_placeBox);
        layoutPanels();
        updatePlaceViewChatOverlay();
        return _placeBox;
    }

    /**
     * Informs the TopPanel that the place container can be readded to its UI hierarchy as the
     * caller has finished with it. The caller must have removed the PlaceBox from their UI
     * hierarchy before calling this method.
     */
    public function restorePlaceContainer () :void
    {
        Log.getLog(this).debug("restorePlaceContainer");
        if (_placeBox.parent != null) {
            Log.getLog(this).warning("Requested to restore PlaceBox but it's still added.");
            _placeBox.parent.removeChild(_placeBox);
        }
        addChild(_headerBar);
        addChildAt(_placeBox, 0);
        updatePlaceViewChatOverlay();
        layoutPanels();
    }

    /**
     * Returns the currently configured place view.
     */
    public function getPlaceView () :PlaceView
    {
        return _placeBox.getPlaceView();
    }

    /**
     * Sets the specified view as the current place view.
     */
    public function setPlaceView (view :PlaceView) :void
    {
        _placeBox.setPlaceView(view);
        updatePlaceViewChatOverlay();
        updatePlaceViewSize();
    }

    /**
     * Clear the specified place view, or null to clear any.
     */
    public function clearPlaceView (view :PlaceView) :void
    {
        _placeBox.clearPlaceView(view);
    }

    /**
     * Returns a reference to our ControlBar component.
     */
    public function getControlBar () :ControlBar
    {
        return _controlBar;
    }

    /**
     * Returns a reference to our HeaderBar component
     */
    public function getHeaderBar () :HeaderBar
    {
        return _headerBar;
    }

    /**
     * Configures our left side panel. Any previous left side panel will be cleared.
     */
    public function setLeftPanel (side :UIComponent) :void
    {
        clearLeftPanel(null);
        _leftPanel = side;
        _leftPanel.includeInLayout = false;
        _leftPanel.width = side.width;

        if (_tableDisp != null) {
            _tableDisp.x += _leftPanel.width;
        }

        _ctx.getWorldClient().setSeparator(side.width - 1);
        addChild(_leftPanel); // add to end
        layoutPanels();

        (_ctx.getChatDirector() as MsoyChatDirector).sendRoomToTab();
    }

    /**
     * Clear the specified left side panel, or null to clear any.
     */
    public function clearLeftPanel (side :UIComponent) :void
    {
        if ((_leftPanel != null) && (side == null || side == _leftPanel)) {
            if (_tableDisp != null) {
                _tableDisp.x -= _leftPanel.width;
                if (_tableDisp.x < 0) {
                    _tableDisp.x = 0;
                }
            }

            _ctx.getWorldClient().clearSeparator();
            removeChild(_leftPanel);
            _leftPanel = null;

            layoutPanels();

            (_ctx.getChatDirector() as MsoyChatDirector).removeRoomTab();
        }
    }

    /**
     * Configures our right side panel. Any previous right side panel will be cleared.
     */
    public function setRightPanel (side :UIComponent) :void
    {
        Log.getLog(this).debug("setRightPanel");
        clearRightPanel(null);
        _rightPanel = side;
        _rightPanel.includeInLayout = false;
        _rightPanel.width = side.width;
        addChild(_rightPanel);
        layoutPanels();
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearRightPanel (side :UIComponent) :void
    {
        if ((_rightPanel != null) && (side == null || side == _rightPanel)) {
            removeChild(_rightPanel);
            _rightPanel = null;
            layoutPanels();
        }
    }

    /**
     * Sets the current table display
     */
    public function setTableDisplay (tableDisp :FloatingTableDisplay) :void
    {
        if (tableDisp != _tableDisp) {
            clearTableDisplay();
            _tableDisp = tableDisp;
            _tableDisp.x = 0;
            _tableDisp.y = DECORATIVE_MARGIN_HEIGHT + HeaderBar.HEIGHT;
        }
    }

    /**
     * Gets the current table display
     */
    public function getTableDisplay () :FloatingTableDisplay
    {
        return _tableDisp;
    }

    /**
     * Clears the current table display - should only be used if this table display should be
     * destroyed (i.e. the game started, or the another table was joined)
     */
    public function clearTableDisplay () :void
    {
        if (_tableDisp != null) {
            _tableDisp.shutdown();
            _tableDisp = null;
        }
    }

    /**
     * Set the panel that should be shown along the bottom. The panel should have an explicit
     * height. If the height is 100 pixels or larger, a chat box will be placed to the left of it
     * and removed from the room overlay.
     */
    public function setBottomPanel (bottom :UIComponent) :void
    {
        clearBottomPanel(null);

        _bottomPanel = new HBox();
        _bottomPanel.setStyle("horizontalGap", 0);
        _bottomPanel.setStyle("bottom", ControlBar.HEIGHT);
        _bottomPanel.setStyle("left", 0);
        _bottomPanel.setStyle("right", 0);

        _bottomComp = bottom;
        _bottomComp.percentWidth = 100;
        _bottomPanel.addChild(bottom);
        _bottomPanel.includeInLayout = false;
        _bottomPanel.height = bottom.height; // eek?

        addChild(_bottomPanel); // add to end
        layoutPanels();
    }

    public function clearBottomPanel (bottom :UIComponent) :void
    {
        if ((_bottomComp != null) && (bottom == null || bottom == _bottomComp)) {
            removeChild(_bottomPanel);
            _bottomPanel = null;
            _bottomComp = null;
            layoutPanels();
        }
    }

    public function showFriends (show :Boolean) :void
    {
        if (show) {
            // lazy-init the friendslist
            if (_friendsList == null) {
                _friendsList = new FriendsList(_ctx);
            }
            // put the pals list atop everything else
            addChild(_friendsList);

        } else {
            if (_friendsList != null) {
                removeChild(_friendsList);
            }
        }
    }

    protected function stageResized (event :Event) :void
    {
        layoutPanels();
    }

    protected function miniWillChange (event :ValueEvent) :void
    {
        // clear out our left panel if we are about to be minimized
        if (event.value as Boolean) {
            clearLeftPanel(null);
            (_ctx.getChatDirector() as MsoyChatDirector).sendRoomToTab();
        } else if (_leftPanel == null) {
            // if we are not minimized, and we don't have a left panel, restore the room to its
            // previous glory
            (_ctx.getChatDirector() as MsoyChatDirector).removeRoomTab();
        }
    }

    protected function layoutPanels () :void
    {
        if (_leftPanel != null) {
            _leftPanel.setStyle("top", 0);
            _leftPanel.setStyle("bottom", getBottomPanelHeight());
            _leftPanel.setStyle("left", 0);
            _controlBar.setStyle("left", _leftPanel.width);
            _headerBar.setStyle("left", _leftPanel.width);
        } else {
            _controlBar.setStyle("left", 0);
            _headerBar.setStyle("left", 0);
        }

        if (_rightPanel != null) {
            _rightPanel.setStyle("top", 0);
            _rightPanel.setStyle("right", 0);
            _rightPanel.setStyle(
                "bottom", getBottomPanelHeight() + ControlBar.HEIGHT + DECORATIVE_MARGIN_HEIGHT);
            _headerBar.setStyle("right", _rightPanel.width);

            // if we have no place view currently and we have no left panel, stretch it all the 
            // way to the left.  Otherwise, let it be as wide as it wants to be.
            if (_placeBox.parent == this || _leftPanel != null) {
                _rightPanel.clearStyle("left");
            } else {
                _rightPanel.setStyle("left", 0);
            }

        } else {
            _headerBar.setStyle("right", 0);
        }

        updatePlaceViewSize();
    }

    /**
     * Check to see if the placeview should be using its own chat overlay.
     */
    protected function updatePlaceViewChatOverlay () :void
    {
        var pv :PlaceView = getPlaceView();
        if (pv is RoomView) {
            (pv as RoomView).setUseChatOverlay(_placeBox.parent == this);
        }
    }

    protected function updatePlaceViewSize () :void
    {
        if (_placeBox.parent != this) {
            return; // nothing doing if we're not in control
        }

        var botHeight :int = getBottomPanelHeight();
        var w :int = stage.stageWidth - getLeftPanelWidth() - getRightPanelWidth();
        var h :int = stage.stageHeight - ControlBar.HEIGHT - botHeight - HeaderBar.HEIGHT;
        var top :int = HeaderBar.HEIGHT;
        var bottom :int = botHeight + ControlBar.HEIGHT;

        // actually, for place views, we want to insert decorative margins above and below the
        // view, so let's tweak the sizes
        if (_placeBox.getPlaceView() is MsoyPlaceView) {
            top += DECORATIVE_MARGIN_HEIGHT;
            bottom += DECORATIVE_MARGIN_HEIGHT;
            h -= DECORATIVE_MARGIN_HEIGHT * 2;
        }

        _placeBox.setStyle("top", top);
        _placeBox.setStyle("bottom", bottom);
        _placeBox.setStyle("left", getLeftPanelWidth());
        _placeBox.setStyle("right", getRightPanelWidth());
        _placeBox.wasResized(w, h);
    }

    protected function getLeftPanelWidth () :int
    {
        return (_leftPanel == null ? 0 : _leftPanel.width);
    }

    protected function getRightPanelWidth () :int
    {
        return (_rightPanel == null ? 0 : _rightPanel.width);
    }

    protected function getBottomPanelHeight () :int
    {
        return (_bottomPanel == null ? 0 : _bottomPanel.height);
    }

    /** The giver of life. */
    protected var _ctx :WorldContext;

    /** The box that will hold the placeview. */
    protected var _placeBox :PlaceBox;

    /** The current left panel component. */
    protected var _leftPanel :UIComponent;

    /** The current right panel component. */
    protected var _rightPanel :UIComponent;

    /** The thing what holds the bottom panel. */
    protected var _bottomPanel :HBox;

    /** The current bottom panel component. */
    protected var _bottomComp :UIComponent;

    /** Header bar at the top of the window. */
    protected var _headerBar :HeaderBar;

    /** Control bar at the bottom of the window. */
    protected var _controlBar :ControlBar;

    /** Storage for a GUI element corresponding to decorative lines. */
    protected var _decorativeBar :Canvas;

    /** The list of our friends. */
    protected var _friendsList :FriendsList;

    /** the currently active table display */
    protected var _tableDisp :FloatingTableDisplay;

    /** Dispatch that displays server notifications. */
    protected var _notificationHandler :NotificationHandler;
    
    protected static const MIN_FLASH_VERSION :int = 9;
    protected static const MIN_FLASH_REVISION :int = 28;
}
}
