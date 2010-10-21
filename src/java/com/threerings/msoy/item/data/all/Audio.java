//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * Represents audio data.
 */
public class Audio extends Item
{
    /** The audio media.*/
    public MediaDesc audioMedia;

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.AUDIO;
    }

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (audioMedia != null) && audioMedia.isAudio() &&
            nonBlank(name, MAX_NAME_LENGTH);
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return audioMedia;
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return audioMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        audioMedia = desc;
    }
}
