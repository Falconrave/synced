//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;

import com.threerings.io.Streamable;

/**
 * Represents a member's rating of an item.
 */
@Entity
public abstract class RatingRecord<T extends ItemRecord> extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The column identifier for the {@link #rating} field. */
    public static final String RATING = "rating";
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The id of the tagged item. */
    @Id
    public int itemId;

    /** The id of the rating member. */
    @Id
    public int memberId;

    /** The rating, from 1 to 5 */
    public byte rating;
}
