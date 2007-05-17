//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.binding.utils.BindingUtils;
import mx.binding.utils.ChangeWatcher;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Button;
import mx.controls.Spacer;

import mx.events.FlexEvent;

import com.threerings.flex.CommandButton;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.world.client.RoomController;

import com.threerings.msoy.chat.client.ChatControl;

[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 24;

    /** Different groups of UI elements. */
    public static const UI_ALL :String = "All UI Elements"; // created automatically
    public static const UI_STD :String = "Standard Member UI";
    public static const UI_MINI :String = "Member UI Mini";
    public static const UI_EDIT :String = "Member UI Edit";
    public static const UI_GUEST :String = "Guest UI";

    public static const ALL_UI_GROUPS :Array = [ UI_ALL, UI_STD, UI_MINI, UI_EDIT, UI_GUEST ];
    
    public function ControlBar (ctx :WorldContext, top :TopPanel)
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
        };
        _ctx.getClient().addClientObserver(new ClientAdapter(fn, fn, null, null, null, null, fn));

        _controller = new ControlBarController(ctx, top, this);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);

        _ctx.getClient().addEventListener(WorldClient.MINI_WILL_CHANGE, miniWillChange);
        _ctx.getClient().addEventListener(WorldClient.EMBEDDED_STATE_KNOWN, embeddedStateKnown);

        checkControls();
    }

    /**
     * Enables or disables our chat input.
     */
    public function setChatEnabled (enabled :Boolean) :void
    {
        if (_chatControl != null) {
            _chatControl.setEnabled(enabled);
        }
    }

    /**
     * Called by the ChannelChatPanel when it needs to stuff a chat input field into the control
     * bar while it's open. Setting it to null removes it.
     */
    public function setChannelChatInput (input :Container) :void
    {
        if (_channelInput != null) {
            removeChild(_channelInput);
            _channelInput = null;
        }
        if (input != null) {
            var chidx :int = -1;
            // TEMP: non-admins have no channel button for now
            if (_channelBtn == null) {
                chidx = numChildren;
            } else {
                // insert it to the left of the channel button
                chidx = getChildIndex(_channelBtn);
            }
            if (chidx >= 0) {
                addChildAt(_channelInput = input, chidx);
            }
        }
    }

    /**
     * Check to see which controls the client should see.
     */
    protected function checkControls () :void
    {
        var user :MemberObject = _ctx.getMemberObject();
        var isMember :Boolean = (user != null) && !user.isGuest();
        if (numChildren > 0 && (isMember == _isMember)) {
            return;
        }

        removeAllChildren();
        clearAllGroups();
        
        _chatControl = null;
        _avatarBtn = null;
        _editBtn = null;
        _channelBtn = null;

        _chatControl = new ChatControl(_ctx, this.height - 4);
        addGroupChild(_chatControl, [ UI_STD, UI_EDIT ]);
        
        var chatBtn :CommandButton = new CommandButton();
        chatBtn.toolTip = Msgs.GENERAL.get("i.chatPrefs");
        chatBtn.setCommand(MsoyController.CHAT_PREFS);
        chatBtn.styleName = "controlBarButtonChat";
        addGroupChild(chatBtn, [ UI_STD ]);

        var volBtn :CommandButton = new CommandButton();
        volBtn.toolTip = Msgs.GENERAL.get("i.volume");
        volBtn.setCommand(ControlBarController.POP_VOLUME, volBtn);
        volBtn.styleName = "controlBarButtonVolume";
        addGroupChild(volBtn, [ UI_STD, UI_GUEST, UI_EDIT ]);

        _avatarBtn = new CommandButton();
        _avatarBtn.toolTip = Msgs.GENERAL.get("i.avatar");
        _avatarBtn.setCommand(MsoyController.PICK_AVATAR);
        _avatarBtn.styleName = "controlBarButtonAvatar";
        addGroupChild(_avatarBtn, [ UI_STD ]);

        _petBtn = new CommandButton();
        _petBtn.toolTip = Msgs.GENERAL.get("i.pet");
        _petBtn.setCommand(MsoyController.SHOW_PETS);
        _petBtn.styleName = "controlBarButtonPet";
        addGroupChild(_petBtn, [ UI_STD ]);

        _roomeditBtn = new CommandButton();
        _roomeditBtn.toolTip = Msgs.GENERAL.get("i.editScene");
        _roomeditBtn.setCommand(ControlBarController.ROOM_EDIT, _roomeditBtn);
        _roomeditBtn.styleName = "controlBarButtonEdit";
        _roomeditBtn.enabled = false;
        addGroupChild(_roomeditBtn, [ UI_STD ]);
        
        if (_ctx.getWorldClient().isEmbedded()) {
            _logonPanel = new LogonPanel(_ctx, this.height - 4);
            addGroupChild(_logonPanel, [ UI_GUEST ]);
        }

        // some elements that are common to guest and logged in users
        var footerLeft :SkinnableImage = new SkinnableImage();
        footerLeft.styleName = "controlBarFooterLeft";
        addGroupChild(footerLeft, [ UI_STD, UI_GUEST ]);

        var blank :Canvas = new Canvas();
        blank.styleName = "controlBarSpacer";
        blank.height = this.height;
        blank.percentWidth = 100;
        addGroupChild(blank, [ UI_STD, UI_MINI, UI_GUEST ]);

        var footerRight :SkinnableImage = new SkinnableImage();
        footerRight.styleName = "controlBarFooterRight";
        addGroupChild(footerRight, [ UI_STD, UI_GUEST ]);

        _channelBtn = new CommandButton();
        _channelBtn.toolTip = Msgs.GENERAL.get("i.channel");
        _channelBtn.setCommand(MsoyController.POP_CHANNEL_MENU, _channelBtn);
        _channelBtn.styleName = "controlBarButtonChannel";
        addGroupChild(_channelBtn, [ UI_STD, UI_MINI ]);

        // and remember how things are set for now
        _isMember = isMember;
        _isMinimized = false;
        _isEditing = false;

        updateUI();
        
        recheckAvatarControl();
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
        for each (var groupName :String in groupNames) {
            if (groupName != UI_ALL) {
                _groups[groupName].push(child);
            }
            _groups[UI_ALL].push(child);
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
        if (_isMember) {
            if (_isMinimized) {
                updateGroup(UI_MINI, true);
            } else if (_isEditing) {
                updateGroup(UI_EDIT, true);
            } else {
                updateGroup(UI_STD, true);
            }
        } else {
            updateGroup(UI_GUEST, true);
        }
    }   
    
    protected function embeddedStateKnown (event :ValueEvent) :void
    {
        var embedded :Boolean = (event.value as Boolean);
        // no logon panel if we're not in embedded mode
        if (!embedded && _logonPanel != null) {
            removeChild(_logonPanel);
        }
    }

    protected function miniWillChange (event :ValueEvent) :void
    {
        _isMinimized = (event.value as Boolean);
        _isEditing = (_isEditing && ! _isMinimized);
        updateUI();
    }

    protected function handleAddRemove (event :Event) :void
    {
        var added :Boolean = (event.type == Event.ADDED_TO_STAGE);
        _controller.registerForSessionObservations(added);

        if (added) {
            _avatarControlWatcher = BindingUtils.bindSetter(
                recheckAvatarControl, _ctx.worldProps, "userControlsAvatar");

        } else {
            _avatarControlWatcher.unwatch();
            _avatarControlWatcher = null;
        }
    }

    protected function recheckAvatarControl (... ignored) :void
    {
        if (_avatarBtn != null) {
            _avatarBtn.enabled = _ctx.worldProps.userControlsAvatar;
        }
    }

    /** Changes the visibility and parameters of the navigation widgets.
     *  @param visible controls visibility of both the name and the back button
     *  @param name specifies the location name to be displayed on the control bar
     *  @param backEnabled specifies whether the back button should be enabled
     */
    public function updateNavigationWidgets (
        visible :Boolean, name :String, backEnabled :Boolean) :void
    {
        // don't do navigation here for now...
        /*const maxLen :int = 25;
        _loc.includeInLayout = _goback.includeInLayout = _bookend.includeInLayout =
            _loc.visible = _goback.visible = _bookend.visible = visible;
        _goback.enabled = backEnabled;
        _goback.toolTip = backEnabled ? Msgs.GENERAL.get("i.goBack") : null;
        if (name != null) {
            _loc.text = name.length < maxLen ? name : (name.substr(0, maxLen) + "...");
        } else {
            _loc.text = "";
        }*/
    }

    /** Receives notification whether scene editing is possible for this scene. */
    public function set sceneEditPossible (value :Boolean) :void
    {
        if (_editBtn != null) {
            _editBtn.enabled = value;
        }
        if (_roomeditBtn != null) {
            _roomeditBtn.enabled = value;
        }
    }

    /** Our clientside context. */
    protected var _ctx :WorldContext;

    /** Controller for this object. */
    protected var _controller :ControlBarController;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** Are we in a minimized mode? */
    protected var _isMinimized :Boolean;

    /** Are we in room editing mode? */
    protected var _isEditing :Boolean;
    
    /** The back-movement button. */
    protected var _goback :CommandButton;

    /** Object that contains all the different groups of UI elements. */
    protected var _groups :Object = new Object();
    
    /** A list of children that can be hidden when we are minimized. */
    //protected var _hiders :Array = new Array();

    /** Our chat control. */
    protected var _chatControl :ChatControl;

    /** Our logon panel (if shown). */
    protected var _logonPanel :LogonPanel;

    /** Button for changing your avatar. */
    protected var _avatarBtn :CommandButton;

    /** Button for managing pets. */
    protected var _petBtn :CommandButton;

    /** Button for editing the current scene. */
    protected var _editBtn :CommandButton;
    protected var _roomeditBtn :CommandButton;

    /** Button for selecting/creating chat channels. */
    protected var _channelBtn :CommandButton;

    /** Our channel chat input. */
    protected var _channelInput :Container;

    /** Notifies us of changes to the userControlsAvatar property. */
    protected var _avatarControlWatcher :ChangeWatcher;

    /** Current location label. */
    protected var _loc :CanvasWithText;

    /** Bookend image at the other end of name label. */
    protected var _bookend :SkinnableImage;
}
}


