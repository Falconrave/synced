//
// $Id$

package com.threerings.msoy.admin.server;

import static com.threerings.msoy.Log.log;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.servlet.util.ServiceWaiter;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;
import com.samskivert.util.Invoker.Unit;

import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.room.server.MsoySceneRegistry;
import com.threerings.msoy.server.BureauManager;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.CharityRecord;
import com.threerings.msoy.server.persist.ContestRecord;
import com.threerings.msoy.server.persist.ContestRepository;
import com.threerings.msoy.server.persist.MemberInviteStatusRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.PromotionRecord;
import com.threerings.msoy.server.persist.PromotionRepository;

import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.Promotion;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.ServletWaiter;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.CloneRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRecord;
import com.threerings.msoy.item.server.persist.ItemFlagRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.gwt.BroadcastHistory;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.money.server.persist.BroadcastHistoryRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;

import com.threerings.msoy.admin.data.MsoyAdminCodes;
import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.admin.gwt.MemberAdminInfo;
import com.threerings.msoy.admin.gwt.MemberInviteResult;
import com.threerings.msoy.admin.gwt.MemberInviteStatus;
import com.threerings.msoy.admin.gwt.StatsModel;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.data.all.CharityInfo;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager.NodeAction;

/**
 * Provides the server implementation of {@link AdminService}.
 */
