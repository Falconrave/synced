//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Prop;

/**
 * Contains the persistent data for a Prop item.
 */
@TableGenerator(name="itemId", pkColumnValue="PROP")
public class PropRecord extends IdentGameItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PropRecord> _R = PropRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp IDENT = colexp(_R, "ident");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING_SUM = colexp(_R, "ratingSum");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp MATURE = colexp(_R, "mature");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
    // AUTO-GENERATED: FIELDS END

    /** Update this version if you change fields specific to this derived class. */
    public static final int ITEM_VERSION = 3;

    /** This combines {@link #ITEM_VERSION} with {@link #BASE_SCHEMA_VERSION} to create a version
     * that allows us to make ItemRecord-wide changes and specific derived class changes. */
    public static final int SCHEMA_VERSION = ITEM_VERSION + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.PROP;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        // Prop temp = (Prop)item;
        // nothing to do
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Prop object = new Prop();
        // nothing to do
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PropRecord}
     * with the supplied key values.
     */
    public static Key<PropRecord> getKey (int itemId)
    {
        return new Key<PropRecord>(
                PropRecord.class,
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
