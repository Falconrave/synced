package client.msgs;

import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.fora.data.ForumMessage;

import client.shell.Application;
import client.shell.Page;

/**
 * A message panel that display a forum message with a link back to the thread.
 */
public class IssueMessagePanel extends SimpleMessagePanel
{
    public IssueMessagePanel ()
    {
    }

    public IssueMessagePanel (ForumMessage message)
    {
        setMessage(message);
    }

    @Override // from SimpleMessagePanel
    public void setMessage (ForumMessage message)
    {
        _threadId = message.threadId;
        super.setMessage(message);
    }

    @Override // from MessagePanel
    public void addInfo (FlowPanel info)
    {
        super.addInfo(info);

        Hyperlink link = Application.createLink(
            CMsgs.mmsgs.iThread(), Page.WHIRLEDS, "t_" + _threadId);
        link.setStyleName("issueMessageLink");
        link.addStyleName("actionLabel");
        info.add(link);
    }

    protected int _threadId;
}
