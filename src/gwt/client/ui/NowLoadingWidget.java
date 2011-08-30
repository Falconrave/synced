//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import client.shell.ShellMessages;

/**
 * Simple widget for displaying a "Now loading" indicator on a page.  It has two states: "loading",
 * which is the initial state, and "finishing", which can be used to indicate that loading is
 * complete, but rendering must still be done.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class NowLoadingWidget extends PopupPanel
{
    public NowLoadingWidget ()
    {
        super(false, false);

        _panel = new VerticalPanel();
        _panel.setStyleName("nowLoadingWidget");
        _panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);

        Image icon = new Image("/images/ui/loading_globe_notext.gif");
        _panel.add(icon);
        _label = new Label();
        _label.setStyleName("label");
        _label.setWordWrap(false);
        _panel.add(_label);
        setWidget(_panel);

        // Begin in the loading state.
        loading();
    }

    public void loading ()
    {
        _label.setText(_smsgs.nowLoading());
    }

    public void finishing (Timer finishTask)
    {
        _label.setText(_smsgs.loadFinishing());
        finishTask.schedule(1);
    }

    protected static final ShellMessages _smsgs = GWT.create(ShellMessages.class);

    protected final VerticalPanel _panel;
    protected final Label _label;
}
