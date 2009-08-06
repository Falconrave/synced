//
// $Id$

package com.threerings.msoy.stuff.server;

import static com.threerings.msoy.Log.log;

import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.UploadUtil;

import com.threerings.msoy.stuff.gwt.StuffService;

/**
 * Provides the server implementation of {@link StuffService}.
 */
public class StuffServlet extends MsoyServiceServlet
    implements StuffService
{
    // from interface ItemService
    public MediaDesc publishExternalMedia (String data, byte mimeType)
        throws ServiceException
    {
        ExternalUploadFile file = new ExternalUploadFile(data, mimeType);
        try {
            UploadUtil.publishUploadFile(file);
            return new MediaDesc(file.getHash(), file.getMimeType(), MediaDesc.NOT_CONSTRAINED);
        } catch (IOException ioe) {
            log.warning("Unable to publish external media file", ioe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface StuffService
    public Item createItem (Item item)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();

        item = _itemLogic.createItem(memrec.memberId, item).toItem();

        // Some items have a stat that may need updating
        if (item instanceof Avatar) {
            _statLogic.ensureIntStatMinimum(
                memrec.memberId, StatType.AVATARS_CREATED, StatType.ITEM_UPLOADED);
        } else if (item instanceof Furniture) {
            _statLogic.ensureIntStatMinimum(
                memrec.memberId, StatType.FURNITURE_CREATED, StatType.ITEM_UPLOADED);
        } else if (item instanceof Decor) {
            _statLogic.ensureIntStatMinimum(
                memrec.memberId, StatType.BACKDROPS_CREATED, StatType.ITEM_UPLOADED);
        }

        // note in the event log that an item was uploaded
        _eventLog.itemUploaded(memrec.memberId, memrec.visitorId);

        return item;
    }

    // from interface StuffService
    public void updateItem (Item item)
        throws ServiceException
    {
        MemberRecord memrec = requireRegisteredUser();

        // make sure the item in question is consistent as far as the item is concerned
        if (!item.isConsistent()) {
            log.warning("Requested to update item with invalid version",
                "who", memrec.who(), "item", item);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // load up the old version of the item
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.getType());
        ItemRecord record = repo.loadItem(item.itemId);
        if (record == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // make sure they own it and created it, or are support+
        if (((record.ownerId != memrec.memberId) || (record.creatorId != memrec.memberId)) &&
            !memrec.isSupport()) {
            throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
        }

        // make a copy of this for later
        ItemRecord oldrec = (ItemRecord)record.clone();

        // update it with data from the supplied runtime record
        record.fromItem(item);

        // write it back to the database
        repo.updateOriginalItem(record);

        // note that we've update our bits
        _itemLogic.itemUpdated(oldrec, record);
    }

    // from interface StuffService
    public Item remixItem (Item item)
        throws ServiceException
    {
        if (item.sourceId == 0) {
            // it's an original being remixed, it's the same as updateItem
            updateItem(item);
            return item;

        } else {
            return remixClone(item.getIdent(), item);
        }
    }

    // from interface StuffService
    public Item revertRemixedClone (ItemIdent itemIdent)
        throws ServiceException
    {
        return remixClone(itemIdent, null);
    }

    // from interface StuffService
    public String renameClone (ItemIdent itemIdent, String newName)
        throws ServiceException
    {
        if (newName != null) {
            newName = newName.trim();
            if (newName.length() > Item.MAX_NAME_LENGTH) {
                // this'll only happen with a hacked client
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        final String fname = newName;
        MemberRecord mrec = requireAuthedUser();
        ItemRecord rec = _itemLogic.editClone(mrec, itemIdent, new ItemLogic.CloneEditOp() {
            public void doOp (CloneRecord record, ItemRecord orig, ItemRepository<ItemRecord> repo)
                throws Exception
            {
                if (StringUtil.isBlank(fname) || fname.equals(orig.name)) {
                    record.name = null; // revert
                } else {
                    record.name = fname;
                }

                // save the updated info
                repo.updateCloneName(record);
            }
        });
        return rec.name;
    }

    // from interface StuffService
    public List<Item> loadInventory (int memberId, byte type, String query, int mogId)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        if (memrec.memberId != memberId && !memrec.isSupport()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        // make sure they supplied a valid item type
        if (Item.getClassForType(type) == null) {
            log.warning("Requested to load inventory for invalid item type",
                "who", who(memrec), "type", type);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        List<Item> items = Lists.newArrayList(Lists.transform(
            _itemLogic.getRepository(type).findItems(memberId, query, mogId),
            new ItemRecord.ToItem<Item>()));

        Collections.sort(items);
        return items;
    }

    // from interface StuffService
    public Item loadItem (ItemIdent item)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(item.type);
        ItemRecord irec = repo.loadItem(item.itemId);
        // we only return the item metadata if they own it, it's a catalog master, or for agents
        boolean accessValid = (irec != null) &&
            ((irec.ownerId == mrec.memberId) || irec.isCatalogMaster() || mrec.isSupport());
        return accessValid ? irec.toItem() : null;
    }

    // from interface StuffService
    public DetailOrIdent loadItemDetail (final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);

        ItemRecord record = repo.loadItem(iident.itemId);
        if (record == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        // if you're not the owner or support+, you cannot view original items
        if (record.ownerId != 0 && record.itemId > 0 &&
            (mrec == null || (!mrec.isSupport() && mrec.memberId != record.ownerId))) {
            // if it's listed, send them to the catalog
            if (record.catalogId != 0) {
                return new DetailOrIdent(null, new ItemIdent(iident.type, record.catalogId));
            } else {
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED); // fall back to error
            }
        }

        ItemDetail detail = new ItemDetail();
        detail.item = record.toItem();
        detail.creator = ((mrec != null) && (record.creatorId == mrec.memberId)) ?
            mrec.getName() : // shortcut for items we created
            _memberRepo.loadMemberName(record.creatorId); // normal lookup
        detail.memberItemInfo = _itemLogic.getMemberItemInfo(mrec, detail.item);
        switch (detail.item.used) {
        case FURNITURE:
        case PET:
        case BACKGROUND:
            detail.useLocation = _sceneRepo.identifyScene(detail.item.location);
            break;
        }
        List<TagNameRecord> trecs = repo.getTagRepository().getTags(iident.itemId);
        detail.tags = Lists.newArrayList(Iterables.transform(trecs, TagNameRecord.TO_TAG));
        // for entity types: try loading up their memory
        if (-1 != ArrayUtil.indexOf(Item.ENTITY_TYPES, iident.type)) {
            MemoriesRecord memories = _memoryRepo.loadMemory(iident.type, iident.itemId);
            if (memories != null) {
                detail.memories = memories.toBase64();
            }
        }

        return new DetailOrIdent(detail, null);
    }

    // from interface StuffService
    public void deleteItem (final ItemIdent iident)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        _itemLogic.deleteItem(memrec, iident);
    }

    /**
     * Helper method for remixItem and revertRemixedClone.
     * @param item the updated item, or null to revert to the original mix.
     */
    protected Item remixClone (ItemIdent itemIdent, final Item item)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure the item isn't boochy
        if (item != null && !item.isConsistent()) {
            log.warning("Requested to remix item with invalid version",
               "who", who(mrec), "item", item);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        ItemRecord rec = _itemLogic.editClone(mrec, itemIdent, new ItemLogic.CloneEditOp() {
            public void doOp (CloneRecord record, ItemRecord orig, ItemRepository<ItemRecord> repo)
                throws Exception
            {
                if (item == null) {
                    record.mediaHash = null; // we're reverting

                } else {
                    // in all probability, the primary media is different now
                    MediaDesc primary = item.getPrimaryMedia();
                    byte[] primaryHash = (primary == null) ? null : primary.hash;
                    if (Arrays.equals(primaryHash, orig.getPrimaryMedia())) {
                        record.mediaHash = null; // a revert here, strange, but ok
                    } else {
                        record.mediaHash = primaryHash;
                    }
                }

                // save the updated info
                repo.updateCloneMedia(record);
            }
        });
        return rec.toItem();
    }

    // our dependencies
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected StatLogic _statLogic;
}
