//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MemberStatusLabel;
import client.ui.MiniNowLoadingWidget;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.InfoCallback;
import client.util.PagedServiceDataModel;
import client.util.ServiceUtil;

/**
 * Displays the members of a particular Group. Allows managers to manage ranks and membership.
 */
public class GroupMembersPanel extends PagedGrid<GroupMemberCard>
{
   public GroupMembersPanel (GroupDetail detail)
    {
        super(5, 2, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");

        _detail = detail;
        String args = Args.compose("w", "g", ""+_detail.group.groupId);
        _invite.addClickListener(Link.createListener(Pages.MAIL, args));
        _invite.setEnabled(Group.canInvite(detail.group.policy, detail.myRank));

        setModel(new PagedServiceDataModel<GroupMemberCard, PagedResult<GroupMemberCard>>(){
            @Override protected void callFetchService (
                int start, int count, boolean needCount,
                AsyncCallback<PagedResult<GroupMemberCard>> callback) {
                _groupsvc.getGroupMembers(_detail.group.groupId, start, count, callback);
            }
            
        }, 0);
    }

    @Override // from PagedGrid
    protected Widget createWidget (GroupMemberCard card)
    {
        return new MemberWidget(card);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return "This group has no members.";
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        controls.setWidget(0, 0, _invite = new Button(_msgs.wmpInvite()));
    }

    @Override // from PagedWidget
    protected Widget getNowLoadingWidget ()
    {
        return new MiniNowLoadingWidget();
    }

    public boolean amSenior (GroupMemberCard member)
    {
        return (_detail.myRank > member.rank) ||
            (_detail.myRank == member.rank && _detail.myRankAssigned < member.rankAssigned);
    }

    protected Command updateMemberRank (final GroupMemberCard card, final byte rank)
    {
        return new Command() {
            public void execute () {
                _groupsvc.updateMemberRank(_detail.group.groupId, card.name.getMemberId(), rank,
                    new InfoCallback<Void>() {
                        public void onSuccess (Void result) {
                            card.rank = rank;
                            card.rankAssigned = System.currentTimeMillis();
                            // force a page refresh so we see the new rank...
                            displayPage(_page, true);
                        }
                    });
            }
        };
    }

    protected Command removeMember (final GroupMemberCard card)
    {
        return new Command() {
            public void execute () {
                _groupsvc.leaveGroup(
                    _detail.group.groupId, card.name.getMemberId(), new InfoCallback<Void>() {
                        public void onSuccess (Void result) {
                            removeItem(card);
                        }
                    });
            }
        };
    }

    protected class MemberWidget extends SmartTable
    {
        public MemberWidget (GroupMemberCard card)
        {
            super("Member", 0, 2);
            MemberName name = card.name;

            int mid = name.getMemberId();
            setWidget(0, 0, new ThumbBox(card.photo, Pages.PEOPLE, ""+mid), 1, "Photo");
            getFlexCellFormatter().setRowSpan(0, 0, 3);
            setWidget(0, 1, Link.memberView(""+name, mid), 1, "Name");
            String rankStr = card.rank == GroupMembership.RANK_MANAGER ? "Manager" : "";
            setText(1, 0, rankStr, 1, "tipLabel");
            setWidget(2, 0, new MemberStatusLabel(card), 2, null);

            // if we're not a manager above this member in rank, or we're not support+ don't add
            // the edit controls
            if (!CShell.isSupport() &&
                (_detail.myRank != GroupMembership.RANK_MANAGER || !amSenior(card))) {
                return;
            }

            if (card.rank == GroupMembership.RANK_MEMBER) {
                setAction(0, 2, _msgs.detailPromote(),
                          _msgs.detailPromotePrompt(""+card.name),
                          updateMemberRank(card, GroupMembership.RANK_MANAGER));
            } else {
                setAction(0, 2, _msgs.detailDemote(),
                          _msgs.detailDemotePrompt(""+card.name),
                          updateMemberRank(card, GroupMembership.RANK_MEMBER));
            }

            setAction(1, 1, _msgs.detailRemove(), _msgs.detailRemovePrompt(
                          ""+card.name, _detail.group.name), removeMember(card));
        }

        protected void setAction (int row, int col, String label, String confirm, Command command)
        {
            setWidget(row, col, MsoyUI.createActionLabel(
                          label, new PromptPopup(confirm, command)), 1, "Edit");
        }
    }

    protected GroupDetail _detail;
    protected Button _invite;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
