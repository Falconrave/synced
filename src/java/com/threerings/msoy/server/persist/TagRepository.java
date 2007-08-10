//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Modifier;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.operator.Conditionals.In;

import com.threerings.msoy.web.data.TagHistory;

/**
 * Manages the persistent side of tagging of things in the MetaSOY system (right now items and
 * groups).
 */
public abstract class TagRepository extends DepotRepository
{
    /**
     * Creates a tag repository for the supplied tag and tag history record classes.
     */
    public TagRepository (PersistenceContext ctx)
    {
        super(ctx);

        @SuppressWarnings("unchecked") Class<TagRecord> tagClass = (Class<TagRecord>)
            createTagRecord().getClass();
        _tagClass = tagClass;

        @SuppressWarnings("unchecked") Class<TagHistoryRecord> thClass = (Class<TagHistoryRecord>)
            createTagHistoryRecord().getClass();
        _tagHistoryClass = thClass;
    }

    /**
     * Creates a {@link TagRecord} derived instance the class for which defines a custom table name
     * for our tag record class. For example:
     * <pre>
     * @Entity(name="PhotoTagRecord")
     * public class PhotoTagRecord extends TagRecord {}
     * </pre>
     */
    protected abstract TagRecord createTagRecord ();

    /**
     * Creates a {@link TagRecord} derived instance the class for which defines a custom table name
     * for our tag record class. See {@link #createTagRecord}.
     */
    protected abstract TagHistoryRecord createTagHistoryRecord ();

    /**
     * Exports the specific tag class used by this repository, for joining purposes.
     */
    public Class<TagRecord> getTagClass ()
    {
        return _tagClass;
    }

    /**
     * Exports the specific tag history class used by this repository, for joining purposes.
     */
    public Class<TagHistoryRecord> getTagHistoryClass ()
    {
        return _tagHistoryClass;
    }

    /**
     * Join TagNameRecord and TagRecord, group by tag, and count how many targets reference each
     * such tag.
     */
    public List<TagPopularityRecord> getPopularTags (int rows)
        throws PersistenceException
    {
        return findAll(TagPopularityRecord.class,
                       new FromOverride(getTagClass()),
                       new Limit(0, rows),
                       new Join(getTagColumn(TagRecord.TAG_ID), TagNameRecord.TAG_ID_C),
                       new FieldOverride(TagPopularityRecord.COUNT, "count(*)"),
                       OrderBy.descending(new LiteralExp("count(*)")),
                       new GroupBy(TagNameRecord.TAG_ID_C, TagNameRecord.TAG_C));
    }

    /**
     * Loads all tag records for the given target, translated to tag names.
     */
    public List<TagNameRecord> getTags (int targetId)
        throws PersistenceException
    {
        return findAll(TagNameRecord.class,
                       new Where(getTagColumn(TagRecord.TARGET_ID), targetId),
                       new Join(TagNameRecord.TAG_ID_C, getTagColumn(TagRecord.TAG_ID)));
    }

    /**
     * Loads all the tag history records for a given target.
     */
    public List<TagHistoryRecord> getTagHistoryByTarget (int targetId)
        throws PersistenceException
    {
        return findAll(getTagHistoryClass(),
                       new Where(getTagHistoryColumn(TagHistoryRecord.TARGET_ID), targetId));
    }

    /**
     * Loads all the tag history records for a given member.
     */
    public List<TagHistoryRecord> getTagHistoryByMember (int memberId)
        throws PersistenceException
    {
        return findAll(getTagHistoryClass(),
                       new Where(getTagHistoryColumn(TagHistoryRecord.MEMBER_ID), memberId));
    }

    /**
     * Loads all tag records for the specified tags.
     */
    public List<TagNameRecord> getTags (String[] tags)
        throws PersistenceException
    {
        return findAll(TagNameRecord.class, new Where(new In(TagNameRecord.TAG_C, tags)));
    }

    /**
     * Finds the tag record for a certain tag.
     */
    public TagNameRecord getTag (String tagName)
        throws PersistenceException
    {
        return load(TagNameRecord.class, new Where(TagNameRecord.TAG_C, tagName));
    }

    /**
     * Finds the tag record for a certain tag, or create it.
     */
    public TagNameRecord getOrCreateTag (String tagName)
        throws PersistenceException
    {
        // load the tag, if it exists
        TagNameRecord record = getTag(tagName);
        if (record == null) {
            // if it doesn't, create it on the fly
            record = new TagNameRecord();
            record.tag = tagName;
            insert(record);
        }
        return record;
    }

    /**
     * Find the tag record for a certain tag id.
     */
    public TagNameRecord getTag (int tagId)
        throws PersistenceException
    {
        return load(TagNameRecord.class, tagId);
    }

