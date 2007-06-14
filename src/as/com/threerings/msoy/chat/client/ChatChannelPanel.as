//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObject;

import flash.events.Event;

import mx.core.mx_internal;

import mx.containers.HBox;
import mx.containers.TabNavigator;
import mx.containers.VBox;

import mx.events.ChildExistenceChangedEvent;
import mx.events.FlexEvent;
import mx.utils.StringUtil;

import flexlib.containers.SuperTabNavigator;
import flexlib.controls.tabBarClasses.SuperTab;

import com.threerings.flex.CommandButton;
import com.threerings.util.CommandEvent;
import com.threerings.util.HashMap;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.data.ChatChannel;

/**
 * Displays our various chat channels in tabs.
 */
public class ChatChannelPanel extends VBox
{
    public function ChatChannelPanel (ctx :WorldContext)
    {
        _ctx = ctx;
        width = TopPanel.RIGHT_SIDEBAR_WIDTH;

        addChild(_tabnav = new ChatTabNavigator());
        _tabnav.closePolicy = SuperTab.CLOSE_SELECTED; // can't do this in the constructor, yay!
        _tabnav.percentWidth = 100;
        _tabnav.percentHeight = 100;
        _tabnav.addEventListener(ChildExistenceChangedEvent.CHILD_REMOVE, tabRemoved);

        // create a UI for sending chat which we'll show when we're active
        _inputBox = new HBox();
        _inputBox.styleName = "chatControl";
        _inputBox.addChild(_input = new ChatInput());
        _input.height = ControlBar.HEIGHT-4;
        _input.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
        var send :CommandButton = new CommandButton();
        send.label = Msgs.CHAT.get("b.send");
        send.setCallback(sendChat);
        send.height = ControlBar.HEIGHT-4;
        _inputBox.addChild(send);
    }

    /**
     * Locates the specified chat display, returning null if it is not open.
     */
    public function findChatDisplay (channel :ChatChannel) :ChatDisplay
    {
        var tab :ChannelChatTab = findChatTab(channel);
        return (tab == null) ? null : tab.getOverlay();
    }

    /**
     * Returns the chat display to use for the specified channel.
     */
    public function getChatDisplay (
        channel :ChatChannel, history :HistoryList, select :Boolean) :ChatDisplay
    {
        var tab :ChannelChatTab = findChatTab(channel);

        // create a new tab if we did not find one already in use
        if (tab == null) {
            tab = new ChannelChatTab(_ctx, channel);
            tab.label = channel.ident.toString();
            tab.getOverlay().setHistory(history);
            tab.init((_ctx.getChatDirector() as MsoyChatDirector).getChannelObject(channel));
            _tabnav.addChild(tab);
        }

        selectTab(tab, select);

        return tab.getOverlay();
    }

    /**
     * Iterates over chat tabs, returning the first one that passes the /predicate/ function.
     * @param predicate Function of the form: <pre>function (tab :ChatTab) :Boolean</pre>
     */
    protected function findAnyTab (predicate :Function) :ChatTab
    {
        for (var ii :int = _tabnav.numChildren - 1; ii >= 0; ii--) {
            var tab :ChatTab = _tabnav.getChildAt(ii) as ChatTab;
            if (predicate(tab)) {
                return tab;
            }
        }
        return null;
    }
    
    /**
     * Find the ChannelChatTab instance being used for the specified ChatChannel.
     */
    protected function findChatTab (channel :ChatChannel) :ChannelChatTab
    {
        return findAnyTab(function (tab :ChatTab) :Boolean {
                var channeltab :ChannelChatTab = tab as ChannelChatTab;
                return (channeltab != null && channeltab.channel.equals(channel));
            }) as ChannelChatTab;
    }

