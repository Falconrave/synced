//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.ObserverList;

import com.threerings.crowd.chat.client.ChatDisplay;
import com.threerings.crowd.chat.data.ChatMessage;

public class HistoryList
    implements ChatDisplay
{
    /**
     * @return the current size of the history.
     */
    public function size () :int
    {
        return _history.length;
    }

    /**
     * Get the history entry at the specified index.
     */
    public function get (index :int) :ChatMessage
    {
        return (_history[index] as ChatMessage);
    }

    /**
     * Add a chat overlay to the list of those interested in hearing
     * about history changes.
     */
    public function addChatOverlay (overlay :ChatOverlay) :void
    {
        _obs.add(overlay);
    }

    /**
     * Add a chat overlay to the list of those interested in hearing
     * about history changes.
     */
    public function removeChatOverlay (overlay :ChatOverlay) :void
    {
        _obs.remove(overlay);
    }

    // from ChatDisplay
    public function clear () :void
    {
        var adjusted :int = _history.length;
        _history.length = 0; // array truncation
        notify(adjusted);
    }

    // from ChatDisplay
    public function displayMessage (
        msg :ChatMessage, alreadyDisp :Boolean) :Boolean
    {
        var adjusted :int;
        if (_history.length == MAX_HISTORY) {
            _history.splice(0, PRUNE_HISTORY);
            adjusted = PRUNE_HISTORY;

        } else {
            adjusted = 0;
        }

        _history.push(msg);
        notify(adjusted);
        return true;
    }

    /**
     * Notifies interested ChatOverlays that there has been a change
     * to the history.
     */
    protected function notify (adjustment :int) :void
    {
        _obs.apply(function (overlay :ChatOverlay) :void {
            overlay.historyUpdated(adjustment);
        });
    }

    /** The array in which we store historical chat. */
    protected var _history :Array = new Array();

    /** A list of overlays interested in history. */
    protected var _obs :ObserverList = new ObserverList();

    /** The maximum number of history entries we'll keep. */
    protected static const MAX_HISTORY :int = 1000;

    /** The number of history entries we'll prune when we hit the max. */
    protected static const PRUNE_HISTORY :int = 100;
}
}