    /**
     * Adds a tag to a target. If the tag already exists, returns false and do nothing else. If it
     * did not, creates the tag and adds a record in the history table.
     */
    public TagHistoryRecord tag (int targetId, int tagId, int taggerId, long now)
        throws PersistenceException
    {
        TagRecord tag = load(getTagClass(), TagRecord.TARGET_ID, targetId, TagRecord.TAG_ID, tagId);
        if (tag != null) {
            return null;
        }

        tag = createTagRecord();
        tag.targetId = targetId;
        tag.tagId = tagId;
        insert(tag);

        TagHistoryRecord history = createTagHistoryRecord();
        history.targetId = targetId;
        history.tagId = tagId;
        history.memberId = taggerId;
        history.action = TagHistory.ACTION_ADDED;
        history.time = new Timestamp(now);
        insert(history);
        return history;
    }

    /**
     * Removes a tag from a target. If the tag didn't exist, returns false and do nothing else. If
     * it did, removes the tag and adds a record in the history table.
     */
    public TagHistoryRecord untag (int targetId, int tagId, int taggerId, long now)
        throws PersistenceException
    {
        TagRecord tag = load(getTagClass(), TagRecord.TARGET_ID, targetId, TagRecord.TAG_ID, tagId);
        if (tag == null) {
            return null;
        }
        delete(tag);

        TagHistoryRecord history = createTagHistoryRecord();
        history.targetId = targetId;
        history.tagId = tagId;
        history.memberId = taggerId;
        history.action = TagHistory.ACTION_REMOVED;
        history.time = new Timestamp(now);
        insert(history);
        return history;
    }

    /**
     * Copy all tags from one target to another. We have to resort to JDBC here, because we want to
     * do the rather non-generic:
     *
     *   INSERT INTO PhotoTagRecord (targetId, tagId)
     *        SELECT 153567, tagId
     *          FROM PhotoTagRecord
     *         WHERE targetId = 89736;
     *
     * TODO: Depot is very very close to being able to handle this.
     */
    public int copyTags (final int fromTargetId, final int toTargetId, int ownerId, long now)
        throws PersistenceException
    {
        final String tagTable = _ctx.getMarshaller(getTagClass()).getTableName();
        int rows = _ctx.invoke(new Modifier() {
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        " INSERT INTO " + liaison.tableSQL(tagTable) + " (" +
                        liaison.columnSQL(TagRecord.TARGET_ID) + ", " +
                        liaison.columnSQL(TagRecord.TAG_ID) + ")" +
                        "      SELECT ?, " + liaison.columnSQL(TagRecord.TAG_ID) +
                        "        FROM " + liaison.tableSQL(tagTable) +
                        "       WHERE " + liaison.columnSQL(TagRecord.TARGET_ID) + " = ?");
                    stmt.setInt(1, toTargetId);
                    stmt.setInt(2, fromTargetId);
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        // add a single row to history for the copy
        TagHistoryRecord history = createTagHistoryRecord();
        history.targetId = toTargetId;
        history.tagId = -1;
        history.memberId = ownerId;
        history.action = TagHistory.ACTION_COPIED;
        history.time = new Timestamp(now);
        insert(history);
        return rows;
    }

    /**
     * Deletes all tag and history records associated with the specified target.
     */
    public void deleteTags (final int targetId)
        throws PersistenceException
    {
        // invalidate and delete tag records for this target
        CacheInvalidator inv = new CacheInvalidator() {
            public void invalidate (PersistenceContext ctx) {
                ctx.cacheTraverse(getTagClass().getName(),
                                  new PersistenceContext.CacheEvictionFilter<TagRecord>() {
                    public boolean testForEviction (Serializable key, TagRecord record) {
                        return record != null && record.targetId == targetId;
                    }
                });
            }
        };
        deleteAll(getTagClass(), new Where(getTagColumn(TagRecord.TARGET_ID), targetId), inv);

        // invalidate and delete tag history records for this target
        inv = new CacheInvalidator() {
            public void invalidate (PersistenceContext ctx) {
                ctx.cacheTraverse(getTagHistoryClass().getName(),
                                  new PersistenceContext.CacheEvictionFilter<TagHistoryRecord>() {
                    public boolean testForEviction (Serializable key, TagHistoryRecord record) {
                        return record != null && record.targetId == targetId;
                    }
                });
            }
        };
        deleteAll(getTagHistoryClass(),
                  new Where(getTagHistoryColumn(TagHistoryRecord.TARGET_ID), targetId), inv);
    }

    protected ColumnExp getTagColumn (String cname)
    {
        return new ColumnExp(getTagClass(), cname);
    }

    protected ColumnExp getTagHistoryColumn (String cname)
    {
        return new ColumnExp(getTagHistoryClass(), cname);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(createTagRecord().getClass());
        classes.add(createTagHistoryRecord().getClass());
    }

    protected Class<TagRecord> _tagClass;
    protected Class<TagHistoryRecord> _tagHistoryClass;
}
