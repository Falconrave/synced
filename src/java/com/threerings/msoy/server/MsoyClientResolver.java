//
// $Id$

package com.threerings.msoy.server;

import static com.threerings.msoy.Log.log;

import java.util.List;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.Tuple;

import com.threerings.io.Streamable;

import com.threerings.util.StreamableArrayIntSet;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.ClientLocal;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.web.gwt.MemberCard;

import com.threerings.msoy.badge.data.EarnedBadgeSet;
import com.threerings.msoy.badge.data.InProgressBadgeSet;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.badge.server.ServerStatSet;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.AvatarRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;

/**
 * Used to configure msoy-specific client object data.
 */
public class MsoyClientResolver extends CrowdClientResolver
{
    @Override
    public ClientObject createClientObject ()
    {
        // see if we have a member object forwarded from our peer
        _fwddata = _peerMan.getForwardedMemberObject(_username);
        if (_fwddata == null) {
            return new MemberObject();
        } else {
            return _fwddata.left;
        }
    }

    @Override
    public ClientLocal createLocalAttribute ()
    {
        return new MemberLocal();
    }

    @Override // from ClientResolver
    protected void resolveClientData (final ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        // copy our forwarded local attributes
        if (_fwddata != null) {
            for (Streamable local : _fwddata.right) {
                @SuppressWarnings("unchecked") Class<Streamable> lclass =
                    (Class<Streamable>)local.getClass();
                clobj.setLocal(lclass, null); // delete any stock local
                clobj.setLocal(lclass, local); // configure our forwarded data
            }
        }

        final MemberObject memobj = (MemberObject) clobj;
        MemberLocal local = memobj.getLocal(MemberLocal.class);

        // create a deferred notifications array so that we can track any notifications dispatched
        // to this client until they're ready to read them; we'd have NotificationManager do this
        // in a MemberLocator.Observer but we need to be sure this is filled in before any other
        // MemberLocator.Observers are notified because that's precisely when early notifications
        // are likely to be generated
        local.deferredNotifications = Lists.newArrayList();

        // do some stats-related hackery
        if (local.stats instanceof ServerStatSet) {
            ((ServerStatSet)local.stats).init(_badgeMan, memobj);
        }

        // if our member object was forwarded from another server, it will already be fully ready
        // to go so we can avoid the expensive resolution process
        if (memobj.memberName != null) {
            log.debug("Resolved forwarded session", "clobj", clobj.who());
            return;
        }

        // guests have MemberName as an auth username, members have Name
        if (_username instanceof MemberName) {
            // our auth username has our assigned name and member id, so use those
            final MemberName aname = (MemberName)_username;
            memobj.memberName = new VizMemberName(
                aname.toString(), aname.getMemberId(), MemberCard.DEFAULT_PHOTO);
            local.stats = new StatSet();
            local.badges = new EarnedBadgeSet();
            local.friendIds = new StreamableArrayIntSet(0);
            local.inProgressBadges = new InProgressBadgeSet();

        } else if (_username instanceof LurkerName) {
            // we are lurker, we have no visible name to speak of
            memobj.memberName = new VizMemberName("", 0, MemberCard.DEFAULT_PHOTO);
            local.stats = new StatSet();
            local.badges = new EarnedBadgeSet();
            local.friendIds = new StreamableArrayIntSet(0);
            local.inProgressBadges = new InProgressBadgeSet();

        } else {
            resolveMember(memobj);
        }

        log.debug("Resolved unforwarded session", "clobj", clobj.who());
    }

