//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;

import flash.geom.Rectangle;

import mx.containers.HBox;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.util.Log;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.MsoyContext;

public class GameChatContainer extends LayeredContainer
{
    public function GameChatContainer (ctx :MsoyContext, chatDtr :ChatDirector, 
        playerList :UIComponent)
    {
        _ctx = ctx;
        _chatDtr = chatDtr;

        width = TopPanel.RIGHT_SIDEBAR_WIDTH;
        styleName = "gameChatContainer";

        _chatDtr = chatDtr;
        _playerList = playerList;
        _playerList.width = width - 3;
        _listContainer = new HBox();
        _listContainer.styleName = "gameListContainer";
        _listContainer.height = _playerList.height;
        _listContainer.width = width - 3;
        _listContainer.addChild(_playerList);
        addChild(_listContainer);

        var tabs :UIComponent = _ctx.getTopPanel().getHeaderBar().removeTabsContainer();
        _tabBar = new HBox();
        _tabBar.horizontalScrollPolicy = ScrollPolicy.OFF;
        _tabBar.y = _playerList.height
        _tabBar.height = _ctx.getTopPanel().getHeaderBar().height;
        _tabBar.width = width - 3;
        _tabBar.styleName = "headerBar";
        _tabBar.addChild(tabs);
        addChild(_tabBar);

        var controlBar :ControlBar = _ctx.getTopPanel().getControlBar();
        //controlBar.inSidebar(true);
        controlBar.setChatDirector(_chatDtr);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
    }
    
    public function getChatOverlay () :ChatOverlay
    {
        return _overlay;
    }

    public function shutdown () :void
    {
        if (_channelOccList != null && _channelOccList.parent == this) {
            _channelOccList.width = 316;
            removeChild(_channelOccList);
            _channelOccList = null;
        }

        clearOverlay();

        _ctx.getTopPanel().getHeaderBar().replaceTabsContainer();
        var controlBar :ControlBar = _ctx.getTopPanel().getControlBar();
        //controlBar.inSidebar(false);
        controlBar.setChatDirector(_ctx.getMsoyChatDirector());
    }

    public function sendChat (message :String) :void
    {
        var result :String = _chatDtr.requestChat(null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _chatDtr.displayFeedback(null, result);
        }
    }

    public function displayOccupantList (occList :UIComponent) :void
    {
        if (_playerList.parent == _listContainer) {
            _listContainer.removeChild(_playerList);
        }

        if (_channelOccList != null) {
            if (_channelOccList.parent == _listContainer) {
                _listContainer.removeChild(_channelOccList);
            }
            _channelOccList.width = 316;
            _channelOccList = null;
        }

        if (occList == null) {
            _listContainer.addChild(_playerList);
        } else {
            _channelOccList = occList;
            _channelOccList.width = TopPanel.RIGHT_SIDEBAR_WIDTH - 3;
            _listContainer.addChild(_channelOccList);
        }
    }

    override public function setActualSize (uw :Number, uh :Number) :void
    {
        if (_overlay != null && (width != uh || height != uh)) {
            var chatTop :Number = _tabBar.y + _tabBar.height;
            _overlay.setTargetBounds(new Rectangle(0, chatTop, uw - 3, uh - chatTop));
        }

        super.setActualSize(uw, uh);
    }

    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            initOverlay();
        } else {
            clearOverlay();
        }
    }

    protected function initOverlay () :void
    {
        clearOverlay();
        _overlay = new ChatOverlay(_ctx, this, ChatOverlay.SCROLL_BAR_RIGHT, false);
        _overlay.setClickableGlyphs(true);
        // this overlay needs to listen on both the msoy and game chat directors
        _chatDtr.addChatDisplay(_overlay);
        _ctx.getMsoyChatDirector().addChatDisplay(_overlay);
        // the bounds will get set via setActualSize();
    }

    protected function clearOverlay () :void
    {
        if (_overlay == null) {
            return;
        }

        _chatDtr.removeChatDisplay(_overlay);
        _ctx.getMsoyChatDirector().removeChatDisplay(_overlay);
        _overlay = null;
    }

    protected var _ctx :MsoyContext;
    protected var _overlay :ChatOverlay;
    protected var _chatDtr :ChatDirector;
    protected var _playerList :UIComponent;
    protected var _listContainer :HBox;
    protected var _channelOccList :UIComponent;
    protected var _tabBar :HBox;
}
}
