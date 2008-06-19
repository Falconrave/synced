//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

/**
 * Information pertaining to a player's IM connections.
 */
public class GatewayEntry
    implements Comparable, DSet.Entry, IsSerializable
{
    /** The gateway name. */
    public String gateway;

    /** Is the user logged into the gateway? */
    public boolean online;

    /** The username being used to connect to the gateway. */
    public String username;

    /** For serialization. */
    public GatewayEntry ()
    {
    }

    public GatewayEntry (String gateway)
    {
        this(gateway, false, null);
    }

    public GatewayEntry (String gateway, boolean online, String username)
    {
        this.gateway = gateway;
        this.online = online;
        this.username = username;
    }

    // from interface DSet.Entry
    public Comparable getKey ()
    {
        return gateway;
    }

    // from interface Comparable
    public int compareTo (Object other)
    {
        GatewayEntry that = (GatewayEntry)other;
        // online connections shown first
        if (this.online != that.online) {
            return this.online ? -1 : 1;
        }
        // then sort by gateway
        return this.gateway.compareTo(that.gateway);
    }

    @Override // from Object
    public int hashCode ()
    {
        return gateway.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof GatewayEntry) && gateway.equals(((GatewayEntry)other).gateway);
    }

    @Override
    public String toString ()
    {
        return "GatewayEntry[" + gateway + "]";
    }
}
