//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.person.data.MailPayload;

import client.util.ClickCallback;
import client.util.MsoyUI;

/**
 * A callback that handles starting a conversation.
 */
public abstract class StartConvoCallback extends ClickCallback
{
    public StartConvoCallback (SourcesClickEvents trigger, int recipientId,
                               TextBox subject, TextArea body)
    {
        super(trigger);
        _recipientId = recipientId;
        _subject = subject;
        _body = body;
    }

    // @Override // from ClickCallback
    public boolean callService ()
    {
        String subject = _subject.getText().trim();
        String body = _body.getText().trim();
        if (subject.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.sccMissingSubject());
            return false;
        }
        if (body.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.sccMissingBody());
            return false;
        }
        CMsgs.mailsvc.startConversation(
            CMsgs.ident, _recipientId, subject, body, getPayload(), this);
        return true;
    }

    protected MailPayload getPayload ()
    {
        return null;
    }

    protected int _recipientId;
    protected TextBox _subject;
    protected TextArea _body;
}
