//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Exetnds MemberName with a profile photo.
 */
public class VizMemberName extends MemberName
{
    /**
     * Returns this member's photo.
     */
    public function getPhoto () :MediaDesc
    {
        return _photo;
    }

    // from OccupantInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _photo = (ins.readObject() as MediaDesc);
    }

    // from OccupantInfo
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeObject(_photo);
    }

    /** This member's profile photo. */
    protected var _photo :MediaDesc;
}
}
