//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.samskivert.Log;
import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntListUtil;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext.CacheEvictionFilter;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagRepository;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.CatalogListing;

import static com.threerings.msoy.Log.log;

/**
 * Manages a repository of digital items of a particular type.
 */
public abstract class ItemRepository<
    T extends ItemRecord,
    CLT extends CloneRecord<T>,
    CAT extends CatalogRecord<T>,
    RT extends RatingRecord<T>>
    extends DepotRepository
{
    @Computed @Entity
    public static class RatingAverageRecord extends PersistentRecord {
        public int count;
        public int sum;
    }

    /** The factor by which we split item cost into gold and flow. */
    public final static int FLOW_FOR_GOLD = 600;

    public ItemRepository (ConnectionProvider provider)
    {
        // we'll be using one persistence context per ItemRecord type
        super(new PersistenceContext("itemdb", provider));
        _tagRepo = new TagRepository(_ctx) {
            protected TagRecord createTagRecord () {
                return ItemRepository.this.createTagRecord();
            }
            protected TagHistoryRecord createTagHistoryRecord () {
                return ItemRepository.this.createTagHistoryRecord();
            }
        };

        // TIMESTAMP -> DATETIME
        _ctx.registerMigration(getCatalogClass(), new EntityMigration.Retype(4, "listedDate"));
    }

    /**
     * Returns the repository that manages tags for this item.
     */
    public TagRepository getTagRepository ()
    {
        return _tagRepo;
    }

    /**
     * Load an item, or a clone.
     */
    public T loadItem (int itemId)
        throws PersistenceException
    {
        // TODO: This will only work for the first two billion clones.
        return itemId > 0 ? loadOriginalItem(itemId) : loadClone(itemId);
    }

    /**
     * Loads an item with the specified identifier. Returns null if no item
     * exists with that identifier.
     */
    public T loadOriginalItem (int itemId)
        throws PersistenceException
    {
        return load(getItemClass(), itemId);
    }

    /**
     * Loads the clone with the given identifier. Returns null if no clone
     * exists with that identifier.
     */
    public T loadClone (int cloneId) throws PersistenceException
    {
        CLT cloneRecord = load(getCloneClass(), cloneId);
        if (cloneRecord == null) {
            return null;
        }

        T clone = loadOriginalItem(cloneRecord.originalItemId);
        if (clone == null) {
            throw new PersistenceException(
                "Clone's original does not exist [cloneId=" + cloneId +
                ", originalItemId=" + cloneRecord.originalItemId + "]");
        }
        clone.initFromClone(cloneRecord);
        return clone;
    }

    /**
     * Loads all original items owned by the specified member.
     */
    public List<T> loadOriginalItems (int ownerId)
        throws PersistenceException
    {
        return findAll(getItemClass(), new Where(ItemRecord.OWNER_ID, ownerId),
                       OrderBy.descending(ItemRecord.RATING));
    }

    /**
     * Loads all cloned items owned by the specified member.
     */
    public List<T> loadClonedItems (int ownerId)
        throws PersistenceException
    {
        return loadClonedItems(new Where(getCloneColumn(CloneRecord.OWNER_ID), ownerId));
    }

    /**
     * Loads the specified items. Omits missing items from results.
     */
    public List<T> loadItems (int[] itemIds)
        throws PersistenceException
    {
        if (itemIds.length == 0) {
            return Collections.emptyList();
        }
        Comparable[] idArr = IntListUtil.box(itemIds);
        Where inClause = new Where(new In(getItemClass(), ItemRecord.ITEM_ID, idArr));
        List<T> items = loadClonedItems(inClause);
        items.addAll(findAll(getItemClass(), inClause));
        return items;
    }

    /**
     * Loads items whose flags match the given mask and bit values, limited to a given
     * number of rows. This method can either require all flags to be set, or merely at
     * least one of them.
     */
    public List<T> loadItemsByFlag (byte flagMask, boolean all, int count)
        throws PersistenceException
    {
        return findAll(
            getItemClass(),
            new Where(all ?
                      new Equals(new BitAnd(ItemRecord.FLAGS, flagMask), flagMask) :
                      new GreaterThan(new BitAnd(ItemRecord.FLAGS, flagMask), 0)),
            new Limit(0, count));
    }

    /**
     * Loads a single clone record by item id.
     */
    public CLT loadCloneRecord (int itemId)
        throws PersistenceException
    {
        return load(getCloneClass(), itemId);
    }

    /**
     * Loads all the raw clone records associated with a given original item id. This is
     * potentially a very large dataset.
     */
    public List<CLT> loadCloneRecords (int itemId)
        throws PersistenceException
    {
        return findAll(getCloneClass(), new Where(CloneRecord.ORIGINAL_ITEM_ID, itemId));
    }

    /**
     * Loads and returns all items (clones and originals) that are "in use" at the specified
     * location.
     */
    public List<T> loadItemsByLocation (int location)
        throws PersistenceException
    {
        List<T> items = loadClonedItems(
            new Where(getCloneColumn(CloneRecord.LOCATION), location));
        List<T> citems = findAll(
            getItemClass(),new Where(getItemColumn(ItemRecord.LOCATION), location));
        items.addAll(citems);
        return items;
    }

    /**
     * Mark the specified items as being used in the specified way.
     */
    public void markItemUsage (int[] itemIds, byte usageType, int location)
        throws PersistenceException
    {
        Class<T> iclass = getItemClass();
        Class<CLT> cclass = getCloneClass();

        for (int itemId : itemIds) {
            int result;
            if (itemId > 0) {
                result = updatePartial(
                    iclass, itemId, ItemRecord.USED, usageType, ItemRecord.LOCATION, location,
                    ItemRecord.LAST_TOUCHED, new Timestamp(System.currentTimeMillis()));
            } else {
                result = updatePartial(
                    cclass, itemId, ItemRecord.USED, usageType, ItemRecord.LOCATION, location,
                    ItemRecord.LAST_TOUCHED, new Timestamp(System.currentTimeMillis()));
            }
            // if the item didn't update, freak out.
            if (0 == result) {
                Log.warning("Unable to find item to mark usage [itemId=" + itemId +
                            ", usageType=" + usageType + ", location=" + location + "].");
            }
        }
    }

    /**
     * Find a single catalog entry randomly.
     */
    public CAT pickRandomCatalogEntry ()
        throws PersistenceException
    {
        CAT record = load(getCatalogClass(), new QueryClause[] {
            new Limit(0, 1),
            OrderBy.random()
        });

        if (record != null) {
            record.item = loadOriginalItem(record.itemId);
        }
        return record;
    }

    /**
     * Find a single random catalog entry that is tagged with *any* of the specified tags.
     */
    public CAT findRandomCatalogEntryByTags (String... tags)
        throws PersistenceException
    {
        // first find the tag record...
        List<TagNameRecord> tagRecords = getTagRepository().getTags(tags);
        int tagCount = tagRecords.size();
        if (tagCount == 0) {
            return null;
        }

        Integer[] tagIds = new Integer[tagCount];
        for (int ii = 0; ii < tagCount; ii++) {
            tagIds[ii] = tagRecords.get(ii).tagId;
        }

        List<CAT> records = findAll(getCatalogClass(),
            new QueryClause[] {
                new Join(getCatalogClass(), CatalogRecord.ITEM_ID,
                    getItemClass(), ItemRecord.ITEM_ID),
                new Limit(0, 1),
                OrderBy.random(),
                new Join(getCatalogClass(), CatalogRecord.ITEM_ID,
                    getTagRepository().getTagClass(), TagRecord.TARGET_ID),
                new Where(new In(TagRecord.TAG_ID, tagIds))
            });

        if (records.isEmpty()) {
            return null;
        }

        CAT record = records.get(0);
        record.item = loadOriginalItem(record.itemId);
        return record;
    }

    /**
     * Counts all items in the catalog that match the supplied query terms.
     */
    public int countListings (boolean mature, String search, int tag, int creator)
        throws PersistenceException
    {
        ArrayList<QueryClause> clauses = new ArrayList<QueryClause>();
        clauses.add(new FromOverride(getCatalogClass()));
        clauses.add(new Join(getCatalogClass(), CatalogRecord.ITEM_ID,
                             getItemClass(), ItemRecord.ITEM_ID));

        // see if there's any where bits to turn into an actual where clause
        addSearchClause(clauses, mature, search, tag, creator);

        // finally fetch all the catalog records of interest
        ListingCountRecord crec = load(
            ListingCountRecord.class, clauses.toArray(new QueryClause[clauses.size()]));
        return crec.count;
    }

    /**
     * Loads all items in the catalog.
     *
     * TODO: This method currently fetches CatalogRecords through a join against ItemRecord,
     *       and then executes a second query against ItemRecord only. This really really has
     *       to be a single join in a sane universe, but that makes significant demands on the
     *       Depot code that we don't know how to handle yet (or possibly some fiddling with
     *       the Item vs Catalog class hierarchies).
     */
    public List<CAT> loadCatalog (byte sortBy, boolean mature, String search, int tag,
                                  int creator, int offset, int rows)
        throws PersistenceException
    {
        ArrayList<QueryClause> clauses = new ArrayList<QueryClause>();
        clauses.add(new Join(getCatalogClass(), CatalogRecord.ITEM_ID,
                             getItemClass(), ItemRecord.ITEM_ID));
        clauses.add(new Limit(offset, rows));

        OrderBy sortExp;
        switch(sortBy) {
        case CatalogListing.SORT_BY_NOTHING:
            sortExp = null;
            break;
        case CatalogListing.SORT_BY_LIST_DATE:
            sortExp = OrderBy.descending(
                new ColumnExp(getCatalogClass(), CatalogRecord.LISTED_DATE));
            break;
        case CatalogListing.SORT_BY_RATING:
            sortExp = OrderBy.descending(
                new FunctionExp("floor", getItemColumn(ItemRecord.RATING)));
            break;
        case CatalogListing.SORT_BY_PRICE_ASC:
        case CatalogListing.SORT_BY_PRICE_DESC:
            SQLExpression bit =
                new Add(new ColumnExp(getCatalogClass(), CatalogRecord.FLOW_COST),
                        new Mul(new ColumnExp(getCatalogClass(), CatalogRecord.GOLD_COST),
                                FLOW_FOR_GOLD));
            sortExp = (sortBy == CatalogListing.SORT_BY_PRICE_ASC) ?
                OrderBy.ascending(bit) : OrderBy.descending(bit);
            break;
        default:
            throw new IllegalArgumentException(
                "Sort method not implemented [sortBy=" + sortBy + "]");
        }
        if (sortExp != null) {
            clauses.add(sortExp);
        }

        // see if there's any where bits to turn into an actual where clause
        addSearchClause(clauses, mature, search, tag, creator);

        // finally fetch all the catalog records of interest
        List<CAT> records = findAll(
            getCatalogClass(), clauses.toArray(new QueryClause[clauses.size()]));
        if (records.size() == 0) {
            return records;
        }

        // construct an array of item ids we need to load
        Comparable[] idArr = new Integer[records.size()];
        int ii = 0;
        for (CatalogRecord record : records) {
            idArr[ii ++] = record.itemId;
        }

        // load those items and map item ID's to items
        List<T> items = findAll(
            getItemClass(), new Where(new In(getItemClass(), ItemRecord.ITEM_ID, idArr)));
        Map<Integer, T> map = new HashMap<Integer, T>();
        for (T iRec : items) {
            map.put(iRec.itemId, iRec);
        }

        // finally populate the catalog records
        for (CatalogRecord<T> record : records) {
            record.item = map.get(record.itemId);
        }
        return records;
    }

    /**
     * Helper function for {@link #countListings} and {@link #loadCatalog}.
     */
    protected void addSearchClause (ArrayList<QueryClause> clauses, boolean mature, String search,
                                    int tag, int creator)
        throws PersistenceException
    {
        ArrayList<SQLOperator> whereBits = new ArrayList<SQLOperator>();

        if (search != null && search.length() > 0) {
            whereBits.add(new Like(ItemRecord.NAME, "%" + search + "%"));
        }

        if (tag > 0) {
            // join against TagRecord
            clauses.add(new Join(getCatalogClass(), CatalogRecord.ITEM_ID,
                                 getTagRepository().getTagClass(), TagRecord.TARGET_ID));
            // and add a condition
            whereBits.add(new Equals(TagRecord.TAG_ID, tag));
        }

        if (creator > 0) {
            whereBits.add(new Equals(ItemRecord.CREATOR_ID, creator));
        }

        if (!mature) {
            // add a check to make sure ItemRecord.FLAG_MATURE is not set on any returned items
            whereBits.add(new Equals(new BitAnd(ItemRecord.FLAGS, Item.FLAG_MATURE), 0));
        }

        if (whereBits.size() > 0) {
            clauses.add(new Where(new And(whereBits.toArray(new SQLOperator[whereBits.size()]))));
        }
    }

    /**
     * Load a single catalog listing.
     *
     * TODO: This needs to be a join, just like {@link #loadCatalog}.
     */
    public CAT loadListing (int itemId)
        throws PersistenceException
    {
        CAT record = load(getCatalogClass(), itemId);
        if (record != null) {
            record.item = load(getItemClass(), itemId);
        }
        return record;
    }

    /**
     * Update either the 'purchases' or the 'returns' field of a catalog listing,
     * and figure out if it's time to reprice it.
     */
    public void nudgeListing (int itemId, boolean purchased)
        throws PersistenceException
    {
        CAT record = load(getCatalogClass(), itemId);
        if (record == null) {
            // if the record managed to vanish, I suppose we don't need to nudge it.
            return;
        }
        if (purchased) {
            record.purchases += 1;
            record.repriceCounter += 1;
        } else {
            record.returns += 1;
            record.repriceCounter += 2;
        }
        if (record.repriceCounter >= Math.sqrt(record.purchases + record.returns)) {
            priceRecord(record, false);
        }
        String column = purchased ? CatalogRecord.PURCHASES : CatalogRecord.RETURNS;
        update(record, column, CatalogRecord.REPRICE_COUNTER, CatalogRecord.FLOW_COST,
               CatalogRecord.GOLD_COST);
    }

    /**
     * Inserts the supplied item into the database. The {@link ItemRecord#itemId} and the
     * {@link ItemRecord#lastTouched) fields will be filled in as a result of this call.
     */
    public void insertOriginalItem (T item)
        throws PersistenceException
    {
        if (item.itemId != 0) {
            throw new PersistenceException("Can't insert item with existing key: " + item);
        }
        item.lastTouched = new Timestamp(System.currentTimeMillis());
        insert(item);
    }

    /**
     * Updates the supplied item in the database. The {@link ItemRecord#lastTouched) field
     * will be filled in as a result of this call.
     */
    public void updateOriginalItem (T item)
        throws PersistenceException
    {
        item.lastTouched = new Timestamp(System.currentTimeMillis());
        update(item);
    }

    /**
     * Create a row in our catalog table corresponding to the given item record, which should
     * be of the immutable variety.
     */
    public CatalogRecord insertListing (ItemRecord listItem, int rarity, long listingTime)
        throws PersistenceException
    {
        if (listItem.ownerId != 0) {
            throw new PersistenceException(
                "Can't list item with owner [itemId=" + listItem.itemId + "]");
        }

        CAT record;
        try {
            record = getCatalogClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        record.item = listItem;
        record.itemId = listItem.itemId;
        record.listedDate = new Timestamp(listingTime);
        record.rarity = rarity;
        record.purchases = record.returns = 0;
        priceRecord(record, true);
        // and insert it - done!
        insert(record);
        return record;
    }

    /**
     * Removes the listing for the specified item from the catalog, returns true if a listing was
     * found and removed, false otherwise.
     */
    public boolean removeListing  (int itemId)
        throws PersistenceException
    {
        return delete(getCatalogClass(), itemId) > 0;
    }

    /**
     * Inserts an item clone into the database with the given owner and purchase data. Also fills
     * (@link CloneRecord#itemId) with the next available ID and {@link CloneRecord#ownerId)
     * with the new owner. Finally, updates {@link CloneRecord#lastTouched) and
     * {@link CloneRecord#purchaseTime).
     */
    public ItemRecord insertClone (ItemRecord parent, int newOwnerId, int flowPaid, int goldPaid)
        throws PersistenceException
    {
        CLT record;
        try {
            record = getCloneClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        record.initialize(parent, newOwnerId, flowPaid, goldPaid);
        insert(record);

        ItemRecord newClone = (ItemRecord) parent.clone();
        newClone.initFromClone(record);
        return newClone;
    }

    /**
     * Deletes an item from the repository and all associated data (ratings, tags, tag history).
     * This method does not perform any checking to determine whether it is safe to delete the item
     * so do not call it unless you know the item is not listed in the catalog or otherwise in use.
     */
    public void deleteItem (final int itemId)
        throws PersistenceException
    {
        if (itemId < 0) {
            delete(getCloneClass(), itemId);

        } else {
            // delete the item in question
            delete(getItemClass(), itemId);

            // invalidate and delete rating records for this item
            CacheInvalidator inv = new CacheInvalidator() {
                public void invalidate (PersistenceContext ctx) {
                    // invalidate and delete rating records for this item
                    ctx.cacheTraverse(getRatingClass().getName(), new CacheEvictionFilter<RT>() {
                        public boolean testForEviction (Serializable key, RT record) {
                            return record != null && record.itemId == itemId;
                        }
                    });
                }
            };
            deleteAll(getRatingClass(), new Where(RatingRecord.ITEM_ID, itemId), inv);

//             // invalidate and delete tag records for this item
//             inv = new CacheInvalidator() {
//                 public void invalidate (PersistenceContext ctx) {
//                     ctx.cacheTraverse(getTagClass().getName(), new CacheEvictionFilter<TT>() {
//                         public boolean testForEviction (Serializable key, TT record) {
//                             return record != null && record.itemId == itemId;
//                         }
//                     });
//                 }
//             };
//             deleteAll(getTagClass(), new Where(TagRecord.ITEM_ID, itemId), inv);

//             // invalidate and delete tag history records for this item
//             inv = new CacheInvalidator() {
//                 public void invalidate (PersistenceContext ctx) {
//                     String cacheName = getTagHistoryClass().getName();
//                     ctx.cacheTraverse(cacheName, new CacheEvictionFilter<THT>() {
//                         public boolean testForEviction (Serializable key, THT record) {
//                             return record != null && record.itemId == itemId;
//                         }
//                     });
//                 }
//             };
//             deleteAll(getTagHistoryClass(), new Where(TagHistoryRecord.ITEM_ID, itemId), inv);
        }
    }

    /**
     * Returns the rating given to the specified item by the specified member.
     */
    public RatingRecord<T> getRating (int itemId, int memberId)
        throws PersistenceException
    {
        return load(getRatingClass(),
                    RatingRecord.ITEM_ID, itemId,
                    RatingRecord.MEMBER_ID, memberId);
    }

    /**
     * Insert/update a rating row, calculate the new rating and finally update the item's rating.
     */
    public float rateItem (final int itemId, int memberId, byte rating)
        throws PersistenceException
    {
        // first create a new rating record
        RatingRecord<T> record;
        try {
            record = getRatingClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(
                "Failed to create a new item rating record " +
                "[itemId=" + itemId + ", memberId=" + memberId + "]", e);
        }
        // populate and insert it
        record.itemId = itemId;
        record.memberId = memberId;
        record.rating = rating;
        store(record);

        RatingAverageRecord average =
            load(RatingAverageRecord.class,
                 new FieldOverride("count", "count(*)"),
                 new FieldOverride("sum", "sum(" + RatingRecord.RATING + ")"),
                 new FromOverride(getRatingClass()),
                 new Where(new Equals(new ColumnExp(getRatingClass(), RatingRecord.ITEM_ID),
                                      itemId)));
        float newRating = (float) ((average.count == 0) ? 0.0 : average.sum/average.count);
        // and then smack the new value into the item using yummy depot code
        updatePartial(getItemClass(), itemId, ItemRecord.RATING, newRating,
                      ItemRecord.LAST_TOUCHED, new Timestamp(System.currentTimeMillis()));
        return newRating;
    }

    /**
     * Safely changes the owner of an item record with a sanity-check against race conditions.
     */
    public void updateOwnerId (ItemRecord item, int newOwnerId)
        throws PersistenceException
    {
        Key key = item.itemId < 0 ?
            new Key<CLT>(getCloneClass(), CloneRecord.ITEM_ID, item.itemId) :
            new Key<T>(getItemClass(), ItemRecord.ITEM_ID, item.itemId);
        int modifiedRows =  updatePartial(
            item.itemId < 0 ? getCloneClass() : getItemClass(),
            new Where(ItemRecord.ITEM_ID, item.itemId,
                      ItemRecord.OWNER_ID, item.ownerId),
            key,
            ItemRecord.OWNER_ID, newOwnerId,
            ItemRecord.LAST_TOUCHED, new Timestamp(System.currentTimeMillis()));
        if (modifiedRows == 0) {
            throw new PersistenceException(
                "Failed to safely update ownerId [item=" + item + ", newOwnerId=" + newOwnerId + "]");
        }
    }

    protected void priceRecord (CAT record, boolean always)
        throws PersistenceException
    {
        // size up the active member population
        int U = MsoyServer.memberRepo.getActivePopulationCount();
        // calculate the target item population
        int C, targetPopulation;
        switch(record.rarity) {
        case CatalogListing.RARITY_PLENTIFUL:
            C = 100;
            targetPopulation = U*10; break;
        case CatalogListing.RARITY_COMMON:
            C = 300;
            targetPopulation = U; break;
        case CatalogListing.RARITY_NORMAL:
            C = 500;
            targetPopulation = U/10; break;
        case CatalogListing.RARITY_UNCOMMON:
            C = 700;
            targetPopulation = U/100; break;
        case CatalogListing.RARITY_RARE:
            C = 900;
            targetPopulation = U/1000; break;
        default:
            throw new PersistenceException(
                "Unknown rarity [class=" + record.getClass() + ", itemId=" + record.itemId +
                ", rarity=" + record.rarity + "]");
        }
        targetPopulation = Math.max(targetPopulation, 1);
        // see if really need to reprice this item
        if (!always && record.repriceCounter < Math.min(Math.sqrt(targetPopulation), 100)) {
            return;
        }

        int currentPopulation = Math.max(0, record.purchases - record.returns);
        double ratio = (double) currentPopulation / targetPopulation;
        double basePrice = 100 + C * ((ratio < 1.0) ? Math.sqrt(ratio) : Math.pow(ratio, 4));
        double S = (record.purchases < MIN_ATTEN_PURCHASES) ? 1f :
            Math.max(0, 1 - record.returns / (double)record.purchases);
        double listPrice = Math.max(1, basePrice * Math.sqrt(S));
        // TEMP: no gold cost
        record.goldCost = 0; // (int) Math.round(listPrice / (2.5 * flowForGoldFactor));
        record.flowCost = (int) (listPrice - record.goldCost * FLOW_FOR_GOLD);
        record.repriceCounter = 0;
        log.info("Repriced [item=" + record.itemId + ", Ec=" + currentPopulation +
                 ", Et=" + targetPopulation + ", base=" + basePrice + ", S=" + S +
                 ", list=" + listPrice + "].");
    }

    /**
     * Performs the necessary join to load cloned items matching the supplied where clause.
     */
    protected List<T> loadClonedItems (Where where)
        throws PersistenceException
    {
        return findAll(
            getItemClass(), where,
            new Join(getItemClass(), ItemRecord.ITEM_ID,
                     getCloneClass(), CloneRecord.ORIGINAL_ITEM_ID),
            new FieldOverride(ItemRecord.ITEM_ID, getCloneClass(), CloneRecord.ITEM_ID),
            new FieldOverride(ItemRecord.PARENT_ID, getItemClass(), ItemRecord.ITEM_ID),
            new FieldOverride(ItemRecord.OWNER_ID, getCloneClass(), CloneRecord.OWNER_ID),
            new FieldOverride(ItemRecord.LOCATION, getItemClass(), CloneRecord.LOCATION),
            new FieldOverride(ItemRecord.USED, getItemClass(), CloneRecord.USED),
            new FieldOverride(ItemRecord.LAST_TOUCHED, getCloneClass(), CloneRecord.LAST_TOUCHED)
            );
    }

    protected ColumnExp getItemColumn (String cname)
    {
        return new ColumnExp(getItemClass(), cname);
    }

    protected ColumnExp getCloneColumn (String cname)
    {
        return new ColumnExp(getCloneClass(), cname);
    }

    /**
     * Specific item repositories override this method and indicate the class of item on which they
     * operate.
     */
    protected abstract Class<T> getItemClass ();

    /**
     * Specific item repositories override this method and indicate their item's clone persistent
     * record class.
     */
    protected abstract Class<CLT> getCloneClass ();

    /**
     * Specific item repositories override this method and indicate their item's catalog persistent
     * record class.
     */
    protected abstract Class<CAT> getCatalogClass ();

    /**
     * Specific item repositories override this method and indicate their item's rating persistent
     * record class.
     */
    protected abstract Class<RT> getRatingClass ();

    /**
     * Specific item repositories override this method and indicate their item's tag persistent
     * record class.
     */
    protected abstract TagRecord createTagRecord ();

    /**
     * Specific item repositories override this method and indicate their item's tag history
     * persistent record class.
     */
    protected abstract TagHistoryRecord createTagHistoryRecord ();

    /** Used to manage our item tags. */
    protected TagRepository _tagRepo;

    /** The minimum number of purchases before we'll start attenuating price based on returns. */
    protected static final int MIN_ATTEN_PURCHASES = 5;
}
