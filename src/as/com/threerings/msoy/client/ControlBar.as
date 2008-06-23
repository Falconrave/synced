//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Spacer;
import mx.core.UIComponent;

import mx.events.FlexEvent;

import com.threerings.flex.ChatControl;
import com.threerings.flex.CommandButton;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.SkinnableImage;

[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * Dispatched when the client is unminimized, and this component's display list has been validated.
 *
 * @eventType com.threerings.msoy.client.ControlBar.DISPLAY_LIST_VALID
 */
[Event(name="displayListValid", type="com.threerings.util.ValueEvent")]

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
    /**
     * An event dispatched when the client is unminimized, and this component's
     * display list has been validated.
     *
     * @eventType displayListValid
     */
    public static const DISPLAY_LIST_VALID :String = "displayListValid";

    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 24;

    /** Different groups of UI elements. */
    public static const UI_ALL :String = "All UI Elements"; // created automatically
    public static const UI_STD :String = "Standard Member UI";
    public static const UI_MINI :String = "Member UI Mini";
    public static const UI_EDIT :String = "Member UI Edit";
    public static const UI_GUEST :String = "Guest UI";
    public static const UI_SIDEBAR :String = "Member UI Sidebar";
    public static const UI_VIEWER :String = "Room Entity Viewer";

    public static const ALL_UI_GROUPS :Array = [
        UI_ALL, UI_STD, UI_SIDEBAR, UI_MINI, UI_EDIT, UI_GUEST, UI_VIEWER ];

    public function init (ctx :MsoyContext, top :TopPanel) :void
    {
        _ctx = ctx;
        styleName = "controlBar";

        var cls :Class = getStyle("backgroundSkin");
        setStyle("backgroundImage", cls);

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        height = HEIGHT;
        percentWidth = 100;

        var fn :Function = function (event :ClientEvent) :void {
            checkControls();
            updateUI();
        };
        _ctx.getClient().addClientObserver(new ClientAdapter(fn, fn, null, null, null, null, fn));

        createControls();
        checkControls();
        updateUI();
    }

    /**
     * Redirects our chat input to the specified chat director.
     */
    public function setChatDirector (chatDtr :ChatDirector) :void
    {
        if (_chatControl != null) {
            _chatControl.setChatDirector(chatDtr);
        }
    }

    /**
     * Called by the TopPanel when we move to a new location. This should some day take over for
     * setLocation().
     */
    public function locationDidChange (place :PlaceObject) :void
    {
        // by default we do nothing, but our derived classes do things
    }

    /**
     * Add a custom component to the control bar.
     * Note that there is no remove: just do component.parent.removeChild(component);
     */
    public function addCustomComponent (comp :UIComponent) :void
    {
        if (_spacer.width != 0) {
            _spacer.addChild(comp);
        } else {
            addChild(comp);
        }
    }

    /**
     * Called when our minimization status has changed.
     */
    public function miniChanged () :void
    {
        _isMinimized = _ctx.getTopPanel().isMinimized();
        updateUI();
    }

    /**
     * Called to tell us when we're in sidebar mode.
     */
    public function inSidebar (sidebaring :Boolean) :void
    {
        _inSidebar = sidebaring;
        updateUI();
    }

    /**
     * Configures our spacer width.
     */
    public function setSpacerWidth (width :Number) :void
    {
        _spacer.width = width;
    }

    /**
     * Configures the chat input background color.
     */
    public function setChatColor (color :int) :void
    {
        _chatControl.setChatColor(color);
    }

    public function enableZoomControl (enabled :Boolean ) :void
    {
        _zoomBtn.enabled = enabled;
        _zoomBtn.toolTip = Msgs.GENERAL.get(enabled ? "i.zoom" : "i.zoom_disabled");
    }

    // from HBox
    override protected function updateDisplayList (w :Number, h :Number) :void
    {
        super.updateDisplayList(w, h);
        if (!_isMinimized && width != 0) {
            dispatchEvent(new ValueEvent(DISPLAY_LIST_VALID, true));
        }
    }

    /**
     * Creates the controls we'll be using.
     */
    protected function createControls () :void
    {
        _spacer = new HBox();
        _spacer.styleName = "controlBarSpacer";
        _spacer.height = this.height;
        _spacer.percentWidth = 100;

        _chatBtn = new CommandButton();
        _chatBtn.toolTip = Msgs.GENERAL.get("i.channel"); // i.chatPrefs
        _chatBtn.setCommand(MsoyController.POP_CHANNEL_MENU, _chatBtn); // MsoyController.CHAT_PREFS
        _chatBtn.styleName = "controlBarButtonChat";

        _volBtn = new CommandButton();
        _volBtn.toolTip = Msgs.GENERAL.get("i.volume");
        _volBtn.setCommand(MsoyController.POP_VOLUME, _volBtn);
        _volBtn.styleName = "controlBarButtonVolume";

        _zoomBtn = new CommandButton();
        _zoomBtn.toolTip = Msgs.GENERAL.get("i.zoom");
        _zoomBtn.setCommand(MsoyController.POP_ZOOM, _zoomBtn);
        _zoomBtn.styleName = "controlBarButtonZoom";
    }

    /**
     * Checks to see which controls the client should see.
     */
    protected function checkControls () :Boolean
    {
        var isMember :Boolean = (_ctx.getMyName() != null &&
                                 !MemberName.isGuest(_ctx.getMyName().getMemberId()));
        if (numChildren > 0 && (isMember == _isMember)) {
            return false;
        }

        removeAllChildren();
        clearAllGroups();

        addGroupChild(_spacer, [ UI_SIDEBAR ]);

        // add our standard control bar features
        addGroupChild(_chatBtn, [ UI_STD, UI_MINI, UI_EDIT, UI_GUEST, UI_SIDEBAR ]);
        _chatControl = null;
        _chatControl = new ChatControl(
            _ctx, Msgs.CHAT.get("b.send"), this.height, this.height - 4);
        addGroupChild(_chatControl, [ UI_STD, UI_MINI, UI_EDIT, UI_GUEST, UI_SIDEBAR, UI_VIEWER ]);
        addGroupChild(_volBtn, [ UI_STD, UI_MINI, UI_GUEST, UI_EDIT, UI_SIDEBAR /*,UI_VIEWER */]);
        addGroupChild(_zoomBtn, [ UI_STD, UI_GUEST, UI_EDIT ]);

        // add our various control buttons
        addControlButtons();

        var blank :Canvas = new Canvas();
        blank.styleName = "controlBarSpacer";
        blank.height = this.height;
        blank.percentWidth = 100;
        addGroupChild(blank, [ UI_STD, UI_EDIT, UI_MINI, UI_GUEST, UI_SIDEBAR, UI_VIEWER ]);

        // and remember how things are set for now
        _isMember = isMember;
        _isMinimized = false;
        return true;
    }

    protected function addControlButtons () :void
    {
        // derived classes can add other buttons here
    }

    protected function clearAllGroups () :void
    {
        for each (var key :String in ALL_UI_GROUPS) {
            if (_groups[key] == null) {
                _groups[key] = new Array();
            } else {
                _groups[key].length = 0;
            }
        }
    }

    protected function addGroupChild (child :UIComponent, groupNames :Array) :void
    {
        addChild(child);
        if (!ArrayUtil.contains(groupNames, UI_ALL)) {
            groupNames.push(UI_ALL);
        }
        for each (var groupName :String in groupNames) {
            _groups[groupName].push(child);
        }
    }

    protected function updateGroup (groupName :String, value :Boolean) :void
    {
        var elt :UIComponent = null;
        for each (elt in _groups[groupName]) {
            elt.visible = elt.includeInLayout = value;
        }
    }

    protected function updateUI () :void
    {
        updateGroup(UI_ALL, false);
        updateGroup(getMode(), true);
    }

    protected function getMode () :String
    {
        if (_isMinimized) {
            return UI_MINI;
        } else if (_inSidebar) {
            return UI_SIDEBAR;
        } else if (_isMember) {
            return UI_STD;
        } else {
            return UI_GUEST;
        }
    }

    /** Our clientside context. */
    protected var _ctx :MsoyContext;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** Are we in a minimized mode? */
    protected var _isMinimized :Boolean;

    /** Are we in a sidebar? */
    protected var _inSidebar :Boolean;

    /** Object that contains all the different groups of UI elements. */
    protected var _groups :Object = new Object();

    /** Our chat control. */
    protected var _chatControl :ChatControl;

    /** The chat preferences button. */
    protected var _chatBtn :CommandButton;

    /** Handles volume adjustment. */
    protected var _volBtn :CommandButton;

    /** Handles room zooming. */
    protected var _zoomBtn :CommandButton;

    /** Bookend image at the other end of name label. */
    protected var _bookend :SkinnableImage;

    /** A spacer to bump the UI bits over to the right if needed */
    protected var _spacer :HBox;
}
}
