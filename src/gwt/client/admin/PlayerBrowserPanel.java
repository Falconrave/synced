//
// $Id$

package client.admin;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MemberInviteStatus;
import com.threerings.msoy.web.data.MemberInviteResult;

import client.shell.Application;

/**
 * Displays the various services available to support and admin personnel.
 */
public class PlayerBrowserPanel extends HorizontalPanel
{
    public PlayerBrowserPanel ()
    {
        setStyleName("playerBrowser");
        setSpacing(10);

        add(_backButton = new Button("<--", new ClickListener() {
            public void onClick (Widget sender) {
                if (_childList == null) {
                    // nothing to do
                    return;
                }
                if (_parentList == null) {
                    // we're keeping the same history token, just need to grab parent info.
                    displayPlayersInvitedBy(_childList.getResult().memberId);
                } else {
                    History.newItem(Application.createLinkToken("admin",
                        "browser_" + _parentList.getResult().memberId));
                }
            }
        }));
        _backButton.setEnabled(false);
        add(_forwardButton = new Button("-->", new ClickListener() {
            public void onClick (Widget sender) {
                if (_childList == null) {
                    // nothing to do
                    return;
                }
                int index = _playerLists.indexOf(_childList);
                if (index == _playerLists.size() - 1) {
                    // nothing to do
                    return;
                }
                History.newItem(Application.createLinkToken("admin", "browser_" + 
                    ((PlayerList) _playerLists.get(index + 1)).getResult().memberId));
            }
        }));
        _forwardButton.setEnabled(false);
    }

    public void displayPlayersInvitedBy (final int memberId) 
    {
        int childList = -1;
        int parentList = -1;
        for (int ii = 0; _playerLists != null && ii < _playerLists.size(); ii++) {
            PlayerList list = (PlayerList) _playerLists.get(ii);
            if (list.highlight(memberId)) {
                parentList = ii;
            } else if (list.getResult().memberId == memberId) {
                childList = ii;
                break;
            }
        }
        
        int memberIdToFetch = memberId;
        if (childList > 0 || 
                // special case only invoked if we have a populated list, and the person goes back
                // to the root admin console and clicks "player browser" again.
                (childList == 0 && 
                 ((PlayerList) _playerLists.get(childList)).getResult().memberId == 0)) {
            // we already have everything the caller wants, just display it
            displayLists(childList);
            return;
        } else if (childList == 0) {
            // we need to fetch the parent list
            PlayerList list = _parentList != null ? _parentList : _childList;
            memberIdToFetch = list.getResult().invitingFriendId;
        } else if (parentList > -1) {
            // we have the parent, but the child is not there, so we're on a new branch... truncate
            // the list at the parent
            while (_playerLists.size() > parentList + 1) {
                _playerLists.remove(parentList + 1);
            }
        } else {
            clearLists();
            _playerLists = new ArrayList();
        }

        CAdmin.adminsvc.getPlayerList(CAdmin.ident, memberIdToFetch, new AsyncCallback() {
            public void onSuccess (Object result) {
                MemberInviteResult res = (MemberInviteResult) result;
                PlayerList newList = new PlayerList(res);
                if (res.memberId != memberId) {
                    // we're fetching a new parent.
                    _playerLists.add(0, newList);
                    PlayerList list = _parentList != null ? _parentList : _childList;
                    newList.highlight(list.getResult().memberId);
                    displayLists(1);
                } else {
                    _playerLists.add(newList);
                    displayLists(_playerLists.size() - 1);
                }
            }
            public void onFailure (Throwable cause) {
                add(new Label(CAdmin.serverError(cause)));
            }
        });
    }

    protected void clearLists ()
    {
        if (_parentList != null) {
            remove(_parentList);
            _parentList = null;
        }
        if (_childList != null) {
            remove(_childList);
            _childList = null;
        }
    }

