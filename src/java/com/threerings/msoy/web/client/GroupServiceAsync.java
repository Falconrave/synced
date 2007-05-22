//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface GroupServiceAsync
{
    /**
     * The asynchronous version of {@link GroupService#getGroupsList}
     */
    public void getGroupsList (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService#searchGroups}
     */
    public void searchGroups (WebIdent ident, String searchString, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService#searchForTag}
     */
    public void searchForTag (WebIdent ident, String tag, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupHomeId}
     */
    public void getGroupHomeId (WebIdent ident, int groupId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService#getMembershipGroups}
     */
    public void getMembershipGroups (WebIdent ident, int memberId, boolean canInvite,
                                     AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.loadGroup} 
     */
    public void getGroupDetail (WebIdent ident, int groupId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.updateGroup} 
     */
    public void updateGroup (WebIdent ident, Group group, GroupExtras extras, 
                             AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.createGroup} 
     */
    public void createGroup (WebIdent ident, Group group, GroupExtras extras, 
                             AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.leaveGroup} 
     */
    public void leaveGroup (WebIdent ident, int groupId, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.joinGroup} 
     */
    public void joinGroup (WebIdent ident, int groupId, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.updateMemberRank}
     */
    public void updateMemberRank (WebIdent ident, int groupId, int memberId, byte newRank,
                                  AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.tagGroup}
     */
    public void tagGroup (WebIdent ident, int groupId, String tag, boolean set,
                          AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.getRecentTags}
     */
    public void getRecentTags (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.getTags}
     */
    public void getTags (WebIdent ident, int groupId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link GroupService.getPopularTags}
     */
    public void getPopularTags (WebIdent ident, int rows, AsyncCallback callback);
}
