//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Represents an uploaded avatar.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="AVATAR")
public class AvatarRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #avatarMediaHash} field. */
    public static final String AVATAR_MEDIA_HASH = "avatarMediaHash";

    /** The qualified column identifier for the {@link #avatarMediaHash} field. */
    public static final ColumnExp AVATAR_MEDIA_HASH_C =
        new ColumnExp(AvatarRecord.class, AVATAR_MEDIA_HASH);

    /** The column identifier for the {@link #avatarMimeType} field. */
    public static final String AVATAR_MIME_TYPE = "avatarMimeType";

    /** The qualified column identifier for the {@link #avatarMimeType} field. */
    public static final ColumnExp AVATAR_MIME_TYPE_C =
        new ColumnExp(AvatarRecord.class, AVATAR_MIME_TYPE);

    /** The column identifier for the {@link #scale} field. */
    public static final String SCALE = "scale";

    /** The qualified column identifier for the {@link #scale} field. */
    public static final ColumnExp SCALE_C =
        new ColumnExp(AvatarRecord.class, SCALE);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the avatar media. */
    public byte[] avatarMediaHash;

    /** The MIME type of the {@link #avatarMediaHash} media. */
    public byte avatarMimeType;

    /** The scaling to apply to the avatar. */
    @Column(defaultValue="1")
    public float scale;

    public AvatarRecord ()
    {
        super();
    }

    protected AvatarRecord (Avatar avatar)
    {
        super(avatar);

        if (avatar.avatarMedia != null) {
            avatarMediaHash = avatar.avatarMedia.hash;
            avatarMimeType = avatar.avatarMedia.mimeType;
        }
        scale = avatar.scale;
    }

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.AVATAR;
    }

    @Override // from ItemRecord
    public void initFromClone (CloneRecord clone)
    {
        super.initFromClone(clone);

        this.scale = ((AvatarCloneRecord) clone).scale;
    }

    @Override
    protected Item createItem ()
    {
        Avatar object = new Avatar();
        object.avatarMedia = avatarMediaHash == null ? null :
            new MediaDesc(avatarMediaHash, avatarMimeType);
        object.scale = scale;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #AvatarRecord}
     * with the supplied key values.
     */
    public static Key<AvatarRecord> getKey (int itemId)
    {
        return new Key<AvatarRecord>(
                AvatarRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