public class AdminServlet extends MsoyServiceServlet
    implements AdminService
{
    // from interface AdminService
    public void grantInvitations (final int numberInvitations, final int memberId)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdminUser();
        _memberRepo.grantInvites(memberId, numberInvitations);
        sendGotInvitesMail(memrec.memberId, memberId, numberInvitations);
    }

    // from interface AdminService
    public MemberAdminInfo getMemberInfo (final int memberId)
        throws ServiceException
    {
        requireSupportUser();

        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            return null;
        }

        final MemberMoney money = _moneyLogic.getMoneyFor(memberId);
        final MemberAdminInfo info = new MemberAdminInfo();
        info.name = tgtrec.getName();
        info.accountName = tgtrec.accountName;
        info.permaName = tgtrec.permaName;
        if (tgtrec.isSet(MemberRecord.Flag.MAINTAINER)) {
            info.role = WebCreds.Role.MAINTAINER;
        } else if (tgtrec.isSet(MemberRecord.Flag.ADMIN)) {
            info.role = WebCreds.Role.ADMIN;
        } else if (tgtrec.isSet(MemberRecord.Flag.SUPPORT)) {
            info.role = WebCreds.Role.SUPPORT;
        } else if (tgtrec.isSet(MemberRecord.Flag.VALIDATED)) {
            info.role = WebCreds.Role.VALIDATED;
        } else {
            info.role = WebCreds.Role.REGISTERED;
        }
        info.flow = money.coins;
        info.accFlow = (int)money.accCoins;
        info.gold = money.bars;
        info.sessions = tgtrec.sessions;
        info.sessionMinutes = tgtrec.sessionMinutes;
        if (tgtrec.lastSession != null) {
            info.lastSession = new Date(tgtrec.lastSession.getTime());
        }
        info.humanity = tgtrec.humanity;
        if (tgtrec.affiliateMemberId != 0) {
            info.affiliate = _memberRepo.loadMemberName(tgtrec.affiliateMemberId);
        }
        // TODO: this isn't quite right.
        info.affiliateOf = _memberRepo.loadMembersInvitedBy(memberId);

        // Check if this member is set as a charity.
        CharityRecord charity = _memberRepo.getCharityRecord(memberId);
        if (charity == null) {
            info.charity = false;
            info.coreCharity = false;
            info.charityDescription = "";
        } else {
            info.charity = true;
            info.coreCharity = charity.core;
            info.charityDescription = charity.description;
        }
        return info;
    }

    // from interface AdminService
    public MemberInviteResult getPlayerList (final int inviterId)
        throws ServiceException
    {
        requireSupportUser();

        final MemberInviteResult res = new MemberInviteResult();
        final MemberRecord memRec = inviterId == 0 ? null : _memberRepo.loadMember(inviterId);
        if (memRec != null) {
            res.name = memRec.permaName == null || memRec.permaName.equals("") ?
                memRec.name : memRec.permaName;
            res.memberId = inviterId;
            res.invitingFriendId = memRec.affiliateMemberId;
        }

        final List<MemberInviteStatus> players = Lists.newArrayList();
        for (final MemberInviteStatusRecord rec : _memberRepo.getMembersInvitedBy(inviterId)) {
            players.add(rec.toWebObject());
        }
        res.invitees = players;
        return res;
    }

    // from interface AdminService
    public void setRole (int memberId, WebCreds.Role role)
        throws ServiceException
    {
        final MemberRecord memrec = requireAdminUser();
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            return;
        }

        // log this as a warning so that it shows up in the nightly filtered logs
        log.warning("Configuring role", "setter", memrec.who(), "target", tgtrec.who(),
                    "role", role);
        tgtrec.setFlag(MemberRecord.Flag.VALIDATED, role != WebCreds.Role.REGISTERED);
        tgtrec.setFlag(MemberRecord.Flag.SUPPORT, role == WebCreds.Role.SUPPORT);
        if (memrec.isMaintainer()) {
            tgtrec.setFlag(MemberRecord.Flag.ADMIN, role == WebCreds.Role.ADMIN);
        }
        if (memrec.isRoot()) {
            tgtrec.setFlag(MemberRecord.Flag.MAINTAINER, role == WebCreds.Role.MAINTAINER);
        }
        _memberRepo.storeFlags(tgtrec);
    }

    // from interface AdminService
    public void setDisplayName (int memberId, String name)
        throws ServiceException
    {
        final MemberRecord memrec = requireSupportUser();
        final MemberRecord tgtrec = _memberRepo.loadMember(memberId);
        if (tgtrec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        _memberLogic.setDisplayName(tgtrec.memberId, name, tgtrec.isSupport());
        // log this as a warning so that it shows up in the nightly filtered logs
        log.warning("Set display name", "setter", memrec.who(), "target", tgtrec.who(),
                    "name", name);
    }

    // from interface AdminService
    public List<ABTest> getABTests ()
        throws ServiceException
    {
        List<ABTestRecord> records = _testRepo.loadTests();
        final List<ABTest> tests = Lists.newArrayList();
        for (final ABTestRecord record : records) {
            final ABTest test = record.toABTest();
            tests.add(test);
        }
        return tests;
    }

    // from interface AdminService
    public void createTest (final ABTest test)
        throws ServiceException
    {
        // make sure there isn't already a test with this name
        if (_testRepo.loadTestByName(test.name) != null) {
            throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
        }
        _testRepo.insertABTest(test);
    }

    // from interface AdminService
    public void updateTest (final ABTest test)
        throws ServiceException
    {
        // make sure there isn't already a test with this name
        final ABTestRecord existingTest = _testRepo.loadTestByName(test.name);
        if (existingTest != null && existingTest.abTestId != test.abTestId) {
            throw new ServiceException(MsoyAdminCodes.E_AB_TEST_DUPLICATE_NAME);
        }
        _testRepo.updateABTest(test);
    }

    // from interface AdminService
    public ItemFlagsResult getItemFlags (int start, int count, boolean needCount)
        throws ServiceException
    {
        requireSupportUser();
        ItemFlagsResult result = new ItemFlagsResult();

        // get the total if needed
        if (needCount) {
            result.total = _itemFlagRepo.countItemFlags();
        }

        // get the page of item flags
        result.page = Lists.newArrayList(Iterables.transform(_itemFlagRepo.loadFlags(start, count),
            new Function<ItemFlagRecord, ItemFlag>() {
                public ItemFlag apply (ItemFlagRecord rec) {
                    return rec.toItemFlag();
                }
            }));

        // collect all ids by type that we require
        Map<Byte, Set<Integer>> itemsToLoad = Maps.newHashMap();
        for (ItemFlag flag : result.page) {
            Set<Integer> itemIds = itemsToLoad.get(flag.itemIdent.type);
            if (itemIds == null) {
                itemsToLoad.put(flag.itemIdent.type, itemIds = Sets.newHashSet());
            }
            itemIds.add(flag.itemIdent.itemId);
        }

        // load items and stash by ident, also grab the creator id
        result.items = Maps.newHashMap();
        Set<Integer> memberIds = Sets.newHashSet();
        for (Map.Entry<Byte, Set<Integer>> ee : itemsToLoad.entrySet()) {
            for (ItemRecord rec : _itemLogic.getRepository(ee.getKey()).loadItems(ee.getValue())) {
                ItemDetail detail = new ItemDetail();
                detail.item = rec.toItem();
                result.items.put(detail.item.getIdent(), detail);
                memberIds.add(detail.item.creatorId);
            }
        }

        // now add all the reporters
        for (ItemFlag flag : result.page) {
            memberIds.add(flag.memberId);
        }

        // resolve and store names for display
        result.memberNames = Maps.newHashMap();
        result.memberNames.putAll(_memberRepo.loadMemberNames(memberIds));

        // backfill the creator names
        for (ItemDetail detail : result.items.values()) {
            detail.creator = result.memberNames.get(detail.item.creatorId);
        }

        return result;
    }

    // from interface AdminService
    public ItemTransactionResult getItemTransactions (
        ItemIdent iident, int from, int count, boolean needCount)
        throws ServiceException
    {
        requireSupportUser();

        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(iident.type);
        final ItemRecord item = repo.loadOriginalItem(iident.itemId);

        if (item == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }
        ItemTransactionResult result = new ItemTransactionResult();
        if (needCount) {
            result.total = _moneyLogic.getItemTransactionCount(iident);
        }
        result.page = _moneyLogic.getItemTransactions(iident, from, count, false);

        Set<Integer> memberIds = Sets.newHashSet();
        for (MoneyTransaction tx : result.page) {
            memberIds.add(tx.memberId);
        }
        result.memberNames = Maps.newHashMap();
        result.memberNames.putAll(_memberRepo.loadMemberNames(memberIds));
        return result;
    }

    // from interface AdminService
    public ItemDeletionResult deleteItemAdmin (
        final ItemIdent iident, final String subject, final String body)
        throws ServiceException
    {
        final MemberRecord memrec = requireSupportUser();

        log.info("Deleting item for admin", "who", memrec.accountName, "item", iident,
            "subject", subject);

        final byte type = iident.type;
        final ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        final ItemRecord item = repo.loadOriginalItem(iident.itemId);

        if (item == null) {
            throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
        }

        final IntSet owners = new ArrayIntSet();

        ItemDeletionResult result = new ItemDeletionResult();
        owners.add(item.creatorId);

        // find the catalog record and remove it, load original if any
        ItemRecord original = null;
        if (item.catalogId != 0) {
            CatalogRecord catrec = repo.loadListing(item.catalogId, true);
            if (catrec != null) {
                if (catrec.listedItemId != item.itemId) {
                    log.warning("Catalog record doesn't match item", "itemId", item.itemId,
                        "listedItemId", catrec.listedItemId);

                } else {
                    _itemLogic.removeListing(memrec, type, item.catalogId);
                    if (catrec.originalItemId != item.itemId) {
                        original = repo.loadOriginalItem(catrec.originalItemId);
                        if (original == null) {
                            log.warning("Could not load original item",
                                "id", catrec.originalItemId);
                        }
                    }
                }
            }
        }

        // reclaim the item and all its copies from scenes
        ItemReclaimer reclaimer = new ItemReclaimer(iident, memrec.memberId);
        reclaimer.addItem(item);
        if (original != null) {
            reclaimer.addItem(original);
        }
        for (final CloneRecord record : repo.loadCloneRecords(item.itemId)) {
            reclaimer.addItem(repo.loadItem(record.itemId));
        }
        reclaimer.reclaim();

        // TODO: depending on frequency of errors here, we may want to provide details on errors
        result.reclaimCount += reclaimer.succeeded;
        result.reclaimErrors += reclaimer.failed;

        // TODO: what about memories? tags? comments?
        // TODO: we need some intermediate logic classes. e.g. items will be deleted elsewhere too

        // delete the clones and add each to the reclaimer
        for (final CloneRecord record : repo.loadCloneRecords(item.itemId)) {
            repo.deleteItem(record.itemId);
            result.deletionCount ++;
            owners.add(record.ownerId);
        }

        // finally delete the actual item
        repo.deleteItem(item.itemId);
        result.deletionCount ++;

        // ... and the owner's original
        if (original != null) {
            repo.deleteItem(original.itemId);
            result.deletionCount ++;
        }

        // notify the owners of the deletion
        for (final int ownerId : owners) {
            if (ownerId == memrec.memberId) {
                continue; // admin deleting their own item? sure, whatever!
            }
            final MemberRecord owner = _memberRepo.loadMember(ownerId);
            if (owner != null) {
                _mailLogic.startBulkConversation(memrec, owner, subject, body, null);
            }
        }

        // now do the refunds
        result.refunds += _moneyLogic.refundAllItemPurchases(new ItemIdent(
            type, item.itemId), item.name);

        return result;
    }

    // from interface AdminService
    public BureauLauncherInfo[] getBureauLauncherInfo ()
        throws ServiceException
    {
        final ServletWaiter<BureauLauncherInfo[]> waiter =
            new ServletWaiter<BureauLauncherInfo[]>("getBureauLauncherInfo");

        _omgr.postRunnable(new Runnable() {
            public void run () {
                _bureauMgr.getBureauLauncherInfo(waiter);
            }
        });

        return waiter.waitForResult();
    }

    // from interface AdminService
    public List<Promotion> loadPromotions ()
        throws ServiceException
    {
        requireSupportUser();
        return Lists.newArrayList(
            Lists.transform(_promoRepo.loadPromotions(), PromotionRecord.TO_PROMOTION));
    }

    // from interface AdminService
    public void addPromotion (Promotion promo)
        throws ServiceException
    {
        requireSupportUser();
        _promoRepo.addPromotion(promo);
    }

    public void updatePromotion (Promotion promo)
        throws ServiceException
    {
        requireSupportUser();
        _promoRepo.updatePromotion(promo);
    }

    // from interface AdminService
    public void deletePromotion (String promoId)
        throws ServiceException
    {
        requireSupportUser();
        _promoRepo.deletePromotion(promoId);
    }

    // from interface AdminService
    public List<Contest> loadContests ()
        throws ServiceException
    {
        requireSupportUser();
        return Lists.newArrayList(Lists.transform(_contestRepo.loadContests(),
            ContestRecord.TO_CONTEST));
    }

    // from interface AdminService
    public void addContest (Contest contest)
        throws ServiceException
    {
        requireSupportUser();
        _contestRepo.addContest(contest);
    }

    // from interface AdminService
    public void updateContest (Contest contest)
        throws ServiceException
    {
        requireSupportUser();
        _contestRepo.updateContest(contest);
    }

    // from interface AdminService
    public void deleteContest (String contestId)
        throws ServiceException
    {
        requireSupportUser();
        _contestRepo.deleteContest(contestId);
    }

    // from interface AdminService
    public StatsModel getStatsModel (StatsModel.Type type)
        throws ServiceException
    {
        requireSupportUser();
        try {
            return _adminMgr.compilePeerStatistics(type).get();
        } catch (InterruptedException ie) {
            log.warning("Stats compilation timed out", "type", type, "error", ie);
            throw new ServiceException(MsoyAdminCodes.E_INTERNAL_ERROR);
        } catch (Exception e) {
            log.warning("Stats compilation failed", "type", type, e);
            throw new ServiceException(MsoyAdminCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface AdminService
    public void setCharityInfo (CharityInfo charityInfo)
        throws ServiceException
    {
        requireSupportUser();

        // Save or delete charity record depending on value of 'charity.
        CharityRecord charityRec = new CharityRecord(charityInfo.memberId, charityInfo.core,
            charityInfo.description);
        _memberRepo.saveCharity(charityRec);
    }

    // from interface AdminService
    public void removeCharityStatus (int memberId)
        throws ServiceException
    {
        requireSupportUser();

        _memberRepo.deleteCharity(memberId);
    }

    // from interface AdminService
    public Set<String> getPeerNodeNames ()
        throws ServiceException
    {
        requireSupportUser();

        // Collect the names of all the nodes
        final Set<String> names = Sets.newHashSet();
        _peerMgr.applyToNodes(new Function<NodeObject, Void>() {
            public Void apply (NodeObject node) {
                names.add(node.nodeName);
                return null;
            }
        });

        return names;
    }

    // from interface AdminService
    public void scheduleReboot (int minutes, final String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAdminUser();
        final long time = System.currentTimeMillis() + minutes * 60 * 1000;
        final String initiator = mrec.name + " (" + mrec.memberId + ")";
        _omgr.postRunnable(new Runnable() {
            public void run () {
                // these fields need to be set for the correct logging and email information
                // to get picked up by our admin manager
                _runtimeConfig.server.setServletRebootInitiator(initiator);
                _runtimeConfig.server.setServletReboot(time);
                _runtimeConfig.server.setServletRebootNode(_peerMgr.getNodeObject().nodeName);
                _runtimeConfig.server.setCustomRebootMsg(message);

                // this actually triggers the reboot
                _runtimeConfig.server.setNextReboot(time);
            }
        });
    }

    public void restartPanopticon (Set<String> nodeNames)
        throws ServiceException
    {
        requireAdminUser();

        for (String node : nodeNames) {
            _peerMgr.invokeNodeAction(node, new RestartPanopticonAction());
        }
    }

    // from AdminService
    public BroadcastHistoryResult getBroadcastHistory (int offset, int count, boolean needCount)
    {
        BroadcastHistoryResult result = new BroadcastHistoryResult();

        // load count if needed
        if (needCount) {
            result.total = _moneyRepo.countBroadcastHistoryRecords();
        }

        // transform and add results
        result.page = Lists.newArrayList();
        result.page.addAll(Lists.transform(_moneyRepo.getBroadcastHistoryRecords(offset, count),
            new Function<BroadcastHistoryRecord, BroadcastHistory>() {
                public BroadcastHistory apply (BroadcastHistoryRecord rec) {
                    return rec.toBroadcastHistory();
                }
            }));

        // resolve and store names for display
        Set<Integer> memberIds = Sets.newHashSet();
        for (BroadcastHistory bh : result.page) {
            memberIds.add(bh.memberId);
        }
        result.memberNames = Maps.newHashMap();
        result.memberNames.putAll(_memberRepo.loadMemberNames(memberIds));
        return result;
    }

    protected void sendGotInvitesMail (final int senderId, final int recipientId, final int number)
    {
        final String subject = _serverMsgs.getBundle("server").get("m.got_invites_subject", number);
        final String body = _serverMsgs.getBundle("server").get("m.got_invites_body", number);
        _mailRepo.startConversation(recipientId, senderId, subject, body, null, true, true);
    }

    protected static class RestartPanopticonAction extends NodeAction
    {
        public RestartPanopticonAction () {}

        @Override
        protected void execute ()
        {
            // Restart the logger on this node and all game servers attached to this node.
            // Restarting is a blocking operation, so run it on the invoker.
            _invoker.postUnit(new Unit() {
                @Override public boolean invoke () {
                    _nodeLogger.restart();
                    return false;
                }
            });
        }

        @Override
        public boolean isApplicable (NodeObject nodeobj)
        {
            // This will automatically go to the node we want.
            return true;
        }

        @Inject protected transient MsoyEventLogger _nodeLogger;
        @Inject protected transient @MainInvoker Invoker _invoker;
    }

    /**
     * Manages the reclamation of all items or item clones from scenes in which they are used. Must
     * be posted to the domgr thread. Provides a means of waiting for all reclamations to finish by
     * extending the waiter.
     */
    protected class ItemReclaimer extends ServiceWaiter<Void>
        implements Runnable
    {
        /** Number of successful reclamations. */
        public int succeeded;

        /** Number of failed reclamations. */
        public int failed;

        /**
         * Creates a new reclaimer.
         */
        public ItemReclaimer (ItemIdent original, int memberId)
        {
            super(60); // one minute timeout... this could take a while
            _memberId = memberId;
            _original = original;
        }

        /**
         * Adds an item to be reclaimed. If the item is not in use, does nothing.
         */
        public void addItem (ItemRecord item)
        {
            // only if it's used in a room
            if ((item.location == 0) ||
                    (-1 == ArrayUtil.indexOf(Item.ROOM_TYPES, item.getType()))) {
                return;
            }

            ItemIdent ident = new ItemIdent(item.getType(), item.itemId);
            _items.add(Tuple.newTuple(item.location, ident));
        }

        /**
         * Posts all reclamations and waits for them to complete. Throws a service exception if
         * this takes longer than a minute.
         */
        public void reclaim ()
            throws ServiceException
        {
            _omgr.postRunnable(this);
            try {
                waitForResponse();

            } catch (ServiceWaiter.TimeoutException te) {
                log.warning("Timeout occurred while reclaiming items", "remaining", _items.size());
                throw new ServiceException("A timeout occurred while reclaiming items. Please " +
                    "wait a few minutes and try again.");
            }

            // item usage update units are not waited upon, so flush invoker queue
            ServletWaiter.queueAndWait(_invoker, "reclaimFlush", new Callable<Void> () {
                public Void call () {
                    return null;
                }
            });
        }

        // from Runnable
        public void run ()
        {
            log.info("Starting reclamation for item deletion", "original", _original,
                "locations", _items.size());

            for (final Tuple<Integer, ItemIdent> item : Lists.newArrayList(_items)) {
                ResultListener<Void> lner = new ResultListener<Void>() {
                    public void requestCompleted (Void result) {
                        finishedOne(item, true);
                    }

                    public void requestFailed (Exception cause) {
                        finishedOne(item, false);
                    }
                };
                _sceneReg.reclaimItem(item.left, _memberId, item.right, lner);
            }

            checkFinished();
        }

        /**
         * Marks the given item as finished with the given success status. If all items are
         * finished, posts the result to the waiter can stop.
         */
        protected void finishedOne (Tuple<Integer, ItemIdent> item, boolean success)
        {
            if (_items.remove(item)) {
                if (success) {
                    succeeded++;
                } else {
                    failed++;
                }
                checkFinished();
            } else {
                log.warning("Finished reclamation for item deletion", "item", item);
            }
        }

        protected void checkFinished ()
        {
            if (_items.size() == 0) {
                log.info("Finished reclaiming items", "original", _original,
                    "succeeded", succeeded, "failed", failed);
                postSuccess(null);
            }
        }

        protected int _memberId;
        protected ItemIdent _original;
        protected Set<Tuple<Integer, ItemIdent>> _items = Sets.newHashSet();
    }

    // our dependencies
    @Inject @MainInvoker Invoker _invoker;
    @Inject protected ABTestRepository _testRepo;
    @Inject protected BureauManager _bureauMgr;
    @Inject protected ContestRepository _contestRepo;
    @Inject protected ItemFlagRepository _itemFlagRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MailLogic _mailLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
    @Inject protected MsoyAdminManager _adminMgr;
    @Inject protected MsoyEventLogger _eventLogger;
    @Inject protected MsoyPeerManager _peerMgr;
    @Inject protected MsoySceneRegistry _sceneReg;
    @Inject protected PromotionRepository _promoRepo;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected RuntimeConfig _runtimeConfig;
    @Inject protected ServerMessages _serverMsgs;
}
