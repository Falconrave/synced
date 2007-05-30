//
// $Id$

package com.threerings.msoy.item.server;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.ItemMarshaller;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link ItemProvider}.
 */
public class ItemDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public ItemDispatcher (ItemProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new ItemMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case ItemMarshaller.GET_INVENTORY:
            ((ItemProvider)provider).getInventory(
                source,
                ((Byte)args[0]).byteValue(), (InvocationService.InvocationListener)args[1]
            );
            return;

        case ItemMarshaller.RECLAIM_ITEM:
            ((ItemProvider)provider).reclaimItem(
                source,
                (ItemIdent)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
