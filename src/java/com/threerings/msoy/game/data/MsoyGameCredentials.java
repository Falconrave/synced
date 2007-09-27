//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.Name;

import com.threerings.presents.net.Credentials;

/**
 * Used to authenticate with an MSOY Game server.
 */
public class MsoyGameCredentials extends Credentials
{
    /** A session token that identifies this user. */
    public String sessionToken;

    public MsoyGameCredentials ()
    {
        // default to no name; member's will have their name filled in on the server; for guests we
        // may preserve their randomly assigned name with a later call to setUsername()
        super(new Name(""));
    }

    @Override // from Credentials
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append("token=").append(sessionToken);
    }
}
