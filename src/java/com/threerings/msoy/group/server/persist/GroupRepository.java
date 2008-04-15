//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.Predicate;
import com.samskivert.util.Tuple;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.SelectClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagRepository;
import com.threerings.msoy.web.data.GroupCard;
import com.threerings.msoy.world.data.MsoySceneModel;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupMembership;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of group data.
 */
@BlockingThread
public class GroupRepository extends DepotRepository
{
    @Entity(name="GroupTagRecord")
    public static class GroupTagRecord extends TagRecord
    {
    }

    @Entity(name="GroupTagHistoryRecord")
    public static class GroupTagHistoryRecord extends TagHistoryRecord
    {
    }

    public GroupRepository (PersistenceContext ctx, MsoyEventLogger eventLog)
    {
        super(ctx);

        _eventLog = eventLog;

        _tagRepo = new TagRepository(_ctx) {
            protected TagRecord createTagRecord () {
                return new GroupTagRecord();
            }
            protected TagHistoryRecord createTagHistoryRecord () {
                return new GroupTagHistoryRecord();
            }
        };

        // TEMP
        ctx.registerMigration(GroupRecord.class, new EntityMigration(18) {
            public boolean runBeforeDefault () {
                return false;
            }
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String tName = liaison.tableSQL("GroupRecord");
                String pName = liaison.columnSQL(GroupRecord.POLICY);
                String fName = liaison.columnSQL(GroupRecord.FORUM_PERMS);

                int[] policies = {
                    Group.POLICY_PUBLIC, Group.POLICY_INVITE_ONLY, Group.POLICY_EXCLUSIVE };
                int[] forumPerms = {
                    Group.makePerms(Group.PERM_MEMBER, Group.PERM_ALL),
                    Group.makePerms(Group.PERM_MEMBER, Group.PERM_MEMBER),
                    Group.makePerms(Group.PERM_MEMBER, Group.PERM_MEMBER) };

                Statement stmt = conn.createStatement();
                try {
                    int totalRows = 0;
                    for (int ii = 0; ii < policies.length; ii++) {
                        int rows = stmt.executeUpdate(
                            "UPDATE " + tName + " set " + fName + " = " + forumPerms[ii] +
                            " where " + pName + " = " + policies[ii]);
                        log.info("Updated " + rows + " groups with policy " + policies[ii] + ".");
                        totalRows += rows;
                    }
                    return totalRows;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        // END TEMP
    }

    /**
     * Returns the repository used to track tags on groups.
     */
    public TagRepository getTagRepository ()
    {
        return _tagRepo;
    }

    /**
     * Returns a list of all public and inv-only groups, sorted by size, then by creation time.
     *
     * @param offset an offset into the collection of groups at which to start.
     * @param limit a limit to the number of groups to load or Integer.MAX_VALUE for all of them.
     */
    public List<GroupRecord> getGroupsList (int offset, int limit)
        throws PersistenceException
    {
        return findAll(
            GroupRecord.class,
            new Where(new Not(new Equals(GroupRecord.POLICY_C, Group.POLICY_EXCLUSIVE))),
            new Limit(offset, limit),
            new OrderBy(
                new SQLExpression[] { GroupRecord.MEMBER_COUNT_C, GroupRecord.CREATION_DATE_C },
                new OrderBy.Order[] { OrderBy.Order.DESC, OrderBy.Order.ASC }));
    }

    /**
     * Searches all public and inv-only groups for the search string against the indexed blurb,
     * charter and name fields.  Results are returned in order of relevance.
     */
    public List<GroupRecord> searchGroups (String search)
        throws PersistenceException
    {
        // for now, always operate with boolean searching enabled, without query expansion
        return findAll(
            GroupRecord.class,
            new Where(new And(new Not(new Equals(GroupRecord.POLICY_C, Group.POLICY_EXCLUSIVE)),
                              new FullTextMatch(GroupRecord.class, GroupRecord.FTS_NBC, search))));
    }

    /**
     * Searches all groups for the specified tag.  Tagging is not supported on exclusive groups
     */
    public List<GroupRecord> searchForTag (String tag)
        throws PersistenceException
    {
        List<Integer> groupIds = Lists.newArrayList();
        int tagId = _tagRepo.getOrCreateTag(tag).tagId;
        Where where = new Where(new ColumnExp(GroupTagRecord.class, GroupTagRecord.TAG_ID), tagId);
        for (GroupTagRecord tagRec : findAll(GroupTagRecord.class, where)) {
            groupIds.add(tagRec.targetId);
        }
        return findAll(GroupRecord.class,
                       new Where(new In(GroupRecord.class, GroupRecord.GROUP_ID, groupIds)));
    }

    /**
     * Fetches a single group, by id. Returns null if there's no such group.
     */
    public GroupRecord loadGroup (int groupId)
        throws PersistenceException
    {
        return load(GroupRecord.class, groupId);
    }

    /**
     * Fetches multiple groups by id.
     */
    public List<GroupRecord> loadGroups (Set<Integer> groupIds)
        throws PersistenceException
    {
        if (groupIds.size() == 0) {
            return Collections.emptyList();
        } else {
            return findAll(GroupRecord.class, new Where(new In(GroupRecord.GROUP_ID_C, groupIds)));
        }
    }

    /**
     * Looks up a group's name by id. Returns null if no group exists with the specified id.
     */
    public GroupName loadGroupName (int groupId)
        throws PersistenceException
    {
        List<GroupName> result = loadGroupNames(Collections.singleton(groupId));
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * Looks up groups' names by id.
     */
    public List<GroupName> loadGroupNames (Set<Integer> groupIds)
        throws PersistenceException
    {
        List<GroupName> names = Lists.newArrayList();
        if (groupIds.size() > 0) {
            for (GroupNameRecord gnr : findAll(
                     GroupNameRecord.class,
                     new Where(new In(GroupRecord.GROUP_ID_C, groupIds)))) {
                names.add(gnr.toGroupName());
            }
        }
        return names;
    }

    /**
     * Creates a new group, defined by a {@link GroupRecord}. The key of the record must be null --
     * it will be filled in through the insertion, and returned.  A blank room is also created that
     * is owned by the group.
     */
    public int createGroup (GroupRecord record)
        throws PersistenceException
    {
        if (record.groupId != 0) {
            throw new PersistenceException(
                "Group record must have a null id for creation " + record);
        }
        record.creationDate = new Date(System.currentTimeMillis());
        insert(record);

        int sceneId = MsoyServer.sceneRepo.createBlankRoom(
            MsoySceneModel.OWNER_TYPE_GROUP, record.groupId, record.name, null, true);
        updateGroup(record.groupId, GroupRecord.HOME_SCENE_ID, sceneId);

        return record.groupId;
    }

    /**
     * Updates the specified group record with field/value pairs, e.g.
     *     updateGroup(groupId,
     *                 GroupRecord.CHARTER, newCharter,
     *                 GroupRecord.POLICY, Group.EXCLUSIVE);
     */
    public void updateGroup (int groupId, Object... fieldValues)
        throws PersistenceException
    {
        int rows = updatePartial(GroupRecord.class, groupId, fieldValues);
        if (rows == 0) {
            throw new PersistenceException("Couldn't find group for update [id=" + groupId + "]");
        }
    }

    /**
     * Updates the specified group record with supplied field/value mapping.
     */
    public void updateGroup (int groupId, Map<String, Object> updates)
        throws PersistenceException
    {
        int rows = updatePartial(GroupRecord.class, groupId, updates);
        if (rows == 0) {
            throw new PersistenceException("Couldn't find group for update [id=" + groupId + "]");
        }
    }

    /**
     * Deletes the specified group from the repository. This assumes that the group has no members
     * and thus does not remove any {@link GroupMembershipRecord} rows. It also assumes the caller
     * will take care of deleting the group's scenes.
     */
    public void deleteGroup (int groupId)
        throws PersistenceException
    {
        delete(GroupRecord.class, groupId);
    }

    /**
     * Makes a given person a member of a given group.
     */
    public void joinGroup (int groupId, int memberId, byte rank)
        throws PersistenceException
    {
        GroupMembershipRecord record = new GroupMembershipRecord();
        record.groupId = groupId;
        record.memberId = memberId;
        record.rank = rank;
        record.rankAssigned = new Timestamp(System.currentTimeMillis());
        insert(record);
        updateMemberCount(groupId);

        _eventLog.groupJoined(memberId, groupId);
    }

    /**
     * Sets the rank of a member of a group.
     */
    public void setRank (int groupId, int memberId, byte newRank)
        throws PersistenceException
    {
        Key key = GroupMembershipRecord.getKey(memberId, groupId);
        int rows = updatePartial(
            GroupMembershipRecord.class, key, key,
            GroupMembershipRecord.RANK, newRank,
            GroupMembershipRecord.RANK_ASSIGNED, new Timestamp(System.currentTimeMillis()));
        if (rows == 0) {
            throw new PersistenceException(
                "Couldn't find group membership to modify [groupId=" + groupId +
                "memberId=" + memberId + "]");
        } else {
            _eventLog.groupRankChange(memberId, groupId, newRank);
        }
    }

    /**
     * Fetches the membership details for a given group and member, or null.
     */
    public GroupMembershipRecord getMembership (int groupId, int memberId)
        throws PersistenceException
    {
        return load(GroupMembershipRecord.class,
                    GroupMembershipRecord.GROUP_ID, groupId,
                    GroupMembershipRecord.MEMBER_ID, memberId);
    }

    /**
     * Resolves the group membership information for the supplied member.
     *
     * @param filter if non-null, only membership in groups that match the supplied filter will be
     * returned.
     */
    public List<GroupMembership> resolveGroupMemberships (
        int memberId, Predicate<Tuple<GroupRecord,GroupMembershipRecord>> filter)
        throws PersistenceException
    {
        List<GroupMembershipRecord> records = MsoyServer.groupRepo.getMemberships(memberId);
        IntMap<GroupMembershipRecord> rmap = IntMaps.newHashIntMap();
        for (GroupMembershipRecord record : records) {
            rmap.put(record.groupId, record);
        }

        // potentially filter exclusive groups and resolve the group names
        IntMap<GroupName> groupNames = IntMaps.newHashIntMap();
        for (GroupRecord group : MsoyServer.groupRepo.loadGroups(rmap.keySet())) {
            if (filter == null || filter.isMatch(new Tuple<GroupRecord,GroupMembershipRecord>(
                                                     group, rmap.get(group.groupId)))) {
                groupNames.put(group.groupId, group.toGroupName());
            }
        }

        // convert the persistent membership records into runtime records
        List<GroupMembership> groups = Lists.newArrayList();
        for (GroupMembershipRecord record : records) {
            if (groupNames.containsKey(record.groupId)) {
                groups.add(record.toGroupMembership(groupNames));
            }
        }
        return groups;
    }

    /**
     * Returns a list of cards for the groups of which the specified person is a member.
     */
    public List<GroupCard> getMemberGroups (int memberId, boolean includeExclusive)
        throws PersistenceException
    {
        List<GroupMembershipRecord> records = MsoyServer.groupRepo.getMemberships(memberId);
        IntMap<GroupMembershipRecord> rmap = IntMaps.newHashIntMap();
        for (GroupMembershipRecord record : records) {
            rmap.put(record.groupId, record);
        }

        // potentially filter exclusive groups and resolve the group names
        List<GroupCard> groups = Lists.newArrayList();
        for (GroupRecord group : MsoyServer.groupRepo.loadGroups(rmap.keySet())) {
            if (group.policy != Group.POLICY_EXCLUSIVE || includeExclusive) {
                groups.add(group.toGroupCard());
            }
        }
        return groups;
    }

    /**
     * Remove a given person as member of a given group. This method returns false if there was no
     * membership to cancel.
     */
    public boolean leaveGroup (int groupId, int memberId)
        throws PersistenceException
    {
        Key key = GroupMembershipRecord.getKey(memberId, groupId);
        int rows = deleteAll(GroupMembershipRecord.class, key, key);
        updateMemberCount(groupId);
        _eventLog.groupLeft(memberId, groupId);
        return rows > 0;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public int countMembers (int groupId)
        throws PersistenceException
    {
        return load(CountRecord.class,
                    new FromOverride(GroupMembershipRecord.class),
                    new Where(GroupMembershipRecord.GROUP_ID_C, groupId)).count;
    }

    /**
     * Fetches the membership roster of a given group.
     */
    public List<GroupMembershipRecord> getMembers (int groupId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.GROUP_ID_C, groupId));
    }

    /**
     * Fetches the membership roster of a given group for a given member rank.
     */
    public List<GroupMembershipRecord> getMembers (int groupId, byte rank)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(new And(
                               new Equals(GroupMembershipRecord.GROUP_ID_C, groupId),
                               new Equals(GroupMembershipRecord.RANK_C, rank))));
    }

    /**
     * Fetches the group memberships a given member belongs to.
     */
    public List<GroupMembershipRecord> getMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(GroupMembershipRecord.class,
                       new Where(GroupMembershipRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Fetches the full records of the groups a given member belongs to.
     */
    public List<GroupRecord> getFullMemberships (int memberId)
        throws PersistenceException
    {
        return findAll(GroupRecord.class,
                       new Join(GroupRecord.GROUP_ID_C, GroupMembershipRecord.GROUP_ID_C),
                       new Where(GroupMembershipRecord.MEMBER_ID_C, memberId));
    }

    protected void updateMemberCount (int groupId)
        throws PersistenceException
    {
        Map<String, SQLExpression> fieldMap = Maps.newHashMap();
        fieldMap.put(GroupRecord.MEMBER_COUNT,
                     new SelectClause<CountRecord>(
                         CountRecord.class,
                         new String[] { CountRecord.COUNT },
                         new FromOverride(GroupMembershipRecord.class),
                         new Where(GroupMembershipRecord.GROUP_ID_C, groupId)));
        updateLiteral(GroupRecord.class, groupId, fieldMap);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GroupRecord.class);
        classes.add(GroupMembershipRecord.class);
    }

    /** Used to manage our group tags. */
    protected TagRepository _tagRepo;

    /** Reference to the event logger. */
    protected MsoyEventLogger _eventLog;
}
