//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** The photo media. This is the full-size version. furniMedia is 4x thumbnail size. */
    public MediaDesc photoMedia;

    /** The width (in pixels) of the photo media. */
    public int photoWidth;

    /** The height (in pixels) of the photo media. */
    public int photoHeight;

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.PHOTO;
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (photoMedia != null) && (photoMedia.isImage());
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return (_furniMedia != null) ? _furniMedia : getThumbnailMedia();
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return photoMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        photoMedia = desc;
    }
}
