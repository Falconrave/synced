//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.web.client.DeploymentConfig;

/**
 * Provides a "faked" media descriptor for static media (default thumbnails and
 * furni representations).
 */
public class StaticMediaDesc extends MediaDesc
{
    /** Used for unserialization. */
    public StaticMediaDesc ()
    {
    }

    /**
     * Creates a configured static media descriptor.
     */
    public StaticMediaDesc (byte mimeType, byte itemType, String mediaType)
    {
        this(mimeType, itemType, mediaType, NOT_CONSTRAINED);
    }

    /**
     * Creates a configured static media descriptor.
     */
    public StaticMediaDesc (byte mimeType, byte itemType, String mediaType, byte constraint)
    {
        super(null, mimeType);
        _itemType = itemType;
        _mediaType = mediaType;
        this.constraint = constraint;
    }

    /**
     * Returns the type of item for which we're providing static media.
     */
    public byte getItemType ()
    {
        return _itemType;
    }

    /**
     * Returns the media type for which we're obtaining the static default. For example {@link
     * Item#MAIN_MEDIA}.
     */
    public String getMediaType ()
    {
        return _mediaType;
    }

    @Override // from MediaDesc
    public String getMediaPath ()
    {
        return DeploymentConfig.staticMediaURL +
            Item.getTypeName(_itemType) + "/" + _mediaType + mimeTypeToSuffix(mimeType);
    }

    protected byte _itemType;
    protected String _mediaType;
}
