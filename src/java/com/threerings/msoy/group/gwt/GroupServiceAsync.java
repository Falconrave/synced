//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PagedResult;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.web.gwt.TagHistory;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

/**
 * The asynchronous (client-side) version of {@link GroupService}.
 */
public interface GroupServiceAsync
{
    /**
     * The asynchronous version of {@link GroupService#getGalaxyData}
     */
    void getGalaxyData (AsyncCallback<GalaxyData> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroups}
     */
    void getGroups (int offset, int count, GroupService.GroupQuery query, boolean needCount,
        AsyncCallback<PagedResult<GroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupInfo}
     */
    void getGroupInfo (int groupId, AsyncCallback<GroupService.GroupInfo> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupHomeId}
     */
    void getGroupHomeId (int groupId, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link GroupService#getMembershipGroups}
     */
    void getMembershipGroups (int memberId, boolean canInvite,
                              AsyncCallback<List<GroupMembership>> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupDetail}
     */
    void getGroupDetail (int groupId, AsyncCallback<GroupDetail> callback);

    /**
     * The asynchronous version of {@link GroupService#getGroupMembers}
     */
    void getGroupMembers (int groupId, int offset, int count,
        AsyncCallback<PagedResult<GroupMemberCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#transferRoom}
     */
    void transferRoom (int groupId, int sceneId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#updateGroup}
     */
    void updateGroup (Group group, GroupExtras extras, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#quoteCreateGroup}
     */
    void quoteCreateGroup (AsyncCallback<PriceQuote> callback);

    /**
     * The asynchronous version of {@link GroupService#createGroup}
     */
    void createGroup (
        Group group, GroupExtras extras, Currency currency, int authedAmount,
        AsyncCallback<PurchaseResult<Group>> callback);

    /**
     * The asynchronous version of {@link GroupService#leaveGroup}
     */
    void leaveGroup (int groupId, int memberId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#joinGroup}
     */
    void joinGroup (int groupId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#updateMemberRank}
     */
    void updateMemberRank (int groupId, int memberId, Rank newRank, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#tagGroup}
     */
    void tagGroup (int groupId, String tag, boolean set, AsyncCallback<TagHistory> callback);

    /**
     * The asynchronous version of {@link GroupService#getRecentTags}
     */
    void getRecentTags (AsyncCallback<List<TagHistory>> callback);

    /**
     * The asynchronous version of {@link GroupService#getTags}
     */
    void getTags (int groupId, AsyncCallback<List<String>> callback);

    /**
     * The asynchronous version of {@link GroupService#getMyGroups}
     */
    void getMyGroups (AsyncCallback<List<GroupCard>> callback);

    /**
     * The asynchronous version of {@link GroupService#getGameGroups}
     */
    void getGameGroups (int gameId, AsyncCallback<List<GroupMembership>> callback);

    /**
     * The asynchronous version of {@link GroupService#updateMedal}
     */
    void updateMedal (Medal medal, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#getAwardedMedals}
     */
    void getAwardedMedals (int groupId, AsyncCallback<GroupService.MedalsResult> callback);

    /**
     * The asynchronous version of {@link GroupService#getMedals}
     */
    void getMedals (int groupId, AsyncCallback<List<Medal>> callback);

    /**
     * The asynchronous version of {@link GroupService#searchGroupMembers}
     */
    void searchGroupMembers (
        int groupId, String search, AsyncCallback<List<VizMemberName>> callback);

    /**
     * The asynchronous version of {@link GroupService#awardMedal}
     */
    void awardMedal (int memberId, int medalId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link GroupService#getMedal}
     */
    void getMedal (int medalId, AsyncCallback<Medal> callback);
}
