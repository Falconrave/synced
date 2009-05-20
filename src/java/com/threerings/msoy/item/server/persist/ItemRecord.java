//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.annotation.Transient;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;

/**
 * The base class for all digital items in the MSOY system.
 */
@Entity(fullTextIndices={
    @FullTextIndex(name=ItemRecord.FTS_ND, fields={ "name", "description" })
})
public abstract class ItemRecord extends PersistentRecord implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ItemRecord> _R = ItemRecord.class;
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

    /** The identifier for the full text search index on Name, Description */
    public static final String FTS_ND = "ND";

    /** This is the version of the base class persistent data. It is combined with ITEM_VERSION to
     * create the actual version for each ItemRecord derived class. */
    public static final int BASE_SCHEMA_VERSION = 21;

    /** This is used to combine {@link #BASE_SCHEMA_VERSION} with ITEM_VERSION to create our
     * per-record SCHEMA_VERSION. */
    public static final int BASE_MULTIPLIER = 1000;

    /** A function for converting this persistent record into a runtime record. */
    public static class ToItem<T extends Item> implements Function<ItemRecord, T> {
        public T apply (ItemRecord record) {
            @SuppressWarnings("unchecked") T item = (T) record.toItem();
            return item;
        }
    }

    /** This item's unique identifier. <em>Note:</em> this identifier is not globally unique among
     * all digital items. Each type of item has its own identifier space. */
    @Id
    @GeneratedValue(generator="itemId", strategy=GenerationType.TABLE, allocationSize=1)
    public int itemId;

    /** The item ID from which this object was cloned, or 0 if this is not a clone. This field is
     * not persisted to the database, but set when we load a clone. */
    @Computed(required=false)
    public int sourceId = 0;

    /** A bit-mask of runtime attributes about this item. These are not saved in the database
     * anywhere, these are created on-the-fly when looking at metadata not otherwise sent
     * down from the server. */
    @Transient
    public byte attrs;

    /** The member id of the member that created this item. */
    @Index(name="ixCreator")
    public int creatorId;

    /** The member id of the member that owns this item, or 0 if it's not in any inventory;
     * e.g. it's listed in the catalog or a gifted item in a mail message. */
    @Index(name="ixOwner")
    public int ownerId;

    /** The catalog listing associated with this item. Set to 0 if this item is not listed. If
     * nonzero, the item is either:
     * the original item (sourceId == 0 && ownerId != 0),
     * the master item (sourceId == 0 && ownerId == 0),
     * or just a purchased item (sourceId != 0).
     *
     * See {@link #isListedOriginal} and {@link #isCatalogMaster}.
     */
    public int catalogId;

    /** The current sum of all ratings that have been applied to this item. */
    public int ratingSum;

    /** The number of user ratings that went into the average rating. */
    public int ratingCount;

    /** How this item is being used. */
    public Item.UsedAs used = Item.UsedAs.NOTHING;

    /** Where it's being used. */
    @Index(name="locationIndex")
    public int location;

    /** The timestamp at which this item was last touched. */
    public Timestamp lastTouched;

    /** A user supplied name for this item. */
    public String name;

    /** A user supplied description for this item. */
    @Column(length=Item.MAX_DESCRIPTION_LENGTH)
    public String description;

    /** Whether or not this item represents mature content. */
    public boolean mature;

    /** A hash code identifying the media used to display this item's thumbnail representation. */
    @Column(nullable=true)
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    public byte thumbMimeType;

    /** The size constraint on the {@link #thumbMediaHash} media. */
    public byte thumbConstraint;

    /** A hash code identifying the media used to display this item's furniture representation. */
    @Column(nullable=true)
    public byte[] furniMediaHash;

    /** The MIME type of the {@link #furniMediaHash} media. */
    public byte furniMimeType;

    /** The size constraint on the {@link #furniMediaHash} media. */
    public byte furniConstraint;

    /**
     * This is used to map {@link ItemRecord} concrete classes to item type
     * values.
     */
    public abstract byte getType ();

    @Override
    public int hashCode ()
    {
        return itemId;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof ItemRecord) {
            ItemRecord that = (ItemRecord) other;
            return (itemId == that.itemId) && (getType() == that.getType());
        }
        return false;
    }

    /**
     * Returns true if this item is an original item that has been listed in the catalog and now
     * serves as the work-in-progress item from which the litsing is generated.
     */
    public boolean isListedOriginal ()
    {
        return (sourceId == 0) && (catalogId != 0) && (ownerId != 0);
    }

    /**
     * Returns true if this item is a catalog master from which clones are configured,
     * rather than just a clone, or the original item.
     */
    public boolean isCatalogMaster ()
    {
        return (sourceId == 0) && (catalogId != 0) && (ownerId == 0);
    }

    /**
     * Clears out any fields that should be reset when listing this item in the catalog.
     *
     * @param oldListing the previous catalog master item if this item has already been listed,
     * or null if it is being listed for the first time.
     */
    public void prepareForListing (ItemRecord oldListing)
    {
        itemId = 0;
        ownerId = 0;
        used = Item.UsedAs.NOTHING;
        location = 0;

        if (oldListing != null) {
            // we're going to replace the old catalog listing item
            itemId = oldListing.itemId;
            // inherit the average rating from the old item
            ratingSum = oldListing.ratingSum;
            ratingCount = oldListing.ratingCount;
            // if the old listing was mature or the new item is mature we want the new listing to
            // be mature as well; there's no going back without listing anew
            mature = oldListing.mature || mature;
        }
    }

    // TODO: this is dormant. See ItemServlet.remixItem.
