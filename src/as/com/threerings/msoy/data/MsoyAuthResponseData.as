//
// $Id$

package com.threerings.msoy.data {

import com.threerings.presents.net.AuthResponseData;

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;

/**
 * Extends the normal auth response data with MSOY-specific bits.
 */
public class MsoyAuthResponseData extends AuthResponseData
{
    /** The session token assigned to this user, or null. */
    public var sessionToken :String;

    /** A machine identifier to be assigned to this machine, or null. */
    public var ident :String;

//    override public function writeObject (out :ObjectOutputStream) :void
//    {
//        super.writeObject(out);
//
//        out.writeField(sessionToken);
//        out.writeField(ident);
//    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        sessionToken = (ins.readField(String) as String);
        ident = (ins.readField(String) as String);
    }
}
}
