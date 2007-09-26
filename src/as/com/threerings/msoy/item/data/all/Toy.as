//
// $Id: Toy.as 4826 2007-06-20 20:07:25Z mdb $

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents an interactive piece of furniture. Something that lives permanently in a room but
 * which is interactive in some way.
 */
public class Toy extends Item
{
    public function Toy ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getFurniMedia();
    }

    // from Item
    override public function getType () :int
    {
        return TOY;
    }

    // from Item
    override protected function getDefaultThumbnailMedia () :MediaDesc
    {
        if (furniMedia != null && furniMedia.isImage()) {
            return furniMedia;
        }
        return super.getDefaultThumbnailMedia();
    }

    // from Item
    override protected function getDefaultFurniMedia () :MediaDesc
    {
        return null; // there is no default
    }
}
}
