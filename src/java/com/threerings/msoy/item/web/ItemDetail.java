//
// $Id$

package com.threerings.msoy.item.web;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.msoy.web.data.MemberGName;

/**
 * This class supplies detailed information for an item, some of which is
 * relative to a given member.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly
 * limited in their contents as they must be translatable into JavaScript
 * ({@link IsSerializable}) and must work with the Presents streaming system
 * ({@link Streamable}).
 */
public class ItemDetail implements Streamable, IsSerializable
{
    /** The Item of which we're a Detail. */
    public Item item;
    
    /** A display-friendly expansion of Item.creatorId. */
    public MemberGName creator; 
    
    /** A display-friendly expansion of Item.ownerId, or null. */
    public MemberGName owner;

    /** The item's rating given by the member specified in the request. */
    public byte memberRating;
}
