//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.profile.gwt.ProfileService;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays a person's friends list.
 */
public class FriendsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.friends != null);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(_msgs.friendsTitle());

        if (pdata.friends.size() == 0) {
            if (CShell.getMemberId() != _name.getId()) {
                setContent(new Label(_msgs.noFriendsOther()));
            } else {
                setContent(GroupsBlurb.createEmptyTable(
                               _msgs.noFriendsSelf(), _msgs.noFriendsFindEm(),
                               Pages.PEOPLE, "search"));
            }
        } else {
            SmartTable grid = new SmartTable();
            for (int ii = 0; ii < pdata.friends.size(); ii++) {
                grid.setWidget(0, ii, new FriendWidget(pdata.friends.get(ii)));
            }
            setContent(grid);
        }

        setFooterLink(_msgs.seeAllFriends("" + pdata.totalFriendCount),
                      Pages.PEOPLE, "f", pdata.name.getId());
    }

    protected class FriendWidget extends FlowPanel
    {
        public FriendWidget (final MemberCard card)
        {
            setStyleName("Friend");
            add(new ThumbBox(card.photo, Pages.PEOPLE, ""+card.name.getId()));
            add(Link.create(card.name.toString(), Pages.PEOPLE, ""+card.name.getId()));
        }
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
