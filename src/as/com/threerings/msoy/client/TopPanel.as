//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.geom.Rectangle;

import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;

import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import com.threerings.crowd.client.PlaceView;

import com.threerings.msoy.chat.client.ChatTabBar;
import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.ComicOverlay;
import com.threerings.msoy.chat.client.GameChatContainer;

public class TopPanel extends Canvas
{
    public static const DECORATIVE_MARGIN_HEIGHT :int = 0;

    public static const RIGHT_SIDEBAR_WIDTH :int = 300;

    /**
     * Construct the top panel.
     */
    public function TopPanel (ctx :MsoyContext, controlBar :ControlBar)
    {
        _ctx = ctx;
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        styleName = "topPanel";

        var chatTabs :ChatTabBar = new ChatTabBar(_ctx);
        _ctx.getMsoyChatDirector().setChatTabs(chatTabs);

        if (UberClient.isRegularClient()) {
            _headerBar = new HeaderBar(_ctx, chatTabs);
            _headerBar.includeInLayout = false;
            _headerBar.setStyle("top", 0);
            _headerBar.setStyle("left", 0);
            _headerBar.setStyle("right", 0);
            addChild(_headerBar);
        }

        _placeBox = new PlaceBox();
        _placeBox.autoLayout = false;
        _placeBox.includeInLayout = false;
        addChild(_placeBox);

        // save the control bar, even if we don't add it (due to being a featured place)
        _controlBar = controlBar;
        _controlBar.init(this);
        if (!UberClient.isFeaturedPlaceView()) {
            // only create and display an overlay for real clients
            if (UberClient.isRegularClient()) {
                _comicOverlay = new ComicOverlay(_ctx, _placeBox);
                _ctx.getMsoyChatDirector().addChatDisplay(_comicOverlay);
            }

            // set up the control bar
            _controlBar.includeInLayout = false;
            _controlBar.setStyle("bottom", 0);
            _controlBar.setStyle("left", 0);
            _controlBar.setStyle("right", 0);
            addChild(_controlBar);

            // show a subtle build-stamp on dev builds
            if (DeploymentConfig.devDeployment) {
                var buildStamp :Label = new Label();
                buildStamp.includeInLayout = false;
                buildStamp.mouseEnabled = false;
                buildStamp.mouseChildren = false;
                buildStamp.text = "Build: " + DeploymentConfig.buildTime;
                buildStamp.setStyle("color", "#F7069A");
                buildStamp.setStyle("fontSize", 8);
                buildStamp.setStyle("bottom", ControlBar.HEIGHT);
                // The scrollbar isn't really this thick, but it's pretty close.
                buildStamp.setStyle("right", ScrollBar.THICKNESS);
                addChild(buildStamp);
            }
        }

        // clear out the application and install ourselves as the only child
        var app :Application = UberClient.getApplication();
        app.removeAllChildren();
        app.addChild(this);
        app.stage.addEventListener(Event.RESIZE, stageResized);

        // display something until someone comes along and sets a real view on us
        setPlaceView(new BlankPlaceView(_ctx));

        _ctx.getUIState().addEventListener(UIStateChangeEvent.STATE_CHANGE, handleUIStateChange);
        handleUIStateChange(null);
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
     * Returns the currently configured place view.
     */
    public function getPlaceView () :PlaceView
    {
        return _placeBox.getPlaceView();
    }

    /**
     * Returns the chat overlay that is in use, or null if there is none.
     */
    public function getChatOverlay () :ChatOverlay
    {
        if (_leftPanel is GameChatContainer) {
            return (_leftPanel as GameChatContainer).getChatOverlay();
        } else {
            return _comicOverlay;
        }
    }

    /**
     * Returns the comic overlay that is used for all place view chat
     */
    public function getPlaceChatOverlay () :ComicOverlay
    {
        return _comicOverlay;
    }

    /**
     * Sets the specified view as the current place view.
     */
    public function setPlaceView (view :PlaceView) :void
    {
        _placeBox.setPlaceView(view);
        layoutPanels();

        const mView :MsoyPlaceView = view as MsoyPlaceView;
        if (_comicOverlay != null) {
            _comicOverlay.displayChat((mView != null) && mView.shouldUseChatOverlay());
        }

        if (_topPanel is EmbedHeader) {
            const embedHeader :EmbedHeader = _topPanel as EmbedHeader;
            if (mView != null) {
                embedHeader.setPlaceName(mView.getPlaceName(), mView.getPlaceLogo());
            } else {
                embedHeader.setPlaceName(null);
            }
        }
    }

    /**
     * Clear the specified place view, or null to clear any.
     */
    public function clearPlaceView (view :PlaceView) :void
    {
        if (_placeBox.clearPlaceView(view)) {
            setPlaceView(new BlankPlaceView(_ctx));
        }
    }

    /**
     * Returns the location and dimensions of the place view in relation to the entire stage.
     */
    public function getPlaceViewBounds () :Rectangle
    {
        var left :Number = _placeBox.getStyle("left");
        var top :Number = _placeBox.getStyle("top");
        var width :Number = _ctx.getWidth() - _placeBox.getStyle("right") - left;
        var height :Number = _ctx.getHeight() - _placeBox.getStyle("bottom") - top;
        return new Rectangle(left, top, width, height);
    }

    /**
     * Returns a rectangle in stage coordinates that specifies the main game area.  This is
     * basically just the bounds on the client, minus the any margins from control/header bars, etc.
     */
    public function getMainAreaBounds () :Rectangle
    {
        var height: Number = _ctx.getHeight() - _placeBox.getStyle("bottom");
        return new Rectangle(0, _placeBox.getStyle("top"), _ctx.getWidth(), height);
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
     * Set the panel at the top of the display.
     *
     * This is an unfortunate name collision.  However, it is the name that makes sense...
     */
    public function setTopPanel (top :UIComponent) :void
    {
        clearTopPanel();
        _topPanel = top;
        _topPanel.includeInLayout = false;
        addChild(_topPanel);
        layoutPanels();
        handleUIStateChange(null);
    }

    public function clearTopPanel (top :UIComponent = null) :void
    {
        if ((_topPanel != null) && (top == null || top == _topPanel)) {
            if (_topPanel.parent == this) {
                removeChild(_topPanel);
            }
            _topPanel = null;
            layoutPanels();
        }
    }

    /**
     * Configures our left side panel. Any previous right side panel will be cleared.
     */
    public function setLeftPanel (side :UIComponent) :void
    {
        clearLeftPanel(null);
        _leftPanel = side;
        _leftPanel.includeInLayout = false;
        _leftPanel.width = side.width;
        addChild(_leftPanel);
        layoutPanels();
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearLeftPanel (side :UIComponent = null) :void
    {
        if ((_leftPanel != null) && (side == null || side == _leftPanel)) {
            if (_leftPanel.parent == this) {
                removeChild(_leftPanel);
            }
            _leftPanel = null;
            layoutPanels();
        }
    }

    public function getLeftPanel () :UIComponent
    {
        return _leftPanel;
    }

    public function getLeftPanelWidth () :int
    {
        return (_leftPanel == null ? 0 : _leftPanel.width);
    }

    protected function stageResized (event :Event) :void
    {
        layoutPanels();
    }

    protected function handleUIStateChange (event :UIStateChangeEvent) :void
    {
        var state :UIState = _ctx.getUIState();

        if (_topPanel != null) {
            _topPanel.visible = state.embedded && state.inRoom;
        }
    }

    protected function getTopPanelHeight () :int
    {
        return _topPanel != null ? Math.round(_topPanel.height) : 0;
    }

    protected function getHeaderBarHeight () :int
    {
        return _headerBar != null ? HeaderBar.HEIGHT : 0;
    }

    protected function layoutPanels () :void
    {
        // Pin the app to the stage.
        // This became necessary for "stubs" after we upgraded to flex 3.2.
        var app :Application = UberClient.getApplication();
        app.width = _ctx.getWidth();
        app.height = _ctx.getHeight();

        if (UberClient.isFeaturedPlaceView()) {
            // in this case, we only have one panel...
            updatePlaceViewSize();
            return;
        }

        _controlBar.setStyle("left", 0);
        if (_headerBar != null) {
            _headerBar.setStyle("left", 0);
            _headerBar.setStyle("top", getTopPanelHeight());
        }

        if (_topPanel != null) {
            _topPanel.setStyle("top", 0);
            _topPanel.setStyle("right", 0);
            _topPanel.setStyle("left", 0);
        }

        if (_leftPanel != null) {
            _leftPanel.setStyle("top", getTopPanelHeight() + getHeaderBarHeight());
            _leftPanel.setStyle("left", 0);
            _leftPanel.setStyle("bottom", ControlBar.HEIGHT);

            // if we have no place view currently, stretch it all the way to the left; otherwise
            // let it be as wide as it wants to be
            if (_placeBox.parent == this) {
                _leftPanel.clearStyle("right");
            } else {
                _leftPanel.setStyle("right", 0);
            }
        }

        updatePlaceViewSize();
    }

    protected function updatePlaceViewSize () :void
    {
        if (_placeBox.parent != this) {
            return; // nothing doing if we're not in control
        }

        if (UberClient.isFeaturedPlaceView()) {
            _placeBox.clearStyle("top");
            _placeBox.clearStyle("bottom");
            _placeBox.clearStyle("left");
            _placeBox.clearStyle("right");
            _placeBox.setActualSize(_ctx.getWidth(), _ctx.getHeight());
            return;
        }

        var top :int = getHeaderBarHeight() + getTopPanelHeight();
        var left :int = 0;
        var right :int = 0;
        var bottom :int = 0;
        var w :int = _ctx.getWidth() - getLeftPanelWidth();
        var h :int = _ctx.getHeight() - top;
        if (padVertical(_placeBox.getPlaceView())) {
            top += DECORATIVE_MARGIN_HEIGHT;
            h -= DECORATIVE_MARGIN_HEIGHT;
        }

        // for place views, we want to insert decorative margins above and below the view
        if (padVertical(_placeBox.getPlaceView())) {
            bottom += DECORATIVE_MARGIN_HEIGHT;
            h -= DECORATIVE_MARGIN_HEIGHT;
        }
        bottom += ControlBar.HEIGHT;
        h -= ControlBar.HEIGHT;

        if (_comicOverlay != null) {
            _comicOverlay.setTargetBounds(new Rectangle(0, 0, ChatOverlay.DEFAULT_WIDTH, h));
        }

        //w -= ScrollBar.THICKNESS;
        _placeBox.setStyle("top", top);
        _placeBox.setStyle("bottom", bottom);
        _placeBox.setStyle("right", right);
        _placeBox.setStyle("left", left + getLeftPanelWidth()); // + ScrollBar.THICKNESS);
        _placeBox.setActualSize(w, h);
    }

    protected function padVertical (view :PlaceView) :Boolean
    {
        return (view is MsoyPlaceView) && (view as MsoyPlaceView).padVertical();
    }

    protected static const CHAT_PADDING :int = 15;

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    /** The box that will hold the placeview. */
    protected var _placeBox :PlaceBox;

    /** The current top panel component. */
    protected var _topPanel :UIComponent;

    /** The current right panel component. */
    protected var _leftPanel :UIComponent;

    /** Header bar at the top of the window. */
    protected var _headerBar :HeaderBar;

    /** Control bar at the bottom of the window. */
    protected var _controlBar :ControlBar;

    protected var _comicOverlay :ComicOverlay;
}
}
