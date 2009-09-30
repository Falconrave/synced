//
// $Id$

package com.threerings.msoy.stuff.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * The asynchronous (client-side) version of {@link StuffService}.
 */
public interface StuffServiceAsync
{
    /**
     * Publish the specified xml string as 'external media'.
     */
    void publishExternalMedia (String data, byte mimeType, AsyncCallback<MediaDesc> callback);

    /**
     * The asynchronous version of {@link StuffService#createItem}.
     */
    void createItem (Item item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#updateItem}.
     */
    void updateItem (Item item, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link StuffService#remixItem}.
     */
    void remixItem (Item item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#revertRemixedClone}.
     */
    void revertRemixedClone (ItemIdent itemIdent, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#renameClone}.
     */
    void renameClone (ItemIdent itemIdent, String name, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link StuffService#loadInventory}.
     */
    void loadInventory (int memberId, byte type, String query, int themeId,
        AsyncCallback<List<Item>> callback);

    /**
     * Loads the details of a particular item.
     */
    void loadItem (ItemIdent item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#loadItemDetail}.
     */
    void loadItemDetail (ItemIdent item, AsyncCallback<StuffService.DetailOrIdent> callback);

    /**
     * The asynchronous version of {@link StuffService#deleteItem}.
     */
    void deleteItem (ItemIdent item, AsyncCallback<Void> callback);
}
