//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntSet;

import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.msoy.person.util.FeedMessageType;

/**
 * Maintains persistent data for feeds.
 */
public class FeedRepository extends DepotRepository
{
    @Computed @Entity
    public static class FeedMessageCount extends PersistentRecord {
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    public FeedRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all applicable feed messages for the specified member.
     *
     * @param since a timestamp before which not to load messages or null if all available messages
     * should be loaded.
     */
    public List<FeedMessageRecord> loadMemberFeed (
        int memberId, IntSet friendIds, IntSet groupIds, Timestamp since)
        throws PersistenceException
    {
        List<FeedMessageRecord> messages = Lists.newArrayList();
        loadFeedMessages(messages, GlobalFeedMessageRecord.class, null, since);
        if (!friendIds.isEmpty()) {
            SQLOperator actors = null;
            actors = new Conditionals.In(FriendFeedMessageRecord.ACTOR_ID_C, friendIds);
            loadFeedMessages(messages, FriendFeedMessageRecord.class, actors, since);
        }
        if (!groupIds.isEmpty()) {
            SQLOperator groups = null;
            groups = new Conditionals.In(GroupFeedMessageRecord.GROUP_ID_C, groupIds);
            loadFeedMessages(messages, GroupFeedMessageRecord.class, groups, since);
        }
        return messages;
    }

    /**
     * Loads all applicable feed messages by the specified member.
     *
     * @param since a timestamp before which not to load messages of null if lal available messages
     * should be loaded.
     */
    public List <FeedMessageRecord> loadPersonalFeed (int memberId, Timestamp since)
        throws PersistenceException
    {
        List<FeedMessageRecord> messages = Lists.newArrayList();
        SQLOperator actor = new Conditionals.Equals(FriendFeedMessageRecord.ACTOR_ID_C, memberId);
        loadFeedMessages(messages, FriendFeedMessageRecord.class, actor, since);
        return messages;
    }


    /**
     * Publishes a global message which will show up in all users' feeds. Note: global messages are
     * never throttled.
     */
    public void publishGlobalMessage (FeedMessageType type, String data)
        throws PersistenceException
    {
        GlobalFeedMessageRecord message = new GlobalFeedMessageRecord();
        message.type = type.getCode();
        message.data = data;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
    }

    /**
     * Publishes a feed message to the specified actor's friends.
     *
     * @return true if the message was published, false if it was throttled because it would cause
     * messages of the specified type to exceed their throttle period.
     */
    public boolean publishMemberMessage (int actorId, FeedMessageType type, String data)
        throws PersistenceException
    {
        if (type.getThrottleCount() > 0) {
            Timestamp throttle =
                new Timestamp(System.currentTimeMillis() - type.getThrottlePeriod());
            List<SQLOperator> bits = Lists.newArrayList();
            bits.add(new Conditionals.Equals(FriendFeedMessageRecord.ACTOR_ID_C, actorId));
            bits.add(new Conditionals.Equals(FriendFeedMessageRecord.TYPE_C, type.getCode()));
            bits.add(new Conditionals.GreaterThan(FriendFeedMessageRecord.POSTED_C, throttle));

            FeedMessageCount count = load(FeedMessageCount.class,
                    new FromOverride(FriendFeedMessageRecord.class),
                    new Where(new Logic.And(bits)));

            if (count.count >= type.getThrottleCount()) {
                return false;
            }
        }

        FriendFeedMessageRecord message = new FriendFeedMessageRecord();
        message.type = type.getCode();
        message.data = data;
        message.actorId = actorId;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
        return true;
    }

    /**
     * Publishes a feed message to the specified group's members.
     */
    public boolean publishGroupMessage (int groupId, FeedMessageType type, String data)
        throws PersistenceException
    {
        if (type.getThrottleCount() > 0) {
            Timestamp throttle =
                new Timestamp(System.currentTimeMillis() - type.getThrottlePeriod());
            List<SQLOperator> bits = Lists.newArrayList();
            bits.add(new Conditionals.Equals(GroupFeedMessageRecord.GROUP_ID_C, groupId));
            bits.add(new Conditionals.Equals(GroupFeedMessageRecord.TYPE_C, type.getCode()));
            bits.add(new Conditionals.GreaterThan(GroupFeedMessageRecord.POSTED_C, throttle));

            FeedMessageCount count = load(FeedMessageCount.class,
                    new FromOverride(GroupFeedMessageRecord.class),
                    new Where(new Logic.And(bits)));

            if (count.count >= type.getThrottleCount()) {
                return false;
            }
        }

        GroupFeedMessageRecord message = new GroupFeedMessageRecord();
        message.type = type.getCode();
        message.data = data;
        message.groupId = groupId;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
        return true;
    }

    // TODO: call this method periodically on the server... I'm not immediately thinking of a good
    // way to do this; having the repository register an Interval at construct time seems sketchy
    /**
     * Prunes feed messages of all types that have expired.
     */
    public void pruneFeeds ()
        throws PersistenceException
    {
        final Timestamp cutoff = new Timestamp(System.currentTimeMillis() - FEED_EXPIRATION_PERIOD);
        deleteAll(GlobalFeedMessageRecord.class,
                new Where(new Conditionals.LessThan(GlobalFeedMessageRecord.POSTED_C, cutoff)),
                new CacheInvalidator.TraverseWithFilter<GlobalFeedMessageRecord>(
                    GlobalFeedMessageRecord.class) {
                    public boolean testForEviction (
                        Serializable key, GlobalFeedMessageRecord record) {
                        return (record != null) && record.posted.before(cutoff);
                    }
                });
        deleteAll(FriendFeedMessageRecord.class,
                new Where(new Conditionals.LessThan(FriendFeedMessageRecord.POSTED_C, cutoff)),
                new CacheInvalidator.TraverseWithFilter<FriendFeedMessageRecord>(
                    FriendFeedMessageRecord.class) {
                    public boolean testForEviction (
                        Serializable key, FriendFeedMessageRecord record) {
                        return (record != null) && record.posted.before(cutoff);
                    }
                });
        deleteAll(GroupFeedMessageRecord.class,
                new Where(new Conditionals.LessThan(GroupFeedMessageRecord.POSTED_C, cutoff)),
                new CacheInvalidator.TraverseWithFilter<GroupFeedMessageRecord>(
                    GroupFeedMessageRecord.class) {
                    public boolean testForEviction (
                        Serializable key, GroupFeedMessageRecord record) {
                        return (record != null) && record.posted.before(cutoff);
                    }
                });
    }

    /**
     * Helper function for {@link #loadMemberFeed}.
     */
    protected void loadFeedMessages (List<FeedMessageRecord> messages,
                                     Class<? extends FeedMessageRecord> pClass,
                                     SQLOperator main, Timestamp since)
        throws PersistenceException
    {
        List<SQLOperator> whereBits = Lists.newArrayList();
        if (main != null) {
            whereBits.add(main);
        }
        if (since != null) {
            whereBits.add(new Conditionals.GreaterThanEquals(
                              new ColumnExp(pClass, FeedMessageRecord.POSTED), since));
        }
        messages.addAll(findAll(pClass, new Where(new Logic.And(whereBits))));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FriendFeedMessageRecord.class);
        classes.add(GroupFeedMessageRecord.class);
        classes.add(GlobalFeedMessageRecord.class);
    }

    /** Feed messages expire after two weeks. */
    protected static final long FEED_EXPIRATION_PERIOD = 14 * 24 * 60 * 60 * 1000L;
}
