//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.data.all.Photo;

/**
 * Represents an uploaded photograph for display in albums or for use as a
 * profile picture.
 */
@TableGenerator(name="itemId", pkColumnValue="PHOTO")
public class PhotoRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<PhotoRecord> _R = PhotoRecord.class;
    public static final ColumnExp PHOTO_MEDIA_HASH = colexp(_R, "photoMediaHash");
    public static final ColumnExp PHOTO_MIME_TYPE = colexp(_R, "photoMimeType");
    public static final ColumnExp PHOTO_CONSTRAINT = colexp(_R, "photoConstraint");
    public static final ColumnExp PHOTO_WIDTH = colexp(_R, "photoWidth");
    public static final ColumnExp PHOTO_HEIGHT = colexp(_R, "photoHeight");
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

    /** A hash code identifying the photo media. */
    public byte[] photoMediaHash;

    /** The MIME type of the {@link #photoMediaHash} media. */
    public byte photoMimeType;

    /** The size constraint on the {@link #photoMediaHash} media. */
    public byte photoConstraint;

    /** The width (in pixels) of the main photo media. */
    public int photoWidth;

    /** The height (in pixels) of the main photo media. */
    public int photoHeight;

    @Override // from ItemRecord
    public MsoyItemType getType ()
    {
        return MsoyItemType.PHOTO;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Photo photo = (Photo)item;
        if (photo.photoMedia != null) {
            photoMediaHash = photo.photoMedia.hash;
            photoMimeType = photo.photoMedia.mimeType;
            photoConstraint = photo.photoMedia.constraint;
        } else {
            photoMediaHash = null;
        }
        photoWidth = photo.photoWidth;
        photoHeight = photo.photoHeight;
    }

    @Override // from ItemRecord
    public byte[] getPrimaryMedia ()
    {
        return photoMediaHash;
    }

    @Override // from ItemRecord
    protected byte getPrimaryMimeType ()
    {
        return photoMimeType;
    }

    @Override // from ItemRecord
    protected void setPrimaryMedia (byte[] hash)
    {
        photoMediaHash = hash;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Photo object = new Photo();
        object.photoMedia = (photoMediaHash == null) ?
            null : new MediaDesc(photoMediaHash, photoMimeType, photoConstraint);
        object.photoWidth = photoWidth;
        object.photoHeight = photoHeight;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PhotoRecord}
     * with the supplied key values.
     */
    public static Key<PhotoRecord> getKey (int itemId)
    {
        return newKey(_R, itemId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ITEM_ID); }
    // AUTO-GENERATED: METHODS END
}
