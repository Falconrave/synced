//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;

import com.threerings.gwt.ui.WidgetUtil;

import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays an interface for creating a new thread.
 */
public class NewThreadPanel extends TableFooterPanel
{
    public NewThreadPanel (int groupId, boolean isManager)
    {
        _groupId = groupId;

        addRow(CMsgs.mmsgs.ntpSubject(), _subject = new TextBox());
        _subject.setMaxLength(ForumThread.MAX_SUBJECT_LENGTH);
        _subject.setVisibleLength(40);

        if (isManager) {
            RowPanel bits = new RowPanel();
            bits.add(_announce = new CheckBox());
            bits.add(MsoyUI.createLabel(CMsgs.mmsgs.ntpAnnounceTip(), "Tip"));
            addRow(CMsgs.mmsgs.ntpAnnounce(), bits);

            bits = new RowPanel();
            bits.add(_sticky = new CheckBox());
            bits.add(MsoyUI.createLabel(CMsgs.mmsgs.ntpStickyTip(), "Tip"));
            addRow(CMsgs.mmsgs.ntpSticky(), bits);
        }

        addRow(WidgetUtil.makeShim(5, 5));
        addRow(CMsgs.mmsgs.ntpFirstMessage());
        addRow(_message = new MessageEditor());

        addFooterButton(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                ((ForumPanel)getParent()).newThreadCanceled(_groupId);
            }
        }));
        Button submit = new Button(CMsgs.cmsgs.submit());
        new ForumCallback(submit) {
            public boolean callService () {
                return submitNewThread(this);
            }
            public boolean gotResult (Object result) {
                ((ForumPanel)getParent()).newThreadPosted((ForumThread)result);
                return false;
            }
        };
        addFooterButton(submit);
    }

    protected boolean submitNewThread (ForumCallback callback)
    {
        String subject = _subject.getText().trim();
        if (subject.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.errNoSubject());
            return false;
        }

        String message = _message.getHTML();
        if (message.length() == 0) {
            MsoyUI.error(CMsgs.mmsgs.errNoMessage());
            return false;
        }

        int flags = 0;
        if (_announce != null && _announce.isChecked()) {
            flags |= ForumThread.FLAG_ANNOUNCEMENT;
        }
        if (_sticky != null && _sticky.isChecked()) {
            flags |= ForumThread.FLAG_STICKY;
        }
        CMsgs.forumsvc.createThread(CMsgs.ident, _groupId, flags, subject, message, callback);
        return true;
    }

    protected int _groupId;
    protected TextBox _subject;
    protected CheckBox _announce, _sticky;
    protected MessageEditor _message;
}