    /**
     * Displays two lists, with the firstList being shown first, if not null.  Otherwise, it
     * simply displays the last two PlayerLists we have.
     */
    protected void displayLists (int childIndex)
    {
        clearLists();
        insert(_childList = (PlayerList) _playerLists.get(childIndex), 1);
        _forwardButton.setEnabled(childIndex != _playerLists.size() - 1);
        if (childIndex != 0) {
            insert(_parentList = (PlayerList) _playerLists.get(childIndex - 1), 1);
            _backButton.setEnabled(_parentList.getResult().memberId != 0);
        } else {
            _backButton.setEnabled(_childList.getResult().memberId != 0);
        }
    }

    protected class PlayerList extends FlexTable
    {
        public PlayerList (MemberInviteResult result) 
        {
            _result = result;
            String title = _result.name != null && !_result.name.equals("") ? 
                CAdmin.msgs.browserInvitedBy(_result.name) : CAdmin.msgs.browserNoInviter();

            setStyleName("PlayerList");
            int row = 0;
            getFlexCellFormatter().setColSpan(row, 0, NUM_COLUMNS);
            getFlexCellFormatter().addStyleName(row, 0, "Title");
            setText(row++, 0, title);

            getFlexCellFormatter().setColSpan(row, 1, 3);
            getFlexCellFormatter().addStyleName(row, 1, "Last");
            getFlexCellFormatter().addStyleName(row, 1, "Header");
            setText(row++, 1, CAdmin.msgs.browserInvites());

            getRowFormatter().addStyleName(row, "Clickable");
            getRowFormatter().addStyleName(row, "Separator");
            // organized in the same order as the NNN_COLUMN constants
            String[] labelText = new String[] { CAdmin.msgs.browserName(), 
                CAdmin.msgs.browserAvailable(), CAdmin.msgs.browserUsed(), 
                CAdmin.msgs.browserTotal() };
            int[] sortType = new int[] { RowComparator.SORT_TYPE_STRING, 
                RowComparator.SORT_TYPE_INT, RowComparator.SORT_TYPE_INT, 
                RowComparator.SORT_TYPE_INT };
            int[] sortOrder = new int[] { RowComparator.SORT_ORDER_ASCENDING,
                RowComparator.SORT_ORDER_DESCENDING, RowComparator.SORT_ORDER_DESCENDING,
                RowComparator.SORT_ORDER_DESCENDING };
            for (int ii = 0; ii < NUM_COLUMNS; ii++) {
                Label headerLabel = new Label(labelText[ii]);
                headerLabel.addStyleName("Header");
                final int column = ii;
                final int type = sortType[ii];
                final int order = sortOrder[ii];
                headerLabel.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        sort(column, type, _sortOrder);
                        _sortOrder *= -1;
                        if (_activeHeader != null) {
                            _activeHeader.removeStyleName("HighlightedHeader");
                        }
                        (_activeHeader = (Label) sender).addStyleName("HighlightedHeader");
                    }
                    protected int _sortOrder = order;
                });
                setWidget(row, ii, headerLabel);
            }
            getFlexCellFormatter().addStyleName(row++, NUM_COLUMNS-1, "Last");

            if (_result.invitees == null) {
                return;
            }
            _rows = new Object[_result.invitees.size()];
            int ii = 0;
            Iterator iter = _result.invitees.iterator();
            while (iter.hasNext()) {
                final MemberInviteStatus member = (MemberInviteStatus) iter.next();
                getRowFormatter().addStyleName(row, "DataRow");
                Label nameLabel = new Label(member.name);
                nameLabel.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        History.newItem(Application.createLinkToken("admin", 
                            "browser_" + member.memberId));
                    }
                });
                nameLabel.addStyleName("Clickable");
                _memberIds.put(new Integer(member.memberId), nameLabel);
                setWidget(row, NAME_COLUMN, nameLabel);
                setText(row, AVAILABLE_INVITES_COLUMN, "" + member.invitesGranted);
                setText(row, USED_INVITES_COLUMN, "" + member.invitesSent);
                setText(row++, TOTAL_INVITES_COLUMN, 
                    "" + (member.invitesGranted + member.invitesSent));
                getFlexCellFormatter().addStyleName(row-1, NUM_COLUMNS-1, "Last");
                _rows[ii++] = getRowFormatter().getElement(row-1);
            }
            getRowFormatter().addStyleName(row-1, "Bottom");
        }

        public boolean highlight (int memberId) 
        {
            Label label = (Label) _memberIds.get(new Integer(memberId));
            if (label == null) {
                return false;
            }

            clearHighlight();
            (_activeLabel = label).addStyleName("Highlighted");
            return true;
        }

        public void clearHighlight ()
        {
            if (_activeLabel != null) {
                _activeLabel.removeStyleName("Highlighted");
                _activeLabel = null;
            }
        }

        public MemberInviteResult getResult () 
        {
            return _result;
        }

        public void sort (int column, int type, int order)
        {
            int rowCount = getRowCount();
            getRowFormatter().removeStyleName(rowCount-1, "Bottom");
            Element table = getBodyElement();
            for (int ii = 0; ii < _rows.length; ii++) {
                DOM.removeChild(table, (Element) _rows[ii]);
            }
            Arrays.sort(_rows, new RowComparator(column, type, order));
            for (int ii = 0; ii < _rows.length; ii++) {
                DOM.appendChild(table, (Element) _rows[ii]);
            }
            getRowFormatter().addStyleName(rowCount-1, "Bottom");
        }

        protected class RowComparator implements Comparator
        {
            public static final int SORT_TYPE_STRING = 1;
            public static final int SORT_TYPE_INT = 2;

            public static final int SORT_ORDER_DESCENDING = -1;
            public static final int SORT_ORDER_ASCENDING = 1;

            public RowComparator (int column, int sortType, int sortOrder) 
            {
                _column = column;
                _sortType = sortType;
                _sortOrder = sortOrder;
            }

            public boolean equals (Object obj) {
                if (!(obj instanceof RowComparator)) {
                    return false;
                }
                RowComparator other = (RowComparator) obj;
                return other._column == _column && other._sortType == _sortType &&
                    other._sortOrder == _sortOrder;
            }

            public int compare (Object o1, Object o2) 
            {
                if (!(o1 instanceof Element) || !(o2 instanceof Element)) {
                    CAdmin.log("Received non-Element when sorting player list! " +
                        "|" + o1 + "|" + o2 + "|");
                    return 0; 
                }
                String s1 = getCellContents((Element) o1);
                String s2 = getCellContents((Element) o2);

                int result = 0;
                if (_sortType == SORT_TYPE_INT) {
                    try {
                        result = new Integer(s1).compareTo(new Integer(s2));
                    } catch (NumberFormatException nfe) {
                        CAdmin.log("NFE when sorting player list: " + nfe.getMessage());
                    }
                } else {
                    result = s1.compareTo(s2);
                }
                return result * _sortOrder;
            }

            protected String getCellContents (Element row) 
            {
                if (DOM.getChildCount(row) < _column) {
                    CAdmin.log("Element row does not contain " + _column + " children.");
                    return "";
                }
                return DOM.getInnerText(DOM.getChild(row, _column));
            }

            protected int _column;
            protected int _sortType;
            protected int _sortOrder;
        }

        protected static final int NUM_COLUMNS = 4;
        protected static final int NAME_COLUMN = 0;
        protected static final int AVAILABLE_INVITES_COLUMN = 1;
        protected static final int USED_INVITES_COLUMN = 2;
        protected static final int TOTAL_INVITES_COLUMN = 3;

        // Something super weird is going on here (possibly a bug with the GWT compiler).  
        // This array holds only Elements, but if it is declared as Element[], then the 
        // comparator's compare() method fails when checking if the objects it receives are
        // instanceof Element.  If the array is declared as Object[], and every time an 
        // element is accessed it is casted to Element, everything works fine.
        protected Object[] _rows;
        protected MemberInviteResult _result;
        protected Label _activeLabel, _activeHeader;
        protected Map _memberIds = new HashMap(); // Map<Integer, Label>
    }

    // ArrayList<PlayerList>
    protected ArrayList _playerLists;
    protected PlayerList _parentList, _childList;
    protected Button _backButton, _forwardButton;
}