//    /**
//     * Clears out any fields that should be reset when remixing an item.
//     */
//    public void prepareForRemixing ()
//    {
//        itemId = 0;
//        sourceId = 0;
//        creatorId = ownerId; // TODO: preserve creator?
//        used = Item.UNUSED;
//        location = 0;
//    }

    /**
     * Initialize this record so that it actually represents the specified clone.
     */
    public void initFromClone (CloneRecord clone)
    {
        // use the clone's override name, if present
        if (clone.name != null) {
            this.name = clone.name;
        }

        // use the clone's media override (remix media)
        if (clone.mediaHash != null) {
            setPrimaryMedia(clone.mediaHash);
            this.attrs |= Item.ATTR_REMIXED_CLONE;
            if (clone.mediaStamp.before(this.lastTouched)) {
                this.attrs |= Item.ATTR_ORIGINAL_UPDATED;
            }
        }

        // copy our itemId to source, and take the clone's itemId
        this.sourceId = this.itemId;
        this.itemId = clone.itemId;

        // we now keep our catalogId set to the correct value

        this.ownerId = clone.ownerId;
        this.used = clone.used;
        this.location = clone.location;
        this.lastTouched = clone.lastTouched;
    }

    /**
     * Build a POJO version of this Record, for use outside the persistence system.
     */
    public Item toItem ()
    {
        Item item = createItem();
        item.itemId = itemId;
        item.sourceId = sourceId;
        item.ownerId = ownerId;
        item.catalogId = catalogId;
        item.ratingSum = ratingSum;
        item.ratingCount = ratingCount;
        item.used = used;
        item.location = location;
        item.lastTouched = lastTouched.getTime();
        item.name = name;
        item.description = description;
        item.creatorId = creatorId;
        item.attrs = attrs;
        item.mature = mature;
        if (furniMediaHash != null) {
            item.setFurniMedia(new MediaDesc(furniMediaHash, furniMimeType, furniConstraint));
        }
        if (thumbMediaHash != null) {
            item.setThumbnailMedia(new MediaDesc(thumbMediaHash, thumbMimeType, thumbConstraint));
        }
        return item;
    }

    /**
     * Initializes this persistent record from the supplied runtime record. Only fields that are
     * user editable should be filled in.
     */
    public void fromItem (Item item)
    {
        // itemId = not user editable
        // sourceId = not user editable
        // ownerId = not user editable
        // catalogId = not user editable
        // rating = not user editable
        // ratingCount = not user editable
        // creatorId = not user editable
        // flagged = not user editable
        // attrs = not user editable
        // mature = not user editable
        // used = not user editable
        // location = not user editable
        // lastTouched = not user editable
        name = (item.name == null) ? "" : item.name;
        description = (item.description == null) ? "" : item.description;
        MediaDesc media = item.getRawThumbnailMedia();
        if (media != null) {
            thumbMediaHash = media.hash;
            thumbMimeType = media.mimeType;
            thumbConstraint = media.constraint;
        } else {
            thumbMediaHash = null;
        }
        media = item.getRawFurniMedia();
        if (media != null) {
            furniMediaHash = media.hash;
            furniMimeType = media.mimeType;
            furniConstraint = media.constraint;
        } else {
            furniMediaHash = null;
        }
    }

    @Override // from Object
    public String toString ()
    {
        return getClass().getSimpleName() + " " + itemId;
    }

    /**
     * Returns the MediaDesc for the thumbnail media of this item, or the default if the item has
     * no thumbnail media.
     */
    public MediaDesc getThumbMediaDesc ()
    {
        return (thumbMediaHash == null) ? Item.getDefaultThumbnailMediaFor(getType()) :
            new MediaDesc(thumbMediaHash, thumbMimeType, thumbConstraint);
    }

    /**
     * Return true if the primary media is remixable.
     */
    public boolean isRemixable ()
    {
        return MediaDesc.isRemixable(getPrimaryMimeType());
    }

    /**
     * Used when comparing against a clone record.
     */
    public byte[] getPrimaryMedia ()
    {
        return furniMediaHash;
    }

    /**
     * Get the mimeType of the primary media.
     */
    protected byte getPrimaryMimeType ()
    {
        return furniMimeType;
    }

    /**
     * Set the specified hash as the primary media.
     */
    protected void setPrimaryMedia (byte[] hash)
    {
        furniMediaHash = hash;
    }

    /** 
     * Calculate this item's average rating from the sum and count.
     */
    public float getRating ()
    {
        return (ratingCount > 0) ? (float) ratingSum / ratingCount : 0f;
    }

    /**
     * Creates an instance of the appropriate item runtime object and initializes the derived
     * class's custom fields. Used by {@link #toItem}.
     */
    protected abstract Item createItem ();
}
