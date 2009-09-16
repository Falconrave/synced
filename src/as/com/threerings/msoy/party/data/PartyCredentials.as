//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.util.Name;

import com.threerings.msoy.data.MsoyCredentials;

/**
 * Used to authenticate a party session.
 */
public class PartyCredentials extends MsoyCredentials
{
    /** The party that the authenticating user wishes to join. */
    public var partyId :int;

    public function PartyCredentials (username :Name)
    {
        super(username);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(partyId);
    }
}
}