import flash.display.DisplayObject;
import flash.text.TextFieldAutoSize;
import mx.containers.Canvas;
import mx.controls.Image;
import mx.core.IFlexDisplayObject;
import mx.core.ScrollPolicy;
import mx.core.UITextField;

/** Internal: helper function that extends ms.control.Image functionality with automatic image
 * loading from the style sheet (e.g. via an external style sheet file). */
[Style(name="backgroundSkin", type="Class", inherit="no")]
internal class SkinnableImage extends Image
{
    public function SkinnableImage ()
    {
    }

    override public function styleChanged (styleProp:String) :void
    {
        super.styleChanged(styleProp);

        var cls : Class = Class(getStyle("backgroundSkin"));
        if (cls != null) {
            updateSkin(cls);
        }
    }

    protected function updateSkin (skinclass : Class) : void
    {
        if (_skin != null) {
            removeChild(_skin);
        }

        _skin = DisplayObject (IFlexDisplayObject (new skinclass()));
        this.width = _skin.width;
        this.height = _skin.height;
        _skin.x = 0;
        _skin.y = 0;
        addChild(_skin);
    }

    protected var _skin : DisplayObject;
}

/** Internal: helper class that extends ms.containers.Canvas
    with automatic background loading from the style sheet (e.g. via an
    external style sheet file). */
internal class CanvasWithText extends Canvas
{
    public var textfield :UITextField;

    public function CanvasWithText (height :int)
    {
        this.height = height;
        horizontalScrollPolicy = verticalScrollPolicy = ScrollPolicy.OFF;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        textfield = new UITextField ();
        textfield.styleName = "controlBarText";
        textfield.x = 5;
        textfield.y = 0;
        textfield.height = height;
        textfield.width = width;
        textfield.autoSize = TextFieldAutoSize.LEFT;
        addChild(textfield);
    }

    public function set text (message :String) :void
    {
        textfield.text = message;
        textfield.y = (this.height - textfield.textHeight) / 2;
    }

    public function get text () :String
    {
        return textfield.text;
    }
}
