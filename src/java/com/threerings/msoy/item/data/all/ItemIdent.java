//
// $Id$

package com.threerings.msoy.item.data.all;

import com.google.common.collect.ComparisonChain;
import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * A fully qualified item identifier (type and integer id).
 */
public class ItemIdent
    implements Comparable<ItemIdent>, Streamable, IsSerializable
{
    /** The item type identifier. */
    public MsoyItemType type;

    /** The integer identifier of the item. */
    public int itemId;

    /**
     * A constructor used for unserialization.
     */
    public ItemIdent ()
    {
    }

    /**
     * Creates an identifier for the specified item.
     */
    public ItemIdent (MsoyItemType type, int itemId)
    {
        this.type = type;
        this.itemId = itemId;
    }

    @Override
    public int compareTo (ItemIdent o)
    {
        return ComparisonChain.start()
            .compare(type, o.type)
            .compare(itemId, o.itemId)
            .result();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof ItemIdent) {
            ItemIdent that = (ItemIdent) other;
            return (this.type == that.type) && (this.itemId == that.itemId);
        }
        return false;
    }

    @Override // from Object
    public int hashCode ()
    {
        return (type.toByte() * 37) | itemId;
    }

    /**
     * Generates a string representation of this instance.
     */
    public String toString ()
    {
        return type + ":" + itemId;
    }
}
