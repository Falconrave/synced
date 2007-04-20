//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

@Entity
@Table
public abstract class CloneRecord<T extends ItemRecord> extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(CloneRecord.class, ITEM_ID);

    /** The column identifier for the {@link #originalItemId} field. */
    public static final String ORIGINAL_ITEM_ID = "originalItemId";

    /** The qualified column identifier for the {@link #originalItemId} field. */
    public static final ColumnExp ORIGINAL_ITEM_ID_C =
        new ColumnExp(CloneRecord.class, ORIGINAL_ITEM_ID);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(CloneRecord.class, OWNER_ID);

    /** The column identifier for the {@link #purchaseTime} field. */
    public static final String PURCHASE_TIME = "purchaseTime";

    /** The qualified column identifier for the {@link #purchaseTime} field. */
    public static final ColumnExp PURCHASE_TIME_C =
        new ColumnExp(CloneRecord.class, PURCHASE_TIME);

    /** The column identifier for the {@link #flowPaid} field. */
    public static final String FLOW_PAID = "flowPaid";

    /** The qualified column identifier for the {@link #flowPaid} field. */
    public static final ColumnExp FLOW_PAID_C =
        new ColumnExp(CloneRecord.class, FLOW_PAID);

    /** The column identifier for the {@link #goldPaid} field. */
    public static final String GOLD_PAID = "goldPaid";

    /** The qualified column identifier for the {@link #goldPaid} field. */
    public static final ColumnExp GOLD_PAID_C =
        new ColumnExp(CloneRecord.class, GOLD_PAID);

    /** The column identifier for the {@link #used} field. */
    public static final String USED = "used";

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(CloneRecord.class, USED);

    /** The column identifier for the {@link #location} field. */
    public static final String LOCATION = "location";

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(CloneRecord.class, LOCATION);
    // AUTO-GENERATED: FIELDS END

    public static final int BASE_SCHEMA_VERSION = 3;
    public static final int BASE_MULTIPLIER = 1000;
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION * BASE_MULTIPLIER;


    /** This clone's ID, unique relative all items of the same type. */
    @Id
    @GeneratedValue(generator="cloneId", strategy=GenerationType.TABLE)
    public int itemId;

    /** The ID of the immutable item from which this was cloned. */
    public int originalItemId;

    /** The owner of this clone. */
    public int ownerId;

    /** The time at which this clone was purchased from the catalog. */
    @Column(columnDefinition="purchaseTime DATETIME NOT NULL")
    public Timestamp purchaseTime;
    
    /** The amount of flow that was paid for this item. */
    public int flowPaid;
    
    /** The amount of gold that was paid for this item. */
    public int goldPaid;

    /** How this item is being used (see Item.USED_AS_FURNITURE). */
    public byte used;

    /** Where it's being used. */
    public int location;

}
