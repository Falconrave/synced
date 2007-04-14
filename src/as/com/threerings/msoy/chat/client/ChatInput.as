//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.FocusEvent;

import mx.controls.TextInput;

/**
 * A special TextInput for entering Chat. One of these is used in ChatControl.
 *
 * A standard TextInput has the stupid behavior of selecting all the text when it receives
 * focus. Disable that so that we can receive focus and people can type and we don't blow away
 * whatever they had before.
 */
public class ChatInput extends TextInput
{
    public function ChatInput ()
    {
        styleName = "chatInput";
    }

    override protected function focusInHandler (event :FocusEvent) :void
    {
        var oldValue :Boolean = textField.selectable;
        textField.selectable = false;
        try {
            super.focusInHandler(event);

        } finally {
            textField.selectable = oldValue;
        }
    }
}
}
