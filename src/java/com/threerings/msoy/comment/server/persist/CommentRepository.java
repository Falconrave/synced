//
// $Id$

package com.threerings.msoy.comment.server.persist;

import java.sql.Timestamp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.comment.data.all.Comment;

import static com.threerings.msoy.Log.log;

/**
 * Manages member comments on various and sundry things.
 */
@Singleton @BlockingThread
public class CommentRepository extends DepotRepository
{
    @Inject public CommentRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads the most recent comments for the specified entity type and identifier.
     *
     * @param start the offset into the comments (in reverse time order) to load.
     * @param count the number of comments to load.
     */
    public List<CommentRecord> loadComments (
        int entityType, int entityId, int start, int count, boolean byRating)
    {
        // load up the specified comment set
        return findAll(CommentRecord.class,
                       new Where(CommentRecord.ENTITY_TYPE, entityType,
                                 CommentRecord.ENTITY_ID, entityId),
                       byRating ? OrderBy.descending(CommentRecord.CURRENT_RATING) :
                                  OrderBy.descending(CommentRecord.POSTED),
                       new Limit(start, count));
    }

    /**
     * Loads the given member's ratings of the comments for the given entity.
     */
    public List<CommentRatingRecord> loadRatings (int entityType, int entityId, int memberId)
    {
        return findAll(CommentRatingRecord.class,
                       new Where(CommentRatingRecord.ENTITY_TYPE, entityType,
                                 CommentRatingRecord.ENTITY_ID, entityId,
                                 CommentRatingRecord.MEMBER_ID, memberId));
    }

    /**
     * Loads the given member's ratings of the comments for the given entity.
     */
    public CommentRatingRecord loadRating (
        int entityType, int entityId, long posted, int memberId)
    {
        return load(CommentRatingRecord.class, CommentRatingRecord.getKey(
                        entityType, entityId, memberId, new Timestamp(posted)));
    }

    /**
     * Loads a specific comment record.
     */
    public CommentRecord loadComment (int entityType, int entityId, long posted)
    {
        return load(CommentRecord.class,
                    CommentRecord.getKey(entityType, entityId, new Timestamp(posted)));
    }

    /**
     * Loads the total number of comments posted to the specified entity.
     */
    public int loadCommentCount (int entityType, int entityId)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(CommentRecord.class));
        clauses.add(new Where(CommentRecord.ENTITY_TYPE, entityType,
                              CommentRecord.ENTITY_ID, entityId));
        return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
    }

    /**
     * Posts a new comment on the specified entity by the specified member.
     */
    public CommentRecord postComment (int entityType, int entityId, int memberId, String text)
    {
        if (text.length() > Comment.MAX_TEXT_LENGTH) { // sanity check
            throw new DatabaseException(
                "Rejecting overlong comment [type=" + entityType + ", id=" + entityId +
                ", who=" + memberId + ", length=" + text.length() + "]");
        }

        CommentRecord record = new CommentRecord();
        record.entityType = entityType;
        record.entityId = entityId;
        record.posted = new Timestamp(System.currentTimeMillis());
        record.memberId = memberId;
        record.currentRating = 1;
        record.text = text;
        insert(record);

        return record;
    }

    /**
     * Inserts a new rating for a comment by a given member.
     * @return true if the comment's rating changed
     */
    public int rateComment (
        int entityType, int entityId, long posted, int memberId, boolean rating)
    {
        Timestamp postedStamp = new Timestamp(posted);
        try {
            // see if this person has rated this record before
            CommentRatingRecord record = load(CommentRatingRecord.class,
                CommentRatingRecord.getKey(entityType, entityId, memberId, postedStamp));

            int adjustment;
            if (record != null) {
                if (record.rating == rating) {
                    // re-rated precisely as previously; we're done
                    return 0;
                }
                // previously rated and user changed their mind; comment gains or loses 2 votes
                adjustment = rating ? 2 : -2;
            } else {
                // previously unrated; the comment gains or loses 1 vote
                adjustment = rating ? 1 : -1;
            }

            // create a new record with the new rating
            CommentRatingRecord newRecord =
                new CommentRatingRecord(entityType, entityId, postedStamp, memberId, rating);

            // insert or update depending on what we already had
            if (record == null) {
                insert(newRecord);
            } else {
                update(newRecord);
            }

            // then update the sums in the comment
            Key<CommentRecord> comment = CommentRecord.getKey(entityType, entityId, postedStamp);
            Map<ColumnExp<?>, SQLExpression<?>> updates = Maps.newHashMap();
            updates.put(CommentRecord.CURRENT_RATING,
                        CommentRecord.CURRENT_RATING.plus(adjustment));
            if (record != null) {
                updates.put(CommentRecord.TOTAL_RATINGS, CommentRecord.TOTAL_RATINGS.plus(1));
            }
            updatePartial(CommentRecord.class, comment, comment, updates);
            return adjustment;

        } catch (DuplicateKeyException dke) {
            log.warning("Ignoring duplicate comment rating", "entityType", entityType,
                        "entityId", entityId, "posted", postedStamp, "memberId", memberId,
                        "rating", rating);
            return 0;
        }
    }

    /**
     * Deletes the comment with the specified key.
     */
    public void deleteComment (int entityType, int entityId, long posted)
    {
        Timestamp postedStamp = new Timestamp(posted);

        // delete the comment
        delete(CommentRecord.getKey(entityType, entityId, postedStamp));

        // delete all its ratings
        deleteAll(CommentRatingRecord.class,
                  new Where(CommentRatingRecord.ENTITY_TYPE, entityType,
                            CommentRatingRecord.ENTITY_ID, entityId,
                            CommentRatingRecord.POSTED, postedStamp));
    }

    /**
     * Deletes all comments for the specified entity.
     */
    public void deleteComments (int entityType, int entityId)
    {
        // delete the comments
        deleteAll(CommentRecord.class, new Where(CommentRecord.ENTITY_TYPE, entityType,
                                                 CommentRecord.ENTITY_ID, entityId), null);

        // delete the comment ratings
        deleteAll(CommentRatingRecord.class,
                  new Where(CommentRatingRecord.ENTITY_TYPE, entityType,
                            CommentRatingRecord.ENTITY_ID, entityId), null);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging *
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // delete all ratings made by these members
        deleteAll(CommentRatingRecord.class,
                  new Where(CommentRatingRecord.MEMBER_ID.in(memberIds)));

        // load up the ids of all comments made by these members
        List<Key<CommentRecord>> keys = findAllKeys(
            CommentRecord.class, false, new Where(CommentRecord.MEMBER_ID.in(memberIds)));

        // delete those comments
        deleteAll(CommentRecord.class, KeySet.newKeySet(CommentRecord.class, keys));

        // TODO: delete all rating records made on the above comments
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CommentRecord.class);
        classes.add(CommentRatingRecord.class);
    }
}
