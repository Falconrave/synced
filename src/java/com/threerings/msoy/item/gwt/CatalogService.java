//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.CatalogListing.DerivedItem;

/**
 * Provides digital items related services.
 */
public interface CatalogService extends RemoteService
{
    /** Provides results for {@link #loadCatalog}. */
    public static class CatalogResult
        implements IsSerializable
    {
        /** The total count of listings matching the query. */
        public int listingCount;

        /** The particular set of listings requested. */
        public List<ListingCard> listings;
    }

    /** Provides results for {@link #loadFavorites}. */
    public static class FavoritesResult
        implements IsSerializable
    {
        /** The member for whom we're returning favorites. */
        public MemberName noter;

        /** The listing information for said favorites. */
        public List<ListingCard> favorites;
    }

    /** Returned by {@link #loadSuite}. */
    public static class SuiteResult
        implements IsSerializable
    {
        /** The name of the suite. For game suites, this is the game's name. */
        public String name;

        /** The id of the suite in question. */
        public int suiteId;

        /** The id of the creator of the items in this suite. */
        public int creatorId;

        /** The tag that identifies non-sub-items in this suite. */
        public String suiteTag;

        /** The listings of items in this suite. */
        public List<ListingCard> listings;
    }

    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/catalogsvc";

    /**
     * Loads the featured items shown on the top-level catalog page.
     */
    ShopData loadShopData ()
        throws ServiceException;

    /**
     * Loads all catalogue items of the specified type.
     *
     * @param includeCount if true, the count of all listings matching the query terms will also be
     * computed and included in the result.
     */
    CatalogResult loadCatalog (CatalogQuery query, int offset, int rows, boolean includeCount)
        throws ServiceException;

    /**
     * Purchases the item of the specified id and type.
     */
    PurchaseResult<Item> purchaseItem (
        byte itemType, int catalogId, Currency currency, int authedCost, String memories)
        throws ServiceException;

    /**
     * Lists the specified item in the catalog.
     *
     * @return the catalog id of the newly listed item.
     */
    int listItem (ItemIdent item, byte rating, int pricing, int salesTarget,
                  Currency currency, int cost, int basisCatalogId)
        throws ServiceException;

    /**
     * Loads and returns the specified catalog listing. If <code>forDisplay</code> is set, some
     * additional listing fields are filled in that are only needed for display.
     */
    CatalogListing loadListing (byte itemType, int catalogId, boolean forDisplay)
        throws ServiceException;

    /**
     * Loads all derived items for a given item. The user interface shows a short list when the
     * listing is loaded, then this may be called at the user's request.
     */
    DerivedItem[] loadAllDerivedItems (byte itemType, int catalogId)
        throws ServiceException;

    /**
     * Updates the catalog listing associated with the supplied catalog original.
     */
    void updateListing (ItemIdent item)
        throws ServiceException;

    /**
     * Updates the specified catalog listing's price.
     */
    void updatePricing (byte itemType, int catalogId, int pricing, int salesTarget,
                        Currency currency, int cost)
        throws ServiceException;

    /**
     * Removes the specified catalog listing.
     */
    void removeListing (byte itemType, int catalogId)
        throws ServiceException;

    /**
     * Fetches the N most-used tags for a given item type.
     */
    Map<String, Integer> getPopularTags (byte type, int rows)
        throws ServiceException;

    /**
     * Loads up the favorite items of the specified member of the specified type.
     */
    FavoritesResult loadFavorites (int memberId, byte itemType)
        throws ServiceException;

    /**
     * Using the authenticated member's favorites, returns the subset that are suitable for use as
     * a basis for the given item type. It's not possible for this method to filter based on the
     * cost of the items, the caller should use {@link CatalogListing#getMinimumDerivedCost()}.
     */
    List<ListingCard> loadPotentialBasisItems (byte itemType)
        throws ServiceException;

    /**
     * Loads the specified suite.
     */
    SuiteResult loadSuite (byte itemType, int suiteId)
        throws ServiceException;
}
