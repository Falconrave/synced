//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.msoy.fora.data.Comment;

/**
 * Manages member comments on various and sundry things.
 */
public class CommentRepository extends DepotRepository
{
    /** Used by {@link #loadCommentCount}. */
    @Entity @Computed
    public static class CommentCountRecord extends PersistentRecord
    {
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    public CommentRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads the most recent comments for the specified entity type and identifier.
     *
     * @param start the offset into the comments (in reverse time order) to load.
     * @param count the number of comments to load.
     */
    public List<CommentRecord> loadComments (int entityType, int entityId, int start, int count)
        throws PersistenceException
    {
        // load up the specified comment set
        return findAll(CommentRecord.class,
                       new Where(CommentRecord.ENTITY_TYPE_C, entityType,
                                 CommentRecord.ENTITY_ID_C, entityId),
                       OrderBy.descending(CommentRecord.POSTED_C),
                       new Limit(start, count));
    }

    /**
     * Loads a specific comment record.
     */
    public CommentRecord loadComment (int entityType, int entityId, long posted)
        throws PersistenceException
    {
        return load(CommentRecord.class,
                    CommentRecord.getKey(entityType, entityId, new Timestamp(posted)));
    }

    /**
     * Loads the total number of comments posted to the specified entity.
     */
    public int loadCommentCount (int entityType, int entityId)
        throws PersistenceException
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(CommentRecord.class));
        clauses.add(new Where(CommentRecord.ENTITY_TYPE_C, entityType,
                              CommentRecord.ENTITY_ID_C, entityId));
        CommentCountRecord crec = load(
            CommentCountRecord.class, clauses.toArray(new QueryClause[clauses.size()]));
        return crec.count;
    }

    /**
     * Posts a new comment on the specified entity by the specified member.
     */
    public void postComment (int entityType, int entityId, int memberId, String text)
        throws PersistenceException
    {
        if (text.length() > Comment.MAX_TEXT_LENGTH) { // sanity check
            throw new PersistenceException(
                "Rejecting overlong comment [type=" + entityType + ", id=" + entityId +
                ", who=" + memberId + ", length=" + text.length() + "]");
        }

        CommentRecord record = new CommentRecord();
        record.entityType = entityType;
        record.entityId = entityId;
        record.posted = new Timestamp(System.currentTimeMillis());
        record.memberId = memberId;
        record.text = text;
        insert(record);
    }

    /**
     * Deletes the comment with the specified key.
     */
    public void deleteComment (int entityType, int entityId, long posted)
        throws PersistenceException
    {
        delete(CommentRecord.class,
               CommentRecord.getKey(entityType, entityId, new Timestamp(posted)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CommentRecord.class);
    }
}
