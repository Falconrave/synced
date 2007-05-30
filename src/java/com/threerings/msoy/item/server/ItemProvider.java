//
// $Id$

package com.threerings.msoy.item.server;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link ItemService}.
 */
public interface ItemProvider extends InvocationProvider
{
    /**
     * Handles a {@link ItemService#getInventory} request.
     */
    public void getInventory (ClientObject caller, byte arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#reclaimItem} request.
     */
    public void reclaimItem (ClientObject caller, ItemIdent arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
