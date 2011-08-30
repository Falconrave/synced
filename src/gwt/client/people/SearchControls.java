//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.NaviUtil;
import client.util.TextBoxUtil;

/**
 * Displays controls that can be used to search for people in Whirled.
 */
public class SearchControls extends SmartTable
    implements ClickHandler
{
    public SearchControls ()
    {
        super("searchControls", 0, 5);

        PushButton action;
        if (CShell.isGuest()) {
            action = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.searchJoin(),
                                         NaviUtil.onMustRegister());
        } else {
            action = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.searchInvite(),
                                         Link.createHandler(Pages.PEOPLE, "invites"));
        }
        setWidget(0, 0, action, 1, "Action");
        getFlexCellFormatter().setRowSpan(0, 0, 3);

        setWidget(0, 1, MsoyUI.createLabel(_msgs.searchTip(), "Tip"), 2);

        setWidget(1, 0, _search = MsoyUI.createTextBox("", -1, -1), 1);
        _search.setWidth("200px");
        TextBoxUtil.addTypingListener(_search, new Command() {
            public void execute () {
                _go.setEnabled(getQuery().length() != 0);
            }
        });
        EnterClickAdapter.bind(_search, this);
        setWidget(1, 1, _go = new Button(_msgs.searchGo(), this));
        _go.setEnabled(false);

        setWidget(1, 2, WidgetUtil.makeShim(15, 15));

        Widget friendlyLink = Link.create(_msgs.searchGreetersLink(), Pages.PEOPLE, "friendly");
        friendlyLink.setStyleName("SeeGreeters");
        setWidget(2, 0, friendlyLink, 3);
    }

    public void setSearch (String query)
    {
        _search.setText(query);
    }

    // from interface ClickHandler
    public void onClick (ClickEvent event)
    {
        Link.go(Pages.PEOPLE, "search", "0", getQuery());
    }

    @Override // from Widget
    protected void onAttach ()
    {
        super.onAttach();
        _search.setFocus(true);
    }

    protected String getQuery ()
    {
        return _search.getText().trim();
    }

    protected TextBox _search;
    protected Button _go;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
