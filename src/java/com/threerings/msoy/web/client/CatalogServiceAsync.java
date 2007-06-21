//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link CatalogService}.
 */
public interface CatalogServiceAsync
{
    /**
     * The asynchronous version of {@link CatalogService#loadCatalog}.
     */
    public void loadCatalog (int memberId, byte type, byte sortBy, String search, String tag,
                             int creator, int offset, int rows, boolean includeCount,
                             AsyncCallback callback);
    
    /**
     *  The asynchronous version of {@link CatalogService#purchaseItem}
     */
    public void purchaseItem (WebIdent ident, ItemIdent item, AsyncCallback callback);
    
    /**
     *  The asynchronous version of {@link CatalogService#listItem}
     */
    public void listItem (WebIdent ident, ItemIdent item, String descrip, int rarity, boolean list,
                          AsyncCallback callback);

    /**
     *  The asynchronous version of {@link CatalogService#returnItem}
     */
    public void returnItem (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link CatalogService#getPopularTags}.
     */
    public void getPopularTags (byte type, int count, AsyncCallback callback);
}
