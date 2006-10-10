//
// $Id$

package com.threerings.msoy.item.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.MemberGName;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.TagHistory;

import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.DocumentRepository;
import com.threerings.msoy.item.server.persist.FurnitureRepository;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PetRepository;
import com.threerings.msoy.item.server.persist.PhotoRepository;
import com.threerings.msoy.item.server.persist.RatingRecord;
import com.threerings.msoy.item.server.persist.TagHistoryRecord;
import com.threerings.msoy.item.server.persist.TagNameRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
public class ItemManager
    implements ItemProvider
{
    /**
     * Initializes the item manager, which will establish database connections
     * for all of its item repositories.
     */
    @SuppressWarnings("unchecked")
    public void init (ConnectionProvider conProv) throws PersistenceException
    {
        // create our various repositories
        ItemRepository repo = new AvatarRepository(conProv);
        _repos.put(Item.AVATAR, repo);
        repo = new DocumentRepository(conProv);
        _repos.put(Item.DOCUMENT, repo);
        repo = new FurnitureRepository(conProv);
        _repos.put(Item.FURNITURE, repo);
        repo = (_gameRepo = new GameRepository(conProv));
        _repos.put(Item.GAME, repo);
        repo = new PetRepository(conProv);
        _repos.put(Item.PET, repo);
        repo = new PhotoRepository(conProv);
        _repos.put(Item.PHOTO, repo);

        // register our invocation service
        MsoyServer.invmgr.registerDispatcher(new ItemDispatcher(this), true);
    }

    /**
     * Provides a reference to the {@link GameRepository} which is used for
     * nefarious ToyBox purposes.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Get the specified item.
     */
    public void getItem (final ItemIdent ident, ResultListener<Item> listener)
    {
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // TODO: do we have to check cloned items as well?
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult ()
                throws PersistenceException
            {
                return repo.loadItem(ident.itemId).toItem();
            }
        });
    }

    /**
     * Inserts the supplied item into the system. The item should be fully
     * configured, and an item id will be assigned during the insertion
     * process. Success or failure will be communicated to the supplied result
     * listener.
     */
    public void insertItem (final Item item, ResultListener<Item> listener)
    {
        final ItemRecord record = ItemRecord.newRecord(item);
        byte type = record.getType();

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and insert the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException {
                repo.insertItem(record);
                item.itemId = record.itemId;
                return item;
            }
            public void handleSuccess () {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(record);
            }
        });
    }

    /**
     * Updates the supplied item. The item should have previously been checked
     * for validity. Success or failure will be communicated to the supplied
     * result listener.
     */
    public void updateItem (final Item item, ResultListener<Item> listener)
    {
        final ItemRecord record = ItemRecord.newRecord(item);
        byte type = record.getType();

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and update the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException {
                repo.updateItem(record);
                return item;
            }
            public void handleSuccess () {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(record);
            }
        });
    }

    /**
     * Get the details of specified item: display-friendly names of creator
     * and owner and the rating given to this item by the member specified
     * by memberId.
     * 
     * TODO: This method re-reads an ItemRecord that the caller of the
     * function almost certainly already has, which seems kind of wasteful.
     * On the other hand, transmitting that entire Item over the wire seems
     * wasteful, too, and sending in ownerId and creatorId by themselves
     * seems cheesy. If we were to cache ItemRecords in the repository, this
     * would be fine. Will we?
     */
    public void getItemDetail (
        final ItemIdent ident, final int memberId,
        ResultListener<ItemDetail> listener)
    {
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }
            
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ItemDetail>(listener) {
                public ItemDetail invokePersistResult ()
                    throws PersistenceException
                {
                    ItemRecord record = repo.loadItem(ident.itemId);
                    ItemDetail detail = new ItemDetail();
                    detail.item = record.toItem();
                    RatingRecord<ItemRecord> rr = repo.getRating(ident.itemId, memberId);
                    detail.memberRating = rr == null ? 0 : rr.rating;
                    MemberRecord memRec =
                        MsoyServer.memberRepo.loadMember(record.creatorId);
                    detail.creator = 
                        new MemberGName(memRec.name, memRec.memberId);
                    if (record.ownerId != -1) {
                        memRec = 
                            MsoyServer.memberRepo.loadMember(record.ownerId);
                        detail.owner =
                            new MemberGName(memRec.name, memRec.memberId);
                    } else {
                        detail.owner = null;
                    }
                    return detail;
                }

        });
    }

    /**
     * Loads up the inventory of items of the specified type for the specified
     * member. The results may come from the cache and will be cached after
     * being loaded from the database.
     */
    public void loadInventory (final int memberId, byte type,
                               ResultListener<ArrayList<Item>> listener)
    {
        // first check the cache
        final Tuple<Integer, Byte> key =
            new Tuple<Integer, Byte>(memberId, type);
//      TODO: Disable cache for the moment
        if (false) {
        Collection<ItemRecord> items = _itemCache.get(key);
        if (items != null) {
            ArrayList<Item> list = new ArrayList<Item>();
            for (ItemRecord record : items) {
                list.add(record.toItem());
            }
            listener.requestCompleted(list);
            return;
        }
        }

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and load their items; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<Item>>(listener) {
                    public ArrayList<Item> invokePersistResult ()
                        throws PersistenceException
                    {
                        Collection<ItemRecord> list =
                            repo.loadOriginalItems(memberId);
                        list.addAll(repo.loadClonedItems(memberId));
                        ArrayList<Item> newList = new ArrayList<Item>();
                        for (ItemRecord record : list) {
                            newList.add(record.toItem());
                        }
                        return newList;
                    }

                    public void handleSuccess ()
                    {
// TODO: The cache needs some rethinking, I figure.
//                        _itemCache.put(key, _result);
                        super.handleSuccess();
                    }
                });
    }

    /**
     * Fetches the entire catalog of listed items of the given type.
     */
    public void loadCatalog (int memberId, byte type,
                             ResultListener<ArrayList<CatalogListing>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and load the catalog
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<CatalogListing>>(listener) {
                public ArrayList<CatalogListing> invokePersistResult ()
                    throws PersistenceException
                {
                    ArrayList<CatalogListing> list =
                        new ArrayList<CatalogListing>();
                    for (CatalogRecord record : repo.loadCatalog()) {
                        list.add(record.toListing());
                    }
                    return list;
                }
            });
    }

    /**
     * Purchases a given item for a given member from the catalog by
     * creating a new clone row in the appropriate database table.
     */
    public void purchaseItem (final int memberId, final ItemIdent ident,
                              ResultListener<Item> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the purchase
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load the item being purchased
                ItemRecord item = repo.loadItem(ident.itemId);
                // sanity check it
                if (item.ownerId != -1) {
                    throw new PersistenceException(
                        "Can only clone listed items [itemId=" +
                        item.itemId + "]");
                }
                // create the clone row in the database!
                int cloneId = repo.insertClone(item.itemId, memberId);
                // then dress the loaded item up as a clone
                item.ownerId = memberId;
                item.parentId = item.itemId;
                item.itemId = cloneId;
                return item.toItem();
            }
        });
    }

    /**
     * Lists the given item in the catalog by creating a new item row and
     * a new catalog row and returning the immutable form of the item.
     */
    public void listItem (final ItemIdent ident,
                          ResultListener<CatalogListing> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the listing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<CatalogListing>(listener) {
            public CatalogListing invokePersistResult ()
                throws PersistenceException
            {
                // load a copy of the original item
                ItemRecord listItem = repo.loadItem(ident.itemId);
                if (listItem == null) {
                    throw new PersistenceException(
                        "Can't find object to list [item= " + ident + "]");
                }
                if (listItem.ownerId == -1) {
                    throw new PersistenceException(
                        "Object is already listed [item=" + ident + "]");
                }
                // reset the owner
                listItem.ownerId = -1;
                // and the iD
                listItem.itemId = 0;
                // then insert it as the immutable copy we list
                repo.insertItem(listItem);
                // and finally create & insert the catalog record
                CatalogRecord record = repo.insertListing(
                    listItem, new Timestamp(System.currentTimeMillis()));
                return record.toListing();
            }
        });
    }

    /**
     * Remix a clone, turning it back into a full-featured original.
     */
    public void remixItem (final ItemIdent ident, ResultListener<Item> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load a copy of the clone to modify
                _item = repo.loadClone(ident.itemId);
                if (_item == null) {
                    throw new PersistenceException(
                        "Can't find item [item=" + ident + "]");
                }
                // TODO: make sure we should not use the original creator here
                // make it ours
                _item.creatorId = _item.ownerId;
                // let the object forget whence it came
                int originalId = _item.parentId;
                _item.parentId = -1;
                // insert it as a genuinely new item
                _item.itemId = 0;
                repo.insertItem(_item);
                // delete the old clone
                repo.deleteClone(ident.itemId);
                // copy tags from the original to the new item
                repo.copyTags(
                    originalId, _item.itemId, _item.ownerId,
                    System.currentTimeMillis());
                return _item.toItem();
            }

            public void handleSuccess ()
            {
                super.handleSuccess();
                // add the item to the user's cached inventory
//                updateUserCache(_item);
            }

            protected ItemRecord _item;
        });

    }

    /** Fetch the rating a user has given an item, or 0. */
    public void getRating (final ItemIdent ident, final int memberId,
                           ResultListener<Byte> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Byte>(listener) {
                public Byte invokePersistResult () throws PersistenceException {
                    RatingRecord<ItemRecord> record =
                        repo.getRating(ident.itemId, memberId);
                    return record != null ? record.rating : 0;
                }
            });
    }
    
    /** Fetch the tags for a given item. */
    public void getTags (final ItemIdent ident,
                         ResultListener<Collection<String>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<String>>(listener) {
                public Collection<String> invokePersistResult ()
                        throws PersistenceException {
                    ArrayList<String> result = new ArrayList<String>();
                    for (TagNameRecord tagName : repo.getTags(ident.itemId)) {
                        result.add(tagName.tag);
                    }
                    return result;
                }
            });
    }
            

    /**
     * Fetch the tagging history for a given item.
     */
    public void getTagHistory (final ItemIdent ident,
                               ResultListener<Collection<TagHistory>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<TagHistory>>(listener) {
            public Collection<TagHistory> invokePersistResult ()
                throws PersistenceException {
                HashMap<Integer, MemberRecord> memberCache =
                    new HashMap<Integer, MemberRecord>();
                ArrayList<TagHistory> list = new ArrayList<TagHistory>();
                for (TagHistoryRecord<ItemRecord> record :
                         repo.getTagHistoryByItem(ident.itemId)) {
                    // TODO: we should probably cache in MemberRepository
                    MemberRecord memRec = memberCache.get(record.memberId);
                    if (memRec == null) {
                        memRec = MsoyServer.memberRepo.loadMember(
                            record.memberId);
                        memberCache.put(record.memberId, memRec);
                    }
                    TagNameRecord tag = repo.getTag(record.tagId);
                    TagHistory history = new TagHistory();
                    history.item =
                        new ItemIdent(ident.type, ident.itemId);
                    history.member =
                        new MemberGName(memRec.name, memRec.memberId);
                    history.tag = tag.tag;
                    history.action = record.action;
                    history.time = new Date(record.time.getTime());
                    list.add(history);
                }
                return list;
            }
        });
    }

    /** Fetch the tagging history for any item by  a given member. */
    public void getTagHistory (final int memberId,
                               ResultListener<Collection<TagHistory>> listener)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<TagHistory>>(listener) {
            public Collection<TagHistory> invokePersistResult ()
                throws PersistenceException {
                MemberRecord memRec =
                    MsoyServer.memberRepo.loadMember(memberId);
                MemberGName memName =
                    new MemberGName(memRec.name, memRec.memberId);
                ArrayList<TagHistory> list = new ArrayList<TagHistory>();
                for (Entry<Byte, ItemRepository<ItemRecord>> entry :
                        _repos.entrySet()) {
                    byte type = entry.getKey();
                    ItemRepository<ItemRecord> repo = entry.getValue();
                    for (TagHistoryRecord<ItemRecord> record :
                            repo.getTagHistoryByMember(memberId)) {
                        TagNameRecord tag = record.tagId == -1 ? null :
                            repo.getTag(record.tagId);
                        TagHistory history = new TagHistory();
                        history.item = new ItemIdent(
                            type, record.itemId);
                        history.member = memName;
                        history.tag = tag == null ? null : tag.tag;
                        history.action = record.action;
                        history.time = new Date(record.time.getTime());
                        list.add(history);
                    }
                }
                return list;
            }
        });
    }
    
    /**
     * Records the specified member's rating of an item.
     */
    public void rateItem (final ItemIdent ident, final int memberId,
                          final byte rating,
                          final ResultListener<ItemDetail> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ItemDetail>(listener) {
                public ItemDetail invokePersistResult ()
                    throws PersistenceException {
                    ItemRecord item = repo.loadItem(ident.itemId);
                    int originalId;
                    if (item == null) {
                        item = repo.loadClone(ident.itemId);
                        if (item == null) {
                            throw new PersistenceException(
                                "Can't find item [item=" + ident + "]");
                        }
                        originalId = item.parentId;
                    } else {
                        // make sure we're not trying to rate a mutable
                        if (item.ownerId != -1) {
                            throw new PersistenceException(
                                "Can't rate mutable object [item=" + ident + "]");
                        }
                        originalId = ident.itemId;
                    }
                    item.rating = repo.rateItem(originalId, memberId, rating);
                    ItemDetail detail = new ItemDetail();
                    detail.item = item.toItem();
                    detail.memberRating = rating;
                    MemberRecord memRec =
                        MsoyServer.memberRepo.loadMember(item.creatorId);
                    detail.creator = 
                        new MemberGName(memRec.name, memRec.memberId);
                    if (item.ownerId != -1) {
                        memRec = 
                            MsoyServer.memberRepo.loadMember(item.ownerId);
                        detail.owner =
                            new MemberGName(memRec.name, memRec.memberId);
                    } else {
                        detail.owner = null;
                    }
                    return detail;
            }
        });
    }

    /**
     * Add the specified tag to the specified item. Return a tag history object
     * if the tag did not already exist.
     */
    public void tagItem (ItemIdent ident, int taggerId, String tagName,
                         ResultListener<TagHistory> listener)
    {
        itemTagging(ident, taggerId, tagName, listener, true);
    }

    /**
     * Remove the specified tag from the specified item. Return a tag history
     * object if the tag existed.
     */
    public void untagItem (ItemIdent ident, int taggerId, String tagName,
                           ResultListener<TagHistory> listener)
    {
        itemTagging(ident, taggerId, tagName, listener, false);
    }

    // from ItemProvider
    public void getInventory (ClientObject caller, byte type,
            final InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memberObj = (MemberObject) caller;
        if (memberObj.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }

        // then, load that type
        // TODO: not everything!
        loadInventory(
            memberObj.getMemberId(), type,
            new ResultListener<ArrayList<Item>>() {
                public void requestCompleted (ArrayList<Item> result)
                {
                    Item[] items = new Item[result.size()];
                    result.toArray(items);
                    listener.requestProcessed(items);
                }

                public void requestFailed (Exception cause)
                {
                    log.warning("Unable to retrieve inventory " + "[cause="
                        + cause + "].");
                    listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            });
    }

    /**
     * Does the facade work for tagging.
     */
    protected void itemTagging (
        final ItemIdent ident, final int taggerId, final String rawTagName,
        ResultListener<TagHistory> listener, final boolean doTag)
    {
        // sanitize the tag name
        final String tagName = rawTagName.trim().toLowerCase();

        if (!validTag.matcher(tagName).matches()) {
            listener.requestFailed(new IllegalArgumentException(
                "Invalid tag [tag=" + tagName + "]"));
            return;
        }

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<TagHistory>(listener) {
            public TagHistory invokePersistResult ()
                throws PersistenceException {
                long now = System.currentTimeMillis();

                ItemRecord item = repo.loadItem(ident.itemId);
                int originalId;
                if (item == null) {
                    // it's probably a clone
                    item = repo.loadClone(ident.itemId);
                    if (item == null) {
                        throw new PersistenceException(
                            "Can't find item [item=" + ident + "]");
                    }
                    // in which case we fetch the original
                    originalId = item.parentId;
                } else {
                    originalId = ident.itemId;
                }

                // map tag to tag id
                TagNameRecord tag = repo.getTag(tagName);

                // and do the actual work
                TagHistoryRecord<ItemRecord> historyRecord = doTag ?
                    repo.tagItem(originalId, tag.tagId, taggerId, now) :
                    repo.untagItem(originalId, tag.tagId, taggerId, now);
                if (historyRecord != null) {
                    // look up the member
                    MemberRecord member = MsoyServer.memberRepo.loadMember(
                        taggerId);
                    // and create the return value
                    TagHistory history = new TagHistory();
                    history.item = new ItemIdent(ident.type, originalId);
                    history.member =
                        new MemberGName(member.name, member.memberId);
                    history.tag = tag.tag;
                    history.action = historyRecord.action; 
                    history.time = new Date(historyRecord.time.getTime());
                    return history;
                }
                return null;
            }
        });
    }
    
    /**
     * Called when an item is newly created and should be inserted into the
     * owning user's inventory cache.
     */
    protected void updateUserCache (ItemRecord item)
    {
        byte type = item.getType();
        Collection<ItemRecord> items =
            _itemCache.get(new Tuple<Integer, Byte>(item.ownerId, type));
        if (items != null) {
            items.add(item);
        }
    }

    /**
     * Helper function for mapping ident to repository.
     */
    protected ItemRepository<ItemRecord> getRepository (
        ItemIdent ident, ResultListener<?> listener)
    {
        return getRepository(ident.type, listener);
    }

    /**
     * Helper function for mapping ident to repository.
     */
    protected ItemRepository<ItemRecord> getRepository (
        byte type, ResultListener<?> listener)
    {
        ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            String errmsg = "No repository registered for " + type + ".";
            listener.requestFailed(new Exception(errmsg));
        }
        return repo;
    }

    /** Contains a reference to our game repository. We'd just look this up
     * from the table but we can't downcast an ItemRepository<ItemRecord> to a
     * GameRepository, annoyingly. */
    protected GameRepository _gameRepo;

    /** A regexp pattern to validate tags. */
    protected static final Pattern validTag =
        Pattern.compile("[a-z](_?[a-z0-9]){2,18}");

    /** Maps byte type ids to repository for all digital item types. */
    protected HashMap<Byte, ItemRepository<ItemRecord>> _repos =
        new HashMap<Byte, ItemRepository<ItemRecord>>();

    /** A soft reference cache of item list indexed on (user,type). */
    protected SoftCache<Tuple<Integer, Byte>, Collection<ItemRecord>>
        _itemCache =
        new SoftCache<Tuple<Integer, Byte>, Collection<ItemRecord>>();
}