    /**
     * Returns a named page display tab. If this named tab does not exist, it creates
     * a new one, and fills its page with contents from the specified location.
     */
    public function displayPageTab (
        tabName :String, pageUrl :String, select :Boolean = true) :PageDisplayTab
    {
        // find the tab
        var tab :PageDisplayTab = findAnyTab(function (tab :ChatTab) :Boolean {
                var pagetab :PageDisplayTab = tab as PageDisplayTab;
                return (pagetab != null && pagetab.tabName == tabName);
            }) as PageDisplayTab;

        // the display doesn't exist - let's create one
        if (tab == null) {
            tab = new PageDisplayTab(_ctx, tabName);
            tab.label = tabName;
            tab.init();
            _tabnav.addChild(tab);
        }
        
        // start loading the page
        CommandEvent.dispatch(tab, PageDisplayController.HELP_PAGE_DISPLAY_COMMAND, pageUrl);

        // try to guess a css url from the page url, and maybe start loading it
        var segments :Array = pageUrl.split(/(.+)\.html$/);
        if (segments.length == 3) {
            var cssUrl :String = String(segments[1] + ".css");
            CommandEvent.dispatch(tab, PageDisplayController.HELP_PAGE_SET_STYLE_COMMAND, cssUrl);
        }
        
        selectTab(tab, select);

        return tab;
    }    

    public function sendRoomToTab () :void
    {
        _wtab = new WorldChatTab(_ctx);
        _wtab.label = Msgs.CHAT.xlate("l.world_channel");
        _tabnav.addChildAt(_wtab, 0);
        _tabnav.setClosePolicyForTab(0, SuperTab.CLOSE_NEVER);
        selectTab(_wtab);
        _ctx.getTopPanel().getControlBar().setTabMode(true);
    }

    public function removeRoomTab () :void
    {
        if (_wtab != null) {
            _ctx.getTopPanel().getControlBar().setTabMode(false);
            _tabnav.removeChild(_wtab);
            _wtab.shutdown();
            _wtab = null;
        }
    }

    public function containsRoomTab () :Boolean
    {
        return _wtab != null;
    }

    protected function selectTab (tab :ChatTab, select :Boolean = true) :void
    {
        // select this tab if requested
        if (select) {
            // We need to defer the action of selecting a tab for one frame.  If the tab is newly
            // added, attempting to select it immediately does not work, for some wacky reason.
            var thisPanel :ChatChannelPanel = this;
            var frameListener :Function;
            frameListener = function (event :Event) :void {
                _tabnav.selectedChild = tab;
                thisPanel.removeEventListener(Event.ENTER_FRAME, frameListener);
            }
            thisPanel.addEventListener(Event.ENTER_FRAME, frameListener);
        }

        // if we're not visible, add ourselves
        if (parent == null) {
            _ctx.getTopPanel().setRightPanel(this);
            _ctx.getTopPanel().getControlBar().setChannelChatInput(_inputBox);
        }
    }
    
    protected function tabRemoved (event :ChildExistenceChangedEvent) :void
    {
        if (event.relatedObject is ChatTab) {
            if (_tabnav.numChildren == 1) {
                _ctx.getTopPanel().clearRightPanel(this);
                _ctx.getTopPanel().getControlBar().setChannelChatInput(null);
            }
        }
        if (event.relatedObject is ChannelChatTab) {
            (event.relatedObject as ChannelChatTab).shutdown();
            var channel :ChatChannel = (event.relatedObject as ChannelChatTab).channel;
            (_ctx.getChatDirector() as MsoyChatDirector).closeChannel(channel);
        }
        if (event.relatedObject is PageDisplayTab) {
            (event.relatedObject as PageDisplayTab).shutdown();
        }
    }

    /**
     * Called when the user presses enter in the chat input field or clicks the "Send" button.
     */
    protected function sendChat (... ignored) :void
    {
        var message :String = StringUtil.trim(_input.text);
        if ("" == message) {
            return;
        }

        var tab :ChatTab = (_tabnav.getChildAt(_tabnav.selectedIndex) as ChatTab);
        if (tab != null) {
            tab.sendChat(message);
            _input.text = "";
        } else {
            Log.getLog(this).warning("Missing selected chat tab?! Dropping '" + message + "'.");
        }
    }

    protected var _ctx :WorldContext;
    protected var _tabnav :SuperTabNavigator;
    protected var _wtab :WorldChatTab;
    protected var _inputBox :HBox;
    protected var _input :ChatInput;
}
}
