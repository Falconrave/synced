//
// $Id$

package com.threerings.msoy.item.data;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.net.Transport;

/**
 * Provides the implementation of the {@link ItemService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ItemMarshaller extends InvocationMarshaller
    implements ItemService
{
    /** The method id used to dispatch {@link #deleteItem} requests. */
    public static final int DELETE_ITEM = 1;

    // from interface ItemService
    public void deleteItem (Client arg1, ItemIdent arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DELETE_ITEM, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #getCatalogId} requests. */
    public static final int GET_CATALOG_ID = 2;

    // from interface ItemService
    public void getCatalogId (Client arg1, ItemIdent arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_CATALOG_ID, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #getItemNames} requests. */
    public static final int GET_ITEM_NAMES = 3;

    // from interface ItemService
    public void getItemNames (Client arg1, ItemIdent[] arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_ITEM_NAMES, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #peepItem} requests. */
    public static final int PEEP_ITEM = 4;

    // from interface ItemService
    public void peepItem (Client arg1, ItemIdent arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, PEEP_ITEM, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #reclaimItem} requests. */
    public static final int RECLAIM_ITEM = 5;

    // from interface ItemService
    public void reclaimItem (Client arg1, ItemIdent arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, RECLAIM_ITEM, new Object[] {
            arg2, listener3
        });
    }
}
