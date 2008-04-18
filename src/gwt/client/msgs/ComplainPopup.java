//
// $Id$

package client.msgs;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import client.util.BorderedDialog;
import client.util.MsoyUI;

/**
 * Displays a popup for complaining about a message.
 */
public abstract class ComplainPopup extends BorderedDialog
    implements AsyncCallback
{
    public ComplainPopup ()
    {
        setHeaderTitle(CMsgs.mmsgs.complainHeader());
        VerticalPanel vbox = new VerticalPanel();
        vbox.setStyleName("complainPopup");
        vbox.setSpacing(5);
        vbox.add(MsoyUI.createLabel(CMsgs.mmsgs.complainMessage(), null));
        vbox.add(MsoyUI.createLabel(CMsgs.mmsgs.complainDesc(), null));
        vbox.add(_description = MsoyUI.createTextBox("", 512, 50));

        ClickListener sendComplain = new ClickListener() {
            public void onClick (Widget sender) {
                sendComplain();
            }
        };
        _description.addKeyboardListener(new EnterClickAdapter(sendComplain));

        setContents(vbox);

        Button submit = new Button(CMsgs.cmsgs.send(), sendComplain);
        addButton(submit);

        addButton(new Button(CMsgs.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));
    }

    /**
     * Returns true if you want to hide the popup when the service call is made and ignore
     * any return values.
     */
    public boolean hideOnSend ()
    {
        return true;
    }

    // from interface AsyncCallback
    public void onSuccess (Object result)
    {
        if (!hideOnSend()) {
            hide();
        }
    }

    // from interface AsyncCallback
    public void onFailure (Throwable cause)
    {
        // nothing by default
    }

    protected abstract boolean callService();

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        _description.setFocus(true);
    }

    protected void sendComplain ()
    {
        if ("".equals(_description.getText())) {
            MsoyUI.info(CMsgs.mmsgs.complainNeedDescription());
            return;
        }
        if (callService() && hideOnSend()) {
            hide();
        }
    }

    protected TextBox _description;
}
