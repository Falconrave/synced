//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.FullScreenEvent;

import flash.utils.Dictionary;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.HBox;

import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.Integer;

import com.threerings.flash.DisplayUtil;

import com.threerings.flex.ChatControl;
import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.ScalingMediaContainer;
import com.threerings.msoy.ui.SliderPopup;
import com.threerings.msoy.ui.skins.VolumeButton;

import com.threerings.msoy.chat.client.BaseChatDirector;
import com.threerings.msoy.notify.client.NotificationDisplay;

import com.threerings.msoy.world.client.WorldController;

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 28; // if you feel like changing this, don't.

    /** Button priorities */
    public static const VOLUME_PRIORITY :int = 0;
    public static const GLOBAL_PRIORITY :int = 100;
    public static const DEFAULT_PRIORITY :int = 200;
    public static const PLACE_PRIORITY :int = 300;

    /** The chat preferences button. */
    public var chatOptsBtn :CommandButton;

    /** Handles volume adjustment. */
    public var volBtn :CommandButton;

    /** Handles recent scenes and other destinations. */
    public var goBtn :CommandButton;

    /** Handles full screening. */
    public var fullBtn :CommandButton;

    /** Handles bringing up a share dialog. */
    public var shareBtn :CommandButton;

    /** Indicates game media loading and handles game menu. */
    public var gameBtn :CommandButton;

    // TEMP
    public function isFullOn () :Boolean
    {
        return _fullOn;
    }
    protected var _fullOn :Boolean;
    public function setFullOn () :void
    {
        _fullOn = true;
        updateUI();
    }

    /**
     * Construct.
     */
    public function ControlBar (ctx :MsoyContext)
    {
        _ctx = ctx;
        // the rest is in init()

        Prefs.events.addEventListener(ConfigValueSetEvent.CONFIG_VALUE_SET, handleConfigValueSet);
    }

    public function init (top :TopPanel) :void
    {
        styleName = "controlBar";
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        height = HEIGHT;
        percentWidth = 100;

        _ctx.getClient().addClientObserver(
            new ClientAdapter(null, checkControls, checkControls, null, checkControls, null, null,
                checkControls));

        _buttons = new ButtonPalette(top);

        createControls();
        checkControls();

        _ctx.getUIState().addEventListener(UIState.STATE_CHANGE, handleUIStateChanged);
        _ctx.getStage().addEventListener(FullScreenEvent.FULL_SCREEN, handleFullScreenChanged);
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

    public function setNotificationDisplay (notificationDisplay :NotificationDisplay) :void
    {
        _notificationDisplay = notificationDisplay;
        setupControls();
        updateUI();
    }

    /**
     * Gives the chat input focus.
     */
    public function giveChatFocus () :void
    {
        if (_chatControl != null) {
            _chatControl.setFocus();
        }
    }

    /**
     * Add a custom component to the control bar.
     * Note that there is no remove: just do component.parent.removeChild(component);
     */
    public function addCustomComponent (comp :UIComponent) :void
    {
        // no priority set- becomes priority = 0.
        addChild(comp);
        sortControls();
    }

    public function addCustomButton (comp :UIComponent, priority :int = DEFAULT_PRIORITY) :void
    {
        _priorities[comp] = priority;
        _buttons.addButton(comp, priority);
        sortControls();
        _buttons.recheckButtons();
    }

    /**
     * Called to tell us what the game button should look like.
     */
    public function setGameButtonIcon (icon :MediaDesc) :void
    {
        setGameButtonStyle(icon);
    }

    /**
     * Configures the chat input background color.
     */
    public function setChatColor (color :int) :void
    {
        _chatControl.setChatColor(color);
    }

    // from Container
    override public function setActualSize (uw :Number, uh :Number) :void
    {
        super.setActualSize(uw, uh);

        if (_notificationDisplay != null && _notificationDisplay.visible) {
            callLater(_notificationDisplay.updatePopupLocation);
        }
    }

    /**
     * Creates the controls we'll be using.
     */
    protected function createControls () :void
    {
        _chatControl = new ChatControl(_ctx, Msgs.GENERAL.get("b.chat_send"));
        _chatControl.chatInput.height = HEIGHT - 8;
        _chatControl.chatInput.maxChars = BaseChatDirector.MAX_CHAT_LENGTH;

        chatOptsBtn = createButton("controlBarButtonChat", "i.channel");
        chatOptsBtn.toggle = true;
        chatOptsBtn.setCommand(WorldController.POP_CHANNEL_MENU, chatOptsBtn);

        volBtn = createButton("imageButton", "i.volume");
        updateVolumeSkin(Prefs.getSoundVolume());
        volBtn.setCallback(handlePopVolume);

        goBtn = createButton("controlBarButtonGo", "i.go");
        goBtn.toggle = true;
        goBtn.setCommand(MsoyController.POP_GO_MENU, goBtn);

        fullBtn = createButton("controlBarButtonFull", "i.full");
        fullBtn.setCommand(MsoyController.SET_DISPLAY_STATE);

        shareBtn = createButton("controlBarButtonShare", "i.share");
        shareBtn.toggle = true;
        shareBtn.setCallback(FloatingPanel.createPopper(function () :ShareDialog {
            return new ShareDialog(_ctx);
        }, shareBtn));

        gameBtn = createButton("controlBarGameButton", "i.game");
        gameBtn.toggle = true;
        gameBtn.setCommand(WorldController.POP_GAME_MENU, gameBtn);
    }

    protected function createButton (style :String, tipKey :String) :CommandButton
    {
        var cb :CommandButton = new CommandButton();
        cb.styleName = style;
        cb.toolTip = Msgs.GENERAL.get(tipKey);
        return cb;
    }

    /**
     * Checks to see which controls the client should see.
     */
    protected function checkControls (... ignored) :void
    {
        const isLoggedOn :Boolean = _ctx.getClient().isLoggedOn();
        chatOptsBtn.enabled = isLoggedOn;
        _chatControl.enabled = isLoggedOn;

        // if we're already set up, then we're done
        if (numChildren > 0) {
            return;
        }

        // and add our various control buttons
        setupControls();
        updateUI();
    }

    protected function setupControls () :void
    {
        removeAllChildren();
        _buttons.clearButtons();
        _priorities = new Dictionary(true);
        _conditions = new Dictionary(true);
        addControls();
    }

    protected function sortControls () :void
    {
        DisplayUtil.sortDisplayChildren(this, comparePriority);
        DisplayUtil.sortDisplayChildren(volBtn.parent, comparePriority);
    }

    protected function addControls () :void
    {
        var state :UIState = _ctx.getUIState();

        // visibility conditions for our buttons
        function showChat () :Boolean {
            return isNotInViewer() && state.showChat;
        }

        function showShare () :Boolean {
            return state.inRoom || state.inGame;
        }

        function showGame () :Boolean {
            return state.inGame || state.inAVRGame;
        }

        // add our standard control bar features
        // TODO: show chat in lobby
        addControl(chatOptsBtn, showChat, CHAT_SECTION);
        addControl(_chatControl, showChat, CHAT_SECTION);
        addControl(_buttons, true, BUTTON_SECTION);

        // add buttons
        addButton(volBtn, true, VOLUME_PRIORITY);
        addButton(goBtn, isNotInViewer, GLOBAL_PRIORITY);
        addButton(fullBtn, isFullOn, GLOBAL_PRIORITY);

        addButton(shareBtn, showShare);
        addButton(gameBtn, showGame);

        if (_notificationDisplay != null) {
            addControl(_notificationDisplay, isNotInViewer, NOTIFICATION_SECTION);
        }
    }

    protected function isNotInViewer () :Boolean
    {
        return true;
    }

    /**
     * Used to sort the buttons.
     */
    protected function comparePriority (comp1 :Object, comp2 :Object) :int
    {
        const pri1 :int = int(_priorities[comp1]);
        const pri2 :int = int(_priorities[comp2]);
        return Integer.compare(pri1, pri2);
    }

    protected function addControl (child :UIComponent, condition :Object, section :int) :void
    {
        addChild(child);
        registerControl(child, condition, section);
    }

    protected function addButton (
        child :UIComponent, condition :Object, priority :int = DEFAULT_PRIORITY) :void
    {
        _buttons.addButton(child, priority);
        registerControl(child, condition, priority);
    }

    protected function registerControl (child :UIComponent, condition :Object, priority :int) :void
    {
        _priorities[child] = priority;
        _conditions[child] = condition;
    }

    protected function updateUI () :void
    {
        for (var key :* in _conditions) {
            var condition :Object = _conditions[key];
            if (condition is Function) {
                condition = (condition as Function).call(null);
            }
            FlexUtil.setVisible(UIComponent(key), Boolean(condition));
        }

        sortControls();
        _buttons.recheckButtons();
    }

    protected function setGameButtonStyle (icon :MediaDesc) :void
    {
        if (icon == null) {
            gameBtn.styleName = "controlBarGameButton";
        } else {
            var smc :ScalingMediaContainer = new ScalingMediaContainer(22, 22);
            smc.setMediaDesc(icon);
            gameBtn.setStyle("image", smc);
        }
    }

    protected function handlePopVolume () :void
    {
        var dfmt :Function = function (value :Number) :String {
            return Msgs.GENERAL.get("i.percent_fmt", ""+Math.floor(value*100));
        };
        SliderPopup.toggle(volBtn, Prefs.getSoundVolume(), Prefs.setSoundVolume,
            { styleName: "volumeSlider", tickValues: [ 0, 1 ], dataTipFormatFunction: dfmt });
    }

    protected function handleConfigValueSet (event :ConfigValueSetEvent) :void
    {
        if (event.name == Prefs.VOLUME) {
            updateVolumeSkin(Number(event.value));
        }
    }

    protected function updateVolumeSkin (level :Number) :void
    {
        volBtn.setStyle("image", VolumeButton.getImage(level));
    }

    protected function handleUIStateChanged (event :Event) :void
    {
        updateUI();
    }

    protected function handleFullScreenChanged (event :FullScreenEvent) :void
    {
        // TODO: different icon for going up or down?
        fullBtn.selected = event.fullScreen;

        // seems to be necessary when we leave fullscreen
        callLater(updateUI);
    }

    protected static const CHAT_SECTION :int = -2;
    protected static const BUTTON_SECTION :int = -1;
    // implicit: custom controls section = 0
    protected static const NOTIFICATION_SECTION :int = 1;

    /** Our clientside context. */
    protected var _ctx :MsoyContext;

    /** Button visibility conditions. */
    protected var _conditions :Dictionary = new Dictionary(true);

    /** Button priority levels. */
    protected var _priorities :Dictionary = new Dictionary(true);

    /** Our chat control. */
    protected var _chatControl :ChatControl;

    /** Holds the 22x22 button area. */
    protected var _buttons :ButtonPalette;

    /** Displays incoming notifications. */
    protected var _notificationDisplay :NotificationDisplay;
}
}