    /**
     * Resolve a msoy member. This is called on the invoker thread.
     */
    protected void resolveMember (final MemberObject memobj)
        throws Exception
    {
//        long startStamp = System.currentTimeMillis();
//        List<Long> resolutionStamps = Lists.newArrayList();
        enforceConnected();

        // load up their member information using on their authentication (account) name
        final MemberRecord member = _memberRepo.loadMember(_username.toString());
        if (member == null) {
            throw new Exception("Missing member record for authenticated member? " +
                                "[username=" + _username + "]");
        }

//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        final MemberMoney money = _moneyLogic.getMoneyFor(member.memberId);

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // we need their profile photo to create the member name
//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        final ProfileRecord precord = _profileRepo.loadProfile(member.memberId);
        memobj.memberName = new VizMemberName(
            member.name, member.memberId,
            (precord == null) ? MemberCard.DEFAULT_PHOTO : precord.getPhoto());
        if (precord != null) {
            memobj.headline = precord.headline;
        }

        // configure various bits directly from their member record
        memobj.homeSceneId = member.homeSceneId;
        memobj.coins = money.coins;
        memobj.accCoins = (int) money.accCoins; // TODO: long? int?
        memobj.bars = money.bars;
        memobj.level = member.level;

        // load up this member's persistent stats
        MemberLocal local = memobj.getLocal(MemberLocal.class);
//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        final List<Stat> stats = _statRepo.loadStats(member.memberId);
        local.stats = new ServerStatSet(stats.iterator(), _badgeMan, memobj);

        // and their mutelist
        int[] muted = _memberRepo.loadMutelist(member.memberId);
        if (muted.length > 0) {
            local.mutedMemberIds = muted;
        }

        // and their badges
        local.badgesVersion = member.badgesVersion;
//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        local.badges = new EarnedBadgeSet(
            Iterables.transform(_badgeRepo.loadEarnedBadges(member.memberId),
                                EarnedBadgeRecord.TO_BADGE));
//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        local.inProgressBadges = new InProgressBadgeSet(
            Iterables.transform(_badgeRepo.loadInProgressBadges(member.memberId),
                                InProgressBadgeRecord.TO_BADGE));

//        // load up any item lists they may have
//        List<ItemListInfo> itemLists = _itemMan.getItemLists(member.memberId);
//        memobj.lists = new DSet<ItemListInfo>(itemLists);

// TEMP: flow evaporation is disabled; we need to think more about this
//         // calculate flow evaporation since last logon
//         int dT = (int) ((System.currentTimeMillis() - member.lastSession.getTime()) / 60000);
//         _memberRepo.getFlowRepository().expireFlow(member, dT); // modifies member.flow
// END TEMP

        // fill in this member's raw friends list; the friend manager will update it later
//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        local.friendIds = _memberRepo.loadFriendIds(member.memberId);

        // load up this member's group memberships
//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        memobj.groups = new DSet<GroupMembership>(
            // we don't pass in member name here because we don't need it on the client
            _groupRepo.resolveGroupMemberships(member.memberId, null).iterator());

        // load up this member's current new mail count
//        resolutionStamps.add(System.currentTimeMillis() - startStamp);
        enforceConnected();
        memobj.newMailCount = _mailRepo.loadUnreadConvoCount(member.memberId);

        // load up their selected avatar, we'll configure it later
        if (member.avatarId != 0) {
//            resolutionStamps.add(System.currentTimeMillis() - startStamp);
            enforceConnected();
            final AvatarRecord avatar = _itemLogic.getAvatarRepository().loadItem(member.avatarId);
            if (avatar != null) {
                memobj.avatar = (Avatar)avatar.toItem();
                enforceConnected();
                MemoriesRecord memrec = _memoryRepo.loadMemory(avatar.getType(), avatar.itemId);
                local.memories = (memrec == null) ? null : memrec.toEntry();
            }
        }

        // for players, resolve this here from the database.
        // guests will get resolution later on, in MsoySession.sessionWillStart()
        memobj.visitorInfo = new VisitorInfo(member.visitorId, true);

        // Load up the member's experiences
        //resolutionStamps.add(System.currentTimeMillis() - startStamp);
        //enforceConnected();
        //memobj.experiences = new DSet<MemberExperience>(
        //        _memberLogic.getExperiences(member.memberId));

//         log.info("Client resolution complete", "memberId", member.memberId,
//             "timing", resolutionStamps);
    }

    @Override // from ClientResolver
    protected void finishResolution (ClientObject clobj)
    {
        super.finishResolution(clobj);

        MemberObject user = (MemberObject)clobj;

        // resolve this user's party info
        user.setParty(_peerMan.getPartySummary(user.getMemberId()));
    }

    /** Info on our member object forwarded from another server. */
    protected Tuple<MemberObject,Streamable[]> _fwddata;

    // dependencies
    @Inject protected BadgeManager _badgeMan;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ItemManager _itemMan;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected StatRepository _statRepo;
}
