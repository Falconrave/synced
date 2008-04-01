//
// $Id$

package client.mail;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Conversation;
import com.threerings.msoy.person.data.Profile;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.ThumbBox;

/**
 * Displays the main interface for mail.
 */
public class MailPanel extends VerticalPanel
{
    public MailPanel (ConvosModel model, int page)
    {
        setStyleName("mail");

//         SmartTable header = new SmartTable("MailHeader", 0, 10);
//         header.setText(0, 0, "BIGIMAGE");
//         header.setText(0, 1, "Inbox + Sent");
//         header.setText(0, 2, "Compose");
//         header.setText(0, 3, "Check Mail");
//         header.setText(0, 4, "Search Mail");
//         add(header);
        add(WidgetUtil.makeShim(10, 10));

        setHorizontalAlignment(ALIGN_CENTER);
        ConvosGrid grid = new ConvosGrid();
        add(grid);
        grid.setModel(model, page);
    }

    protected static class ConvosGrid extends PagedGrid
    {
        public ConvosGrid ()
        {
            super(10, 1, NAV_ON_TOP);
            addStyleName("Folder");
        }

        // @Override // from PagedGrid
        protected Widget createWidget (Object item)
        {
            return new ConvoWidget(this, (Conversation)item);
        }

        // @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return CMail.msgs.mailNoMail();
        }

        // @Override // from PagedGrid
        protected void formatCell (HTMLTable.CellFormatter formatter, int row, int col, Object item)
        {
            super.formatCell(formatter, row, col, item);
            Conversation convo = (Conversation)item;
            if (convo.hasUnread) {
                formatter.addStyleName(row, col, "Unread");
            }
        }

        // @Override // from PagedGrid
        protected void displayResults (int start, int count, List list)
        {
            super.displayResults(start, count, list);

            SmartTable footer = new SmartTable("Footer", 0, 0);
            footer.setWidth("100%");
            footer.setHTML(0, 0, "&nbsp;", 1, "BottomLeft");
            footer.setHTML(0, 1, "&nbsp;");
            footer.setHTML(0, 2, "&nbsp;", 1, "BottomRight");
            add(footer);
        }

        // @Override // from PagedGrid
        protected void displayPageFromClick (int page)
        {
            Application.go(Page.MAIL, ""+page);
        }

        // @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return true;
        }

        // @Override // from PagedGrid
        protected void addCustomControls (FlexTable controls)
        {
            super.addCustomControls(controls);

            controls.setWidget(0, 0, new Button(CMail.msgs.mailCheck(), new ClickListener() {
                public void onClick (Widget sender) {
                    if (_page == 0) {
                        ((ConvosModel)_model).reset();
                        displayPage(0, true);
                    } else {
                        Application.go(Page.MAIL, "");
                    }
                }
            }));
        }
    }

    protected static class ConvoWidget extends SmartTable
    {
        public ConvoWidget (final ConvosGrid grid, final Conversation convo)
        {
            super("Convo", 0, 0);

            Image delete = new Image("/images/profile/remove.png");
            if (convo.hasUnread) {
                delete.setTitle(CMail.msgs.mailNoDeleteTip());
            } else {
                delete.setTitle(CMail.msgs.mailDeleteTip());
                delete.addStyleName("actionLabel");
                new ClickCallback(delete, CMail.msgs.deleteConfirm()) {
                    public boolean callService () {
                        CMail.mailsvc.deleteConversation(CMail.ident, convo.conversationId, this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        boolean deleted = ((Boolean)result).booleanValue();
                        if (!deleted) {
                            MsoyUI.info(CMail.msgs.deleteNotDeleted());
                        } else {
                            grid.removeItem(convo);
                            MsoyUI.info(CMail.msgs.deleteDeleted());
                        }
                        return !deleted;
                    }
                };
            }
            setWidget(0, 0, delete, 1, "Delete");
            getFlexCellFormatter().setRowSpan(0, 0, 2);

            MediaDesc photo;
            Widget name;
            if (convo.other == null) {
                photo = Profile.DEFAULT_PHOTO;
                name = MsoyUI.createLabel("<unknown>", null);
            } else {
                photo = convo.other.photo;
                name = Application.memberViewLink(convo.other.name);
            }

            setWidget(0, 1, new ThumbBox(photo, MediaDesc.HALF_THUMBNAIL_SIZE, null), 1, "Photo");
            getFlexCellFormatter().setRowSpan(0, 1, 2);

            setWidget(0, 2, name, 1, "Name");
            setText(1, 0, _fmt.format(convo.lastSent), 1, "Sent");

            Widget link = Application.createLink(
                (convo.subject.length() == 0) ? CMail.msgs.mailNoSubject() : convo.subject,
                Page.MAIL, Args.compose("c", convo.conversationId));
            setWidget(0, 3, link, 1, "Subject");
            setText(1, 1, convo.lastSnippet, 1, "Snippet");
        }
    }

    protected static SimpleDateFormat _fmt = new SimpleDateFormat("h:mm a MMM dd");
}
