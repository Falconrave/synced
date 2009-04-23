//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.parlor.data.TableConfig;

/**
 * A table configuration with Msoy-specific extras.
 */
public class MsoyTableConfig extends TableConfig
{
    /** The display text for this table. */
    public var title :String;

    /** The required partyId for joining this table, or 0. */
    public var partyId :int;

    // from Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        title = ins.readField(String) as String;
        partyId = ins.readInt();
    }

    // from Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(title);
        out.writeInt(partyId);
    }
}
}
