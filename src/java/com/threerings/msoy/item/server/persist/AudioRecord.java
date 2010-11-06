//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.server.MediaDescFactory;

/**
 * Represents an uploaded piece of audio.
 */
@TableGenerator(name="itemId", pkColumnValue="AUDIO")
public class AudioRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AudioRecord> _R = AudioRecord.class;
    public static final ColumnExp AUDIO_MEDIA_HASH = colexp(_R, "audioMediaHash");
    public static final ColumnExp AUDIO_MIME_TYPE = colexp(_R, "audioMimeType");
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
    public static final int ITEM_VERSION = 1;

    /** This combines {@link #ITEM_VERSION} with {@link #BASE_SCHEMA_VERSION} to create a version
     * that allows us to make ItemRecord-wide changes and specific derived class changes. */
    public static final int SCHEMA_VERSION = ITEM_VERSION + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the audio media. */
    public byte[] audioMediaHash;

    /** The MIME type of the {@link #audioMediaHash} media. */
    public byte audioMimeType;

    @Override // from ItemRecord
    public MsoyItemType getType ()
    {
        return MsoyItemType.AUDIO;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Audio audio = (Audio)item;
        if (audio.audioMedia != null) {
            audioMediaHash = HashMediaDesc.unmakeHash(audio.audioMedia);
            audioMimeType = audio.audioMedia.getMimeType();
        } else {
            audioMediaHash = null;
        }
    }

    @Override // from ItemRecord
    public byte[] getPrimaryMedia ()
    {
        return audioMediaHash;
    }

    @Override // from ItemRecord
    protected byte getPrimaryMimeType ()
    {
        return audioMimeType;
    }

    @Override // from ItemRecord
    protected void setPrimaryMedia (byte[] hash)
    {
        audioMediaHash = hash;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Audio object = new Audio();
        object.audioMedia = audioMediaHash == null ? null :
            MediaDescFactory.createMediaDesc(audioMediaHash, audioMimeType);
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AudioRecord}
     * with the supplied key values.
     */
    public static Key<AudioRecord> getKey (int itemId)
    {
        return newKey(_R, itemId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ITEM_ID); }
    // AUTO-GENERATED: METHODS END
}
