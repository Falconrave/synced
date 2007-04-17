//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** The photo media. */
    public MediaDesc photoMedia;

    /** The width (in pixels) of the photo media. */
    public int photoWidth;

    /** The height (in pixels) of the photo media. */
    public int photoHeight;

    // @Override // from Item
    public byte getType ()
    {
        return PHOTO;
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (photoMedia != null);
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return photoMedia;
    }

    // @Override // from Item
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        if (photoMedia != null && photoMedia.isImage()) {
            return photoMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return photoMedia;
    }
}
