//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.person.data.FeedMessage;
import com.threerings.msoy.person.data.GroupFeedMessage;

/**
 * Contains persistent data on a feed message distributed to a group's members.
 */
@Entity(indices={
    @Index(name="ixGroupId", fields={ GroupFeedMessageRecord.GROUP_ID })
})
public class GroupFeedMessageRecord extends FeedMessageRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #groupId} field. */
    public static final String GROUP_ID = "groupId";

    /** The qualified column identifier for the {@link #groupId} field. */
    public static final ColumnExp GROUP_ID_C =
        new ColumnExp(GroupFeedMessageRecord.class, GROUP_ID);

    /** The qualified column identifier for the {@link #messageId} field. */
    public static final ColumnExp MESSAGE_ID_C =
        new ColumnExp(GroupFeedMessageRecord.class, MESSAGE_ID);

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(GroupFeedMessageRecord.class, TYPE);

    /** The qualified column identifier for the {@link #data} field. */
    public static final ColumnExp DATA_C =
        new ColumnExp(GroupFeedMessageRecord.class, DATA);

    /** The qualified column identifier for the {@link #posted} field. */
    public static final ColumnExp POSTED_C =
        new ColumnExp(GroupFeedMessageRecord.class, POSTED);
    // AUTO-GENERATED: FIELDS END

    /** The group id of the originator of this message. */
    public int groupId;

    @Override // from FeedMessageRecord
    protected FeedMessage createMessage ()
    {
        return new GroupFeedMessage();
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GroupFeedMessageRecord}
     * with the supplied key values.
     */
    public static Key<GroupFeedMessageRecord> getKey (int messageId)
    {
        return new Key<GroupFeedMessageRecord>(
                GroupFeedMessageRecord.class,
                new String[] { MESSAGE_ID },
                new Comparable[] { messageId });
    }
    // AUTO-GENERATED: METHODS END
}
