//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Represents audio data.
 */
public class Audio extends Item
{
    /** The audio media.*/
    public var audioMedia :MediaDesc;

    public function Audio ()
    {
    }

    // from Item
    override public function getPreviewMedia () :MediaDesc
    {
        return getThumbnailMedia(); // TODO: support album art?
    }

    // from Item
    override public function isConsistent () :Boolean
    {
        return super.isConsistent() && (audioMedia != null) && audioMedia.isAudio() &&
            nonBlank(name);
    }

    // from Item
    override public function getType () :int
    {
        return AUDIO;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        audioMedia = (ins.readObject() as MediaDesc);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(audioMedia);
    }
}
}
