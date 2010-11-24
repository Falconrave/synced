//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.presents.dobj.DSet;

public class PlayerEntry
    implements IsSerializable, DSet.Entry
{
    /** The display name of the friend. */
    public VizMemberName name;

    /** Suitable for deserialization. */
    public PlayerEntry ()
    {
    }

    public PlayerEntry (VizMemberName name)
    {
        this.name = name;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return this.name.getKey();
    }

    @Override // from Object
    public int hashCode ()
    {
        return this.name.hashCode();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof PlayerEntry) &&
            (this.name.getId() == ((PlayerEntry)other).name.getId());
    }

    @Override
    public String toString ()
    {
        return "PlayerEntry[" + name + "]";
    }
}
