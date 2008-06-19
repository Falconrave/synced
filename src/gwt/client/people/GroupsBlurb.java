//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GroupCard;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;
import client.util.ThumbBox;

/**
 * Displays a list of the groups of which a person is a member.
 */
public class GroupsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.groups != null);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(CPeople.msgs.groupsTitle());
        setContent(new GroupsGrid(pdata.groups));
    }

    protected static Widget createEmptyTable (String message, String link, String page, String args)
    {
        FlowPanel bits = new FlowPanel();
        bits.add(new InlineLabel(message, false, false, true));
        bits.add(Application.createLink(link, page, args));
        return bits;
    }

    protected class GroupsGrid extends PagedGrid
    {
        public GroupsGrid (List groups) {
            super(GROUP_ROWS, GROUP_COLUMNS, PagedGrid.NAV_ON_BOTTOM);
            setModel(new SimpleDataModel(groups), 0);
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return CPeople.msgs.notInGroupsOther();
        }

        @Override // from PagedGrid
        protected Widget createEmptyContents ()
        {
            if (CPeople.getMemberId() != _name.getMemberId()) {
                return super.createEmptyContents();
            }
            return createEmptyTable(CPeople.msgs.notInGroupsSelf(),
                                    CPeople.msgs.notInGroupsJoin(), Page.WHIRLEDS, "");
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return (items > _rows * _cols);
        }

        @Override // from PagedGrid
        protected Widget createWidget (Object item)
        {
            return new GroupWidget((GroupCard)item);
        }

//         @Override // from PagedGrid
//         protected void addCustomControls (FlexTable controls)
//         {
//             if (CPeople.getMemberId() > 0 && CPeople.getMemberId() != _name.getMemberId()) {
//                 Button inviteButton = new Button(CPeople.msgs.inviteToGroup());
//                 new ClickCallback(inviteButton) {
//                     public boolean callService () {
//                         CPeople.groupsvc.getMembershipGroups(
//                             CPeople.ident, CPeople.getMemberId(), true, this);
//                         return true;
//                     }
//                     public boolean gotResult (Object result) {
//                         List inviteGroups = (List) result;
//                         if (inviteGroups.size() == 0) {
//                             MsoyUI.infoNear(CPeople.msgs.haveNoGroups(), _trigger);
//                         } else {
//                             new MailComposition(_name, "Join this group!",
//                                                 new GroupInvite.Composer(inviteGroups),
//                                                 "Check out this scrumptious group.").show();
//                         }
//                         return true;
//                     }
//                 };
//                 controls.setWidget(0, 0, inviteButton);
//             }
//         }
    }

    protected class GroupWidget extends FlowPanel
    {
        public GroupWidget (final GroupCard card) {
            setStyleName("Group");

            ClickListener groupClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WHIRLEDS, Args.compose("d", card.name.getGroupId()));
                }
            };
            add(new ThumbBox(card.logo, groupClick));
            add(MsoyUI.createActionLabel(card.name.toString(), groupClick));
        }
    }

    protected static final int GROUP_COLUMNS = 6;
    protected static final int GROUP_ROWS = 2;
}
