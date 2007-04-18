//
// $Id$

package com.threerings.msoy.item.data.all {

/**
 * Represents video data.
 */
public class Video extends Item
{
    /** The video media.*/
    public var videoMedia :MediaDesc;

    public function Video ()
    {
    }

    // from Item
    override public function isConsistent () :Boolean
    {
        return super.isConsistent() && (videoMedia != null) && videoMedia.isVideo() &&
            nonBlank(name);
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getThumbnailMedia(); // TODO: support preview image
    }

    // from Item
    override public function getType () :int
    {
        return VIDEO;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        videoMedia = (ins.readObject() as MediaDesc);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(videoMedia);
    }
}
}
