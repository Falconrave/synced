//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.server.persist.GroupRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.GroupMembershipRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRepository;
import com.threerings.msoy.server.persist.TagPopularityRecord;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class GroupServlet extends MsoyServiceServlet
    implements GroupService
{
    // from GroupService
    public List<Group> getGroupsList (WebCreds creds)
        throws ServiceException
    {
        try {
            List<Group> groups = new ArrayList<Group>();
            for (GroupRecord gRec : MsoyServer.groupRepo.getGroupsList()) {
                groups.add(gRec.toGroupObject());
            }
            return groups;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGroupsList failed", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    /**
     * Fetches the members of a given group, as {@link GroupMembership} records. This method
     * does not distinguish between a nonexistent group and a group without members;
     * both situations yield empty collections.
     */
    public GroupDetail getGroupDetail (WebCreds creds, int groupId)
        throws ServiceException
    {
        try {
            // load the group record
            GroupRecord gRec = MsoyServer.groupRepo.loadGroup(groupId);
            if (gRec == null) {
                return null;
            }

            // load the creator's member record
            MemberRecord mRec = MsoyServer.memberRepo.loadMember(gRec.creatorId);
            if (mRec == null) {
                log.warning("Couldn't load group creator [groupId=" + groupId +
                    ", creatorId=" + gRec.creatorId + "]");
                throw new ServiceException(ServiceException.INTERNAL_ERROR);
            }

            // set up the detail
            GroupDetail detail = new GroupDetail();
            detail.creator = mRec.getName();
            detail.group = gRec.toGroupObject();
            detail.extras = gRec.toExtrasObject();
            ArrayList<GroupMembership> members = new ArrayList<GroupMembership>();
            detail.members = members;
            for (GroupMembershipRecord gmRec : MsoyServer.groupRepo.getMembers(groupId)) {
                mRec = MsoyServer.memberRepo.loadMember(gmRec.memberId);
                if (mRec == null) {
                    log.warning("Group has non-existent member [groupId=" + groupId +
                                ", memberId=" + gmRec.memberId + "].");
                    continue;
                }
                GroupMembership membership = new GroupMembership();
                // membership.group left null intentionally 
                membership.member = mRec.getName();
                membership.rank = gmRec.rank;
                membership.rankAssignedDate = gmRec.rankAssigned.getTime();
                members.add(membership);
            }
            return detail;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getGroupDetail failed [groupId=" + groupId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from GroupService
    public Integer getGroupHomeId (WebCreds creds, final int groupId)
        throws ServiceException
    {
        final ServletWaiter<Integer> waiter =new ServletWaiter<Integer>(
            "getHomeId[" + groupId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.memberMan.getHomeId(MsoySceneModel.OWNER_TYPE_GROUP, groupId, waiter);
            }
        });
        return waiter.waitForResult();
    }
    
    // from interface GroupService
    public List<Group> searchGroups (WebCreds creds, String searchString) 
        throws ServiceException
    {
        try {
            List<Group> groups = new ArrayList<Group>();
            for (GroupRecord gRec : MsoyServer.groupRepo.searchGroups(searchString)) {
                groups.add(gRec.toGroupObject());
            }
            return groups;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "searchGroups failed [searchString=" + searchString + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public List<Group> searchForTag (WebCreds creds, String tag)
        throws ServiceException
    {
        try {
            List<Group> groups = new ArrayList<Group>();
            for (GroupRecord gRec : MsoyServer.groupRepo.searchForTag(tag)) {
                groups.add(gRec.toGroupObject());
            }
            return groups;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "searchForTag failed [tag=" + tag + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public List<GroupMembership> getMembershipGroups (
        WebCreds creds, final int memberId, final boolean canInvite)
        throws ServiceException
    {
        int requesterId = getMemberId(creds);

        try {
            List<GroupMembership> result = new ArrayList<GroupMembership>();
            MemberRecord mRec = MsoyServer.memberRepo.loadMember(memberId);
            if (mRec == null) {
                log.warning("Requested group membership for unknown member [id=" + memberId + "].");
                return result;
            }

            for (GroupMembershipRecord gmRec : MsoyServer.groupRepo.getMemberships(memberId)) {
                GroupRecord gRec = MsoyServer.groupRepo.loadGroup(gmRec.groupId);
                if (gRec == null) {
                    log.warning("Unknown group membership [memberId=" + memberId +
                                ", groupId=" + gmRec.groupId + "]");
                    continue;
                }

                // if we're not the person in question, don't show exclusive groups
                if (memberId != requesterId && gRec.policy == Group.POLICY_EXCLUSIVE) {
                    continue;
                }

                // if we're only including groups we can invite to, strip out non-public groups of
                // which we're not managers
                if (canInvite && gRec.policy != Group.POLICY_PUBLIC &&
                    gmRec.rank != GroupMembership.RANK_MANAGER) {
                    continue;
                }

                result.add(gmRec.toGroupMembership(gRec, mRec.getName()));
            }
            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getMembershipGroups failed [id=" + memberId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public Group createGroup (WebCreds creds, final Group group, final GroupExtras extras) 
        throws ServiceException
    {
        // we'll need the MemberRec for charging for this in the future
        MemberRecord memrec = requireAuthedUser(creds);

        if(!isValidName(group.name)) {
            log.log(Level.WARNING, "invalid group name: " + group.name);
            throw new ServiceException("m.invalid_group_name");
        }

        final ServletWaiter<Group> waiter = new ServletWaiter<Group>("createGroup[" + group + "]");
        group.creatorId = memrec.memberId;
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.groupMan.createGroup(group, extras, waiter);
            }
        });
        return waiter.waitForResult();
    }

    // from interface GroupService
    public void updateGroup (WebCreds creds, Group group, GroupExtras extras) 
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        
        if(!isValidName(group.name)) {
            log.log(Level.WARNING, "in updateGroup, invalid group name: " + group.name);
            throw new ServiceException("m.invalid_group_name");
        }

        try {
            GroupMembershipRecord gmrec = MsoyServer.groupRepo.getMembership(group.groupId, 
                memrec.memberId);
            if (gmrec == null || gmrec.rank != GroupMembership.RANK_MANAGER) {
                log.log(Level.WARNING, "in updateGroup, invalid permissions");
                throw new ServiceException("m.invalid_permissions");
            }

            GroupRecord gRec = MsoyServer.groupRepo.loadGroup(group.groupId);
            if (gRec == null) {
                throw new PersistenceException("Group not found! [id=" + group.groupId + 
                    "]");
            }
            Map<String, Object> updates = gRec.findUpdates(group, extras);
            if (updates.size() > 0) {
                MsoyServer.groupRepo.updateGroup(group.groupId, updates);
            }
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "updateGroup failed [group=" + group + ", extras=" + 
                extras + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public void leaveGroup (WebCreds creds, final int groupId, final int memberId)
        throws ServiceException
    {
        requireAuthedUser(creds);

        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "leaveGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.groupMan.leaveGroup(groupId, memberId, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from interface GroupService
    public void joinGroup (WebCreds creds, final int groupId, final int memberId)
        throws ServiceException
    {
        requireAuthedUser(creds);

        final ServletWaiter<Void> waiter = new ServletWaiter<Void>(
            "joinGroup[" + groupId + ", " + memberId + "]");
        MsoyServer.omgr.postRunnable(new Runnable() {
            public void run () {
                MsoyServer.groupMan.joinGroup(
                    groupId, memberId, GroupMembership.RANK_MEMBER, waiter);
            }
        });
        waiter.waitForResult();
    }

    // from interface GroupService
    public void updateMemberRank (WebCreds creds, int groupId, int memberId, byte newRank) 
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);

        try {
            GroupMembershipRecord gmrec = MsoyServer.groupRepo.getMembership(groupId, 
                memrec.memberId);
            if (gmrec == null || gmrec.rank != GroupMembership.RANK_MANAGER) {
                log.log(Level.WARNING, "in updateMemberRank, invalid permissions");
                throw new ServiceException("m.invalid_permissions");
            }

            MsoyServer.groupRepo.setRank(groupId, memberId, newRank);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "updateMemberRank failed [groupId=" + groupId + ", memberId=" +
                memberId + ", newRank=" + newRank + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public TagHistory tagGroup (WebCreds creds, int groupId, String tag, boolean set) 
        throws ServiceException
    {
        String tagName = tag.trim().toLowerCase();
        if (!TagNameRecord.VALID_TAG.matcher(tagName).matches()) {
            log.log(Level.WARNING, "in tagGroup, invalid tag: " + tagName);
            throw new ServiceException("Invalid tag [tag=" + tagName + "]");
        }

        MemberRecord memrec = requireAuthedUser(creds);

        try {
            GroupMembershipRecord gmrec = MsoyServer.groupRepo.getMembership(groupId, 
                memrec.memberId);
            if (gmrec == null || gmrec.rank != GroupMembership.RANK_MANAGER) {
                log.log(Level.WARNING, "in tagGroup, invalid permissions");
                throw new ServiceException("m.invalid_permissions");
            }

            long now = System.currentTimeMillis();
                
            TagRepository tagRepo = MsoyServer.groupRepo.getTagRepository();
            TagNameRecord tagRec = tagRepo.getTag(tagName);

            TagHistoryRecord historyRecord = set ?
                tagRepo.tag(groupId, tagRec.tagId, memrec.memberId, now) :
                tagRepo.untag(groupId, tagRec.tagId, memrec.memberId, now);
            if (historyRecord != null) {
                TagHistory history = new TagHistory();
                history.member = memrec.getName();
                history.tag = tagRec.tag;
                history.action = historyRecord.action;
                history.time = new Date(historyRecord.time.getTime());
                return history;
            }
            return null;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "tagGroup failed [groupId=" + groupId + ", tag=" + tag +
                ", set=" + set + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public Collection<TagHistory> getRecentTags (WebCreds creds) throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser(creds);
        int memberId = memrec.memberId;
        try {
            MemberRecord memRec = MsoyServer.memberRepo.loadMember(memberId);
            MemberName memName = memRec.getName();
            TagRepository tagRepo = MsoyServer.groupRepo.getTagRepository();
            ArrayList<TagHistory> list = new ArrayList<TagHistory>();
            for (TagHistoryRecord record : tagRepo.getTagHistoryByMember(memberId)) {
                TagNameRecord tag = record.tagId == -1 ? null :
                    tagRepo.getTag(record.tagId);
                TagHistory history = new TagHistory();
                history.member = memName;
                history.tag = tag == null ? null : tag.tag;
                history.action = record.action;
                history.time = new Date(record.time.getTime());
                list.add(history); 
            }
            return list;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getRecentTags failed", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public Collection<String> getTags (WebCreds creds, int groupId) throws ServiceException
    {
        try {
            ArrayList<String> result = new ArrayList<String>();
            for (TagNameRecord tagName : MsoyServer.groupRepo.getTagRepository().
                    getTags(groupId)) {
                result.add(tagName.tag);
            }
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getTags failed [groupId=" + groupId + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface GroupService
    public List<String> getPopularTags (WebCreds creds, int rows) throws ServiceException
    {
        try {
            ArrayList<String> result = new ArrayList<String>();
            for (TagPopularityRecord popRec : MsoyServer.groupRepo.getTagRepository().
                    getPopularTags(rows)) {
                result.add(popRec.tag);
            }
            return result;
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "getPopularTags failed [rows=" + rows + "]", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    protected static boolean isValidName (String name) 
    {
        return Character.isLetter(name.charAt(0)) || Character.isDigit(name.charAt(0));
    }
}
