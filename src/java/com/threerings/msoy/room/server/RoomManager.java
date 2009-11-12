//
// $Id$

package com.threerings.msoy.room.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.text.MessageUtil;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Comparators;
import com.samskivert.util.ComplainingListener;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.QuickSort;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Throttle;

import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.ProxySubscriber;
import com.threerings.presents.dobj.SetListener;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ConfirmAdapter;
import com.threerings.presents.util.IgnoreConfirmAdapter;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.server.CrowdObjectAccess;
import com.threerings.crowd.server.LocationManager;

import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.whirled.bureau.data.BureauTypes;

import com.whirled.game.data.PropertySetEvent;
import com.whirled.game.data.PropertySpaceObject.PropertySetException;
import com.whirled.game.server.PropertySpaceDispatcher;
import com.whirled.game.server.PropertySpaceHandler;
import com.whirled.game.server.PropertySpaceHelper;
import com.whirled.game.server.WhirledGameMessageDispatcher;
import com.whirled.game.server.WhirledGameMessageHandler;

import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyBodyObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.BootablePlaceManager;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberLocator;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.bureau.data.WindowClientObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.AvatarRecord;

import com.threerings.msoy.party.server.PartyRegistry;

import com.threerings.msoy.room.client.RoomService;
import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.Controllable;
import com.threerings.msoy.room.data.ControllableEntity;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemories;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.FurniUpdate;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MobObject;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyPortal;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.ObserverInfo;
import com.threerings.msoy.room.data.PuppetName;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.data.RoomLocal;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.RoomPropertiesObject;
import com.threerings.msoy.room.data.SceneAttrsUpdate;
import com.threerings.msoy.room.data.SceneOwnershipUpdate;
import com.threerings.msoy.room.server.RoomExtras;
import com.threerings.msoy.room.server.persist.MemoriesRecord;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.RoomPropertyRecord;
import com.threerings.msoy.room.server.persist.SceneRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages a "Room".
 */
@EventThread
public class RoomManager extends SpotSceneManager
    implements RoomProvider, BootablePlaceManager
{
    /** Time a room is idle before being unloaded. This is more aggressive than the default. */
    public static long ROOM_IDLE_UNLOAD_PERIOD = 30 * 1000L;

    /**
     * Flush any modified memories contained within the specified Iterable.
     */
    public static void flushMemories (Invoker invoker, final MemoryRepository memoryRepo,
                                      Iterable<EntityMemories> entries)
    {
        if (entries == null) {
            return;
        }
        final List<MemoriesRecord> memrecs = Lists.newArrayList();
        for (EntityMemories entry : entries) {
            if (entry != null && entry.modified) {
                memrecs.add(new MemoriesRecord(entry));
            }
        }
        if (memrecs.size() > 0) {
            invoker.postUnit(new WriteOnlyUnit("storeMemories") {
                public void invokePersist () throws Exception {
                    memoryRepo.storeMemories(memrecs);
                }
            });
        }
    }

    /**
     * Forcibly change the location of the specified body.
     * @return null on success, or an error string.
     */
    public String changeLocation (BodyObject body, MsoyLocation loc)
    {
        try {
            handleChangeLoc(body, loc);
            return null;
        } catch (InvocationException ie) {
            return ie.getMessage(); // return the error string
        }
    }

    /**
     * Forcibly change the state of an actor.
     */
    public void setState (MsoyBodyObject actor, String state)
    {
        // update the state in their body object
        actor.actorState = state;
        // and in the occInfo
        setState(actor.getOid(), state);
    }

    /**
     * Part 2 of setting an actor's state. Also used if the actor has no body.
     */
    public void setState (int occupantOid, final String state)
    {
        // TODO: consider, instead of updating the whole dang occInfo, dispatching a custom event
        // that will update just the state and serve as the trigger event to usercode...
        updateOccupantInfo(occupantOid, new ActorInfo.Updater<ActorInfo>() {
            public boolean update (ActorInfo info) {
                if (ObjectUtil.equals(info.getState(), state)) {
                    return false; // if there was no change, we're done.
                }
                info.setState(state);
                return true;
            }
        });
    }

    @Override
    public String ratifyBodyEntry (BodyObject body)
    {
        MsoySceneModel model = (MsoySceneModel) _scene.getSceneModel();
        Set<Integer> friendIds = (body instanceof MemberObject)
            ? body.getLocal(MemberLocal.class).friendIds
            : null;
        // check to see if the scene permits access
        if (!((MsoyBodyObject) body).canEnterScene(
                model.sceneId, model.ownerId, model.ownerType, model.accessControl, friendIds)) {
            return InvocationCodes.E_ACCESS_DENIED; // TODO: better? "This room is friend only"
        }

        // if we have a bootlist, check against that
        if (_booted != null && (body instanceof MemberObject)) {
            MemberObject user = (MemberObject) body;
            if (_booted.contains(user.getMemberId()) && !user.tokens.isSupport()) {
                return "e.booted";
            }
        }

        return super.ratifyBodyEntry(body);
    }

    // from interface BootablePlaceManager
    public String bootFromPlace (MemberObject user, int booteeId)
    {
        // make sure this user has access to boot
        if (!canManage(user)) {
            return InvocationCodes.E_ACCESS_DENIED;
        }

        // let's look up the user they want to boot
        MemberObject bootee = _locator.lookupMember(booteeId);
        if ((bootee == null) || (bootee.location == null) ||
                (bootee.location.placeOid != _plobj.getOid())) {
            return "e.user_not_present";
        }

        // let's see if the user is another manager
        if (canManage(bootee)) {
            // send a little message to the bootee telling them about the attempt
            SpeakUtil.sendInfo(bootee, MsoyCodes.GENERAL_MSGS,
                MessageUtil.tcompose("m.boot_attempt_mgr", user.getVisibleName()));
            return MessageUtil.tcompose("e.cant_boot_mgr", bootee.getVisibleName());
        }
        // don't let guests get screwed over
        int bootSceneId = bootee.getHomeSceneId();
        if (bootSceneId == _scene.getId()) {
            return InvocationCodes.E_ACCESS_DENIED; // bah, we don't need a better msg
        }

        // success! add them to the boot list
        if (_booted == null) {
            _booted = new ArrayIntSet(1);
        }
        _booted.add(booteeId);

        // and boot them right now.
        SpeakUtil.sendInfo(bootee, MsoyCodes.GENERAL_MSGS,
            MessageUtil.tcompose("m.booted", _scene.getName()));
        _screg.moveBody(bootee, bootSceneId);
        SpeakUtil.sendFeedback(user, MsoyCodes.GENERAL_MSGS, "m.boot_success");

        return null; // indicates success
    }

    /**
     * Evict all the players in room and shut it down.
     */
    public void evictPlayersAndShutdown ()
    {
        // copy the occupant set as a player list, as occupancy is modified in the loop below
        List<MemberObject> players = Lists.newArrayList();
        for (OccupantInfo playerInfo : _roomObj.occupantInfo) {
            DObject body = _omgr.getObject(playerInfo.bodyOid);
            if (body instanceof MemberObject) {
                players.add((MemberObject) body);
            }
        }

        // now throw the players out
        for (MemberObject player : players) {
            SpeakUtil.sendInfo(player, MsoyCodes.GENERAL_MSGS,
                MessageUtil.tcompose("m.shutdown_evicted", _scene.getName()));
            int homeId = player.getHomeSceneId();
            if (homeId == _scene.getId()) {
                // a player's home room is being updated, send them to BNW instead?
                if (homeId == 1) {
                    // silly edge case: it's the BNW room being stamped, just evict them to nowhere
                    _locmgr.leaveOccupiedPlace(player);
                    continue;
                }
            }
            _screg.moveBody(player, player.getHomeSceneId());
        }

        // then immediately shut down the manager
        shutdown();
    }

    /**
     * Can the specified user manage this room.
     */
    public boolean canManage (MemberObject user)
    {
        return ((MsoyScene) _scene).canManage(user);
    }

    /**
     * Is the specified user a manager, and NOT support staff?
     */
    public boolean isStrictlyManager (MemberObject user)
    {
        return ((MsoyScene) _scene).canManage(user, false, null);
    }

    /**
     * Checks whether or not the calling user can manage, if so returns the user cast
     * to a MemberObject, throws an {@link InvocationException} if not.
     */
    public MemberObject requireManager (ClientObject caller)
        throws InvocationException
    {
        MemberObject member = (MemberObject) caller;
        if (!canManage(member)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }
        return member;
    }

    /**
     * Checks whether or not the calling user can bring pets into this room. Returns normally if
     * so, throws an {@link InvocationException} if not.
     */
    public void requireCanAddPet (MemberObject caller)
        throws InvocationException
    {
        if (!canManage(caller)) {
            throw new InvocationException(RoomCodes.E_CANNOT_ADD_PET);
        }
    }

    /**
     * Reclaims an item from this room.
     */
    public void reclaimItem (ItemIdent item, int memberId)
    {
        MsoyScene scene = (MsoyScene)_scene;

        if (item.type == Item.DECOR) {
            // replace the decor with defaults
            SceneAttrsUpdate update = new SceneAttrsUpdate();
            update.init(scene.getId(), scene.getVersion());
            update.name = scene.getName();
            update.accessControl = scene.getAccessControl();
            update.playlistControl = scene.getPlaylistControl();
            update.decor = MsoySceneModel.defaultMsoySceneModelDecor();
            update.entrance = ((MsoySceneModel)scene.getSceneModel()).entrance;
            update.noPuppet = ((MsoySceneModel)scene.getSceneModel()).noPuppet;
            doRoomUpdate(update, memberId, null);

        } else if (item.type == Item.AUDIO) {
            // TODO: remove from playlist? This is not ever called presently

        } else {
            // find the right furni and pull it out
            for (FurniData furni : scene.getFurni()) {
                if (item.equals(furni.getItemIdent())) {
                    FurniUpdate.Remove update = new FurniUpdate.Remove();
                    update.data = furni;
                    doRoomUpdate(update, memberId, null);
                    break;
                }
            }
        }
    }

    public void transferOwnership (byte ownerType, int ownerId, Name ownerName, boolean lockToOwner)
    {
        SceneOwnershipUpdate update = new SceneOwnershipUpdate();
        update.ownerType = ownerType;
        update.ownerId = ownerId;
        update.ownerName = ownerName;
        update.lockToOwner = lockToOwner;
        doRoomUpdate(update, 0, null);
    }

    public void occupantLeftAVRGame (MemberObject member)
    {
        // we don't care
    }

    public void occupantEnteredAVRGame (MemberObject member)
    {
        ensureAVRGamePropertySpace(member);
    }

    // documentation inherited from RoomProvider
    public void requestControl (ClientObject caller, ItemIdent item)
    {
        ensureEntityControl(caller, item, "requestControl");
        // TODO: throw invocationexception on failure?
    }

    // documentation inherited from RoomProvider
    public void sendSpriteMessage (ClientObject caller, ItemIdent item, String name, byte[] arg,
                                   boolean isAction)
    {
        // make sure the caller is in the room
        if (caller instanceof MemberObject) {
            MemberObject who = (MemberObject)caller;
            if (!_roomObj.occupants.contains(who.getOid())) {
                return;
            }
        }

        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (isAction && !ensureEntityControl(caller, item, "triggerAction")) {
            log.info("Dropping sprite message for lack of control", "who", caller.who(),
                "item", item, "name", name);
            return;
        }

        // dispatch this as a simple MessageEvent
        _roomObj.postMessage(RoomCodes.SPRITE_MESSAGE, item, name, arg, isAction);
    }

    // documentation inherited from RoomProvider
    public void sendSpriteSignal (ClientObject caller, String name, byte[] arg)
    {
        // Caller could be a WindowClientObject if coming from a thane client
        if (caller instanceof MemberObject) {
            // make sure the caller is in the room
            MemberObject who = (MemberObject)caller;
            if (!_roomObj.occupants.contains(who.getOid())) {
                return;
            }
        }

        // dispatch this as a simple MessageEvent
        _roomObj.postMessage(RoomCodes.SPRITE_SIGNAL, name, arg);
    }

    // documentation inherited from RoomProvider
    public void modifyPlaylist (
        ClientObject caller, int audioItemId, final boolean add,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        final MemberObject who = (MemberObject) caller;
        ItemIdent key = new ItemIdent(Item.AUDIO, audioItemId);
        Audio current = _roomObj.playlist.get(key);

        // see if it's already done!
        if (add == (current != null)) {
            listener.requestProcessed();
            return;
        }

        // removals are really straightforward
        if (!add) {
            // they just want to remove it, no problem as long as they own it or the room
            if (current.ownerId != who.getMemberId() && !canManage(who)) {
                throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
            }
            _roomObj.startTransaction();
            try {
                _roomObj.removeFromPlaylist(key);
                if (_roomObj.currentSongId == audioItemId) {
                    playNextSong(false);
                }
            } finally {
                _roomObj.commitTransaction();
            }
            if (!current.used.forAnything()) {
                listener.requestProcessed(); // if the item's not in use, we're done
                return;
            }
            if (current.location != _scene.getId()) {
                log.warning("Whoa? Room contains song in-use elsewhere",
                    "song", current);
                // but, clear it anyway...
            }
            _itemMan.updateItemUsage(Item.AUDIO, Item.UsedAs.BACKGROUND, who.getMemberId(),
                _scene.getId(), audioItemId, 0, new ConfirmAdapter(listener));
            return;
        }

        // now handle additions
        if ((((MsoyScene)getScene()).getPlaylistControl() != MsoySceneModel.ACCESS_EVERYONE) &&
                !canManage(who)) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }
        if (_roomObj.playlist.size() >= MAX_PLAYLIST_SIZE) {
            throw new InvocationException("e.playlist_full");
        }
        _itemMan.getItem(key, new IgnoreConfirmAdapter<Item>(listener) {
            @Override public void requestCompleted (Item result) {
                addToPlaylist2(who, (Audio)result, listener);
            }
        });
    }

    // documentation inherited from RoomProvider
    public void jumpToSong (
        ClientObject caller, int songId, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        requireManager(caller);

        if ((_roomObj.currentSongId != songId) &&
                _roomObj.playlist.containsKey(new ItemIdent(Item.AUDIO, songId))) {
            if (_songJumpThrottle.throttleOp()) {
                throw new InvocationException("e.rapid_music_jump");
            }
            playSong(songId);
        } // else: no need to give an error
        listener.requestProcessed();
    }

    // documentation inherited from RoomProvider
    public void songEnded (ClientObject caller, int playCount)
    {
        // validation? We just trust the clients completely for now. Not sure what we'd do anyhow

        if (playCount != _roomObj.playCount) {
            return; // not applicable, another client has already set us straight
        }

        playNextSong(false);
    }

    // documentation inherited from RoomProvider
    public void setActorState (ClientObject caller, ItemIdent item, int actorOid, String state)
    {
        if (caller instanceof MemberObject) {
            MemberObject who = (MemberObject) caller;
            if (!_roomObj.occupants.contains(who.getOid())) {
                return;
            }
        }

        if (actorOid == PUPPET_OID) {
            // make sure the ident is correct for the puppet
            OccupantInfo info = _roomObj.occupantInfo.get(PUPPET_OID);
            if (info != null && item.equals(((ActorInfo)info).getItemIdent())) {
                // if the actor is here and all is valid, directly enact the state
                setState(actorOid, state);
            } // else: silently ignore
            return;
        }

        // make sure the actor to be state-changed is also in this room
        MsoyBodyObject actor;
        if (caller.getOid() != actorOid) {
            if (!_roomObj.occupants.contains(actorOid)) {
                return;
            }
            actor = (MsoyBodyObject) _omgr.getObject(actorOid);

        } else {
            // the actor is the caller
            actor = (MemberObject)caller;
        }

        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (!ensureEntityControl(caller, item, "setState")) {
            log.info("Dropping change state for lack of control", "who", caller.who(),
                "item", item, "state", state);
            return;
        }

        // call the public (non-invocation service) method to enact it
        setState(actor, state);
    }

    // documentation inherited from RoomProvider
    public void editRoom (ClientObject caller, RoomService.ResultListener listener)
        throws InvocationException
    {
        requireManager(caller);

        // for now send back a TRUE
        listener.requestProcessed(Boolean.TRUE);
    }

    // documentation inherited from RoomProvider
    public void updateRoom (ClientObject caller, SceneUpdate update,
                            RoomService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject user = requireManager(caller);
        doRoomUpdate(update, user.getMemberId(), user);
    }

    // from interface RoomProvider
    public void publishRoom (ClientObject caller, RoomService.InvocationListener listener)
        throws InvocationException
    {
        MemberObject user = (MemberObject) caller;
        if (!canManage(user)) {
            throw new InvocationException(RoomCodes.E_ACCESS_DENIED);
        }

        _invoker.postUnit(new WriteOnlyUnit("publishRoom") {
            public void invokePersist () {
                _sceneRepo.publishScene(_scene.getId());
            }
        });
    }

    // documentation inherited from RoomProvider
    public void updateMemory (
        ClientObject caller, ItemIdent ident, String key, byte[] newValue,
        RoomService.ResultListener listener)
    {
        // do any first-level validation based on the item and the caller
        if (!validateMemoryUpdate((MemberObject)caller, ident)) {
            listener.requestProcessed(Boolean.FALSE);
            return;
        }

        // verify that the memory does not exceed legal size
        EntityMemories mems = _roomObj.memories.get(ident);
        int totalSize = (mems == null) ? 0 : mems.getSize(key);
        int newSize = EntityMemories.getSize(key, newValue);
        if ((totalSize + newSize) > EntityMemories.MAX_ENCODED_MEMORY_LENGTH) {
            log.info("Rejecting memory update as too large",
                "otherSize", totalSize, "newEntrySize", newSize);
            // Let the client know we looked at the memory, but didn't actually store it
            listener.requestProcessed(Boolean.FALSE);
            return;
        }

        // mark it as modified and update the room object; we'll save it when we unload the room
        _roomObj.updateMemory(ident, key, newValue);
        listener.requestProcessed(Boolean.TRUE);
    }

    // from interface RoomProvider
    public void changeLocation (ClientObject caller, ItemIdent item, Location newLoc)
    {
        // if this client does not currently control this entity; ignore the request; if no one
        // controls it, this will assign this client as controller
        if (!ensureEntityControl(caller, item, "changeLocation")) {
            return;
        }

        int oid = findActorOid(item);
        if (oid != 0) {
            _roomObj.updateOccupantLocs(new SceneLocation(newLoc, oid));
        }
    }

    // from RoomProvider
    public void spawnMob (
        ClientObject caller, int gameId, String mobId, String mobName, Location startLoc,
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (!WindowClientObject.isForGame(caller, gameId)) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
        if (StringUtil.isBlank(mobName)) {
            throw new IllegalArgumentException(
                "Mob spawn request without name [gameId=" + gameId + ", mobId=" + mobId + "]");
        }

        // these two messages will only ever show up in an AVRG server agent log
        if (countMobs(gameId) >= MAX_MOBS_PER_GAME) {
            throw new InvocationException("e.too_many_mobs");
        }
        MobObject mobObj = getMob(gameId, mobId);
        if (mobObj != null) {
            throw new InvocationException("e.duplicate_mob_id " + mobId);
        }

        mobObj = _omgr.registerObject(new MobObject());
        mobObj.setGameId(gameId);
        mobObj.setIdent(mobId);
        mobObj.setUsername(new Name(mobName));

        putMob(gameId, mobId, mobObj);

        // prepare to set the starting location
        _startingLocs.put(mobObj.getOid(), startLoc);

        // then enter the place
        _locmgr.moveTo(mobObj, _plobj.getOid());
    }

    // from RoomProvider
    public void moveMob (
        ClientObject caller, int gameId, String mobId, Location newLoc,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (!WindowClientObject.isForGame(caller, gameId)) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }

        MobObject mobObj = getMob(gameId, mobId);
        if (mobObj == null) {
            // this will only ever show up in an AVRG server agent log
            throw new InvocationException("e.mob_not_found " + mobId);
        }

        changeLocation(mobObj, (MsoyLocation)newLoc);
    }

    // from RoomProvider
    public void despawnMob (
        ClientObject caller, int gameId, String mobId,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (!WindowClientObject.isForGame(caller, gameId)) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }

        MobObject mobObj = removeMob(gameId, mobId);
        if (mobObj != null) {
            _locmgr.leaveOccupiedPlace(mobObj);
            _omgr.destroyObject(mobObj.getOid());
        } else {
            // this will only ever show up in an AVRG server agent log
            listener.requestFailed("e.mob_not_found " + mobId);
        }
    }

    // from RoomProvider
    public void setProperty (ClientObject caller, String propName, Object data, Integer key,
        boolean isArray, boolean testAndSet, Object testValue,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // This call is only allowed from a bureau window
        if (!(caller instanceof WindowClientObject)) {
            throw new InvocationException(RoomCodes.E_CANNOT_SET_PROPERTY);
        }

        // Fish out the game id
        String bureauId = ((WindowClientObject)caller).bureauId;
        if (!bureauId.startsWith(BureauTypes.GAME_BUREAU_ID_PREFIX)) {
            log.warning("Bad bureau id", "bureauId", bureauId, "caller", caller);
            throw new InvocationException(RoomCodes.E_CANNOT_SET_PROPERTY);
        }
        int gameId = Integer.parseInt(bureauId.substring(
            BureauTypes.GAME_BUREAU_ID_PREFIX.length()));

        // Find the properties
        RoomPropertiesEntry entry = _roomObj.propertySpaces.get(gameId);
        if (entry == null) {
            log.warning("Properties not loaded", "caller", caller);
            throw new InvocationException(RoomCodes.E_CANNOT_SET_PROPERTY);
        }
        RoomPropertiesObject props = (RoomPropertiesObject)_omgr.getObject(entry.propsOid);

        // Test, if requested
        if (testAndSet && !PropertySpaceHelper.testProperty(props, propName, testValue)) {
            return; // the test failed: do not set the property
        }

        // And apply
        try {
            Object oldData = PropertySpaceHelper.applyPropertySet(
                props, propName, data, key, isArray);
            props.postEvent(
                new PropertySetEvent(props.getOid(), propName, data, key, isArray, oldData));
        } catch (PropertySetException pse) {
            throw new InvocationException(
                MessageBundle.tcompose(RoomCodes.E_CANNOT_SET_PROPERTY, pse.getMessage()));
        }
    }

    // from RoomProvider
    public void rateRoom (ClientObject caller, final byte rating,
                          final RoomService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject member = (MemberObject) caller;
        _invoker.postUnit(new WriteOnlyUnit("rateRoom") {
            public void invokePersist () throws Exception {
                _sceneRepo.getRatingRepository().rate(getScene().getId(),
                    member.getMemberId(), rating);
            }
        });
    }

    // from RoomProvider
    public void sendPostcard (ClientObject caller, final String[] recips, final String subject,
                              final String caption, final String snapURL,
                              RoomService.ConfirmListener lner)
        throws InvocationException
    {
        final MemberObject sender = (MemberObject)caller;

        // sanity check
        if (recips.length > 25) {
            log.warning("ZOMG, rejecting spammer", "who", sender.who(), "recips", recips);
            throw new InvocationException(RoomCodes.E_INTERNAL_ERROR);
        }

        // if we have a snap URL, we can send the mail directly
        if (snapURL != null) {
            sendPostcardMail(sender, recips, subject, caption, snapURL);
            lner.requestProcessed();
            return;
        }

        // otherwise we need to look up the URL of the canonical scene snapshot
        _invoker.postUnit(new PersistingUnit(lner) {
            public void invokePersistent () throws Exception {
                // if we have no snapshot URL, we want the canonical URL
                SceneRecord srec = _sceneRepo.loadScene(getScene().getId());
                _snap = (srec == null) ? null : srec.getSnapshotFull();
                if (_snap == null) {
                    log.warning("Unable to load snapshot", "where", where(), "srec", srec);
                    throw new InvocationException(RoomCodes.E_INTERNAL_ERROR);
                }
            }
            public void handleSuccess () {
                sendPostcardMail(sender, recips, subject, caption, _snap.getMediaPath());
                super.handleSuccess();
            }
            protected MediaDesc _snap;
        });
    }

    @Override // from PlaceManager
    public void bodyWillEnter (BodyObject body)
    {
        // provide MsoyBodyObject instances with a RoomLocal they can use to determine stoniness
        // and managerness; MsoyBodyObject clears this local out in its didLeavePlace() override
        if (body instanceof MsoyBodyObject && ((MsoyBodyObject)body).isActor()) {
            // add them to our list of ordered bodies
            _actors.add(body.getOid());

            body.setLocal(RoomLocal.class, _roomLocal);
        }

        if (body instanceof MemberObject) {
            // possibly deactivate the owner puppet
            if (isOwnerMember(body)) {
                deactivatePuppet();
            } else if (!_puppetInRoom) {
                activatePuppet(); // won't if the room's not the right type
            }
            // as we arrive at a room, we entrust it with our memories for broadcast to clients
            body.getLocal(MemberLocal.class).willEnterRoom((MemberObject)body, _roomObj);
            if (_puppetInRoom) {
                final int greetOid = body.getOid();
                new Interval(_omgr) {
                    public void expired () {
                        puppetGreet(greetOid);
                    }
                }.schedule(5000); // 5 seconds
            }
        }
        if (body instanceof MsoyUserObject) {
            _partyReg.userEnteringPlace((MsoyUserObject) body, _roomObj);
        }

        // Note: we want to add the occupant info *after* we set up the party
        // (in MemberLocal.willEnterRoom), so we call super last.
        super.bodyWillEnter(body);
    }

    @Override // from PlaceManager
    public void bodyWillLeave (BodyObject body)
    {
        // super first. See "Note", below.
        super.bodyWillLeave(body);

        // Note: Calling MemberLocal.willLeaveRoom() must now occur after we've removed the
        // OccupantInfo, which happens in super.
        if (body instanceof MemberObject) {
            MemberObject member = (MemberObject)body;
            member.getLocal(MemberLocal.class).willLeaveRoom(member, _roomObj);

            if (!isStrictlyManager(member)) {
                removeVisitorSongs(member);
            }

            // possibly activate the owner puppet
            if (isOwnerMember(body)) {
                MemberObject owner = (MemberObject) body;
                PuppetName pupName = new PuppetName(
                    owner.memberName.toString(), owner.memberName.getMemberId());
                // clone the outgoing owner's memories
                EntityMemories mems = member.getLocal(MemberLocal.class).memories;
                if (mems != null) {
                    mems = (EntityMemories) mems.clone();
                    mems.modified = false; // clear the modified flag in the clone...
                }
                activatePuppet(pupName, owner.avatar, owner.isPermaguest(), mems);
            }
        }
        if (body instanceof MsoyUserObject) {
            _partyReg.userLeavingPlace((MsoyUserObject) body, _roomObj);
        }
        // clear any RoomLocal
        body.setLocal(RoomLocal.class, null);
    }

    @Override // from SpotSceneManager
    public void willTraversePortal (BodyObject body, Portal portal)
    {
        MsoyLocation loc = (MsoyLocation) portal.getLocation();
        // We need to set the body's orientation to match the approach to the portal.
        // Look up their current location and move them from there. This could be a little
        // "off" if their sprite has not yet walked to this location, but oh well.
        SceneLocation sloc = _roomObj.occupantLocs.get(body.getOid());
        if (sloc != null) {
            MsoyLocation origin = (MsoyLocation) sloc.loc;
            double radians = Math.atan2(loc.z - origin.z, loc.x - origin.x);
            // turn the radians into a positive degree value in the whirled orientation space
            loc.orient = (short) ((360 + 90 + (int) Math.round(Math.toDegrees(radians))) % 360);
        }

        // note: we don't call super, we call updateLocation() ourselves
        updateLocation(body, loc);
    }

    @Override // from PlaceManager
    public void messageReceived (MessageEvent event)
    {
        // we want to explicitly disable the standard method calling by name that we allow in more
        // trusted environments
    }

    @Override // from PlaceManager
    public void shutdown ()
    {
        deactivatePuppet(); // just to see if the memories got modified
        super.shutdown();
    }

    /**
     * Checks to see if an item is being controlled by any client. If not, the calling client is
     * assigned as the item's controller and true is returned. If the item is already being
     * controlled or is controllable by the calling client, true is returned. Otherwise false is
     * returned (indicating that another client currently has control of the item or the client
     * is not allowed to control the item).
     */
    protected boolean ensureEntityControl (ClientObject who, ItemIdent item, String from)
    {
        Integer memberOid = _avatarIdents.get(item);
        if (memberOid != null) {
            if (who instanceof WindowClientObject) {
                // Agents may control avatars that are playing their game
                MemberObject target = (MemberObject)_omgr.getObject(memberOid);
                if (target.game == null || !target.game.avrGame ||
                    !WindowClientObject.isForGame(who, target.game.gameId)) {
                    log.info("Agent attempting control of non-player avatar", "who",
                        who.who(), "avatar", item);
                    return false;
                }
                return true;

            } else if (who.getOid() == memberOid.intValue()) {
                // yes, you may control your own avatar
                return true;
            }
            log.warning("Some user is trying to control another's avatar", "who", who.who(),
                "avatar", item, "member", memberOid);
            return false;
        }
        // otherwise, it's for some entity other than a user's avatar...

        Controllable reference = new ControllableEntity(item);
        EntityControl ctrl = _roomObj.controllers.get(reference);
        if (ctrl == null) {
            //log.info("Assigning control", "item", item, "to", who.who());
            _roomObj.addToControllers(new EntityControl(reference, who.getOid()));
            return true;
        }
        return (ctrl.controllerOid == who.getOid());
    }

    /**
     * Validate that the caller be allowed to update memory for the item.
     */
    protected boolean validateMemoryUpdate (MemberObject caller, ItemIdent ident)
    {
        String reason;
        if (ident.type == Item.AVATAR) {
            // only the wearer of an avatar may update its memory
            OccupantInfo info = _roomObj.occupantInfo.get(caller.getOid());
            if (info == null) {
                reason = "not in room";

            } else if ((info instanceof ActorInfo) &&
                    ident.equals(((ActorInfo)info).getItemIdent())) {
                return true;

            } else {
                reason = "not wearing avatar";
            }

        } else if (ident.type == Item.PET) {
            // TODO: keep open, but when we have ThanePetBrain, those pets can only update
            // memory from the thane process.
            for (OccupantInfo info : _roomObj.occupantInfo) {
                if ((info instanceof ActorInfo) && ident.equals(((ActorInfo)info).getItemIdent())) {
                    return true;
                }
            }
            reason = "not in room";

        } else if (ident.type == Item.DECOR) {
            // Control is not required, it was too much of a PITA.
            MsoySceneModel msm = (MsoySceneModel)getScene().getSceneModel();
            if ((msm.decor != null) && (msm.decor.itemId == ident.itemId)) {
                return true;
            }
            reason = "decor not in room";

        } else {
            // Control is not required, it was too much of a PITA.
            MsoySceneModel msm = (MsoySceneModel)getScene().getSceneModel();
            for (FurniData furni : msm.furnis) {
                if ((furni.itemType == ident.type) && (furni.itemId == ident.itemId)) {
                    return true;
                }
            }
            reason = "furni not in room";
        }

        log.debug("Rejecting memory update", "who", caller.who(), "ident", ident, "reason", reason);
        return false;
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new RoomObject();
    }

    @Override // from PlaceManager
    protected long idleUnloadPeriod ()
    {
        return ROOM_IDLE_UNLOAD_PERIOD;
    }

    @Override // from SceneManager
    protected void gotSceneData (Object extras)
    {
        super.gotSceneData(extras);

        _extras = (RoomExtras) extras;
    }

    @Override
    protected AccessController getAccessController ()
    {
        return ROOM;
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        // set up our room object
        _roomObj = (RoomObject) _plobj;
        _roomObj.setRoomService(addDispatcher(new RoomDispatcher(this)));
        _roomObj.addListener(_roomListener);

        // register ourselves in our peer object
        MsoyScene mscene = (MsoyScene) _scene;
        _peerMan.roomDidStartup(
            mscene.getId(), mscene.getName(), mscene.getThemeId(),
            mscene.getOwnerId(), mscene.getOwnerType(), mscene.getAccessControl());

        _roomObj.startTransaction();
        try {
            // if we have memories for the items in our room, add'em to the room object
            _roomObj.setName(mscene.getName());
            _roomObj.setOwner(mscene.getOwner());
            _roomObj.setAccessControl(mscene.getAccessControl());
            if (_extras.memories != null) {
                addMemoriesToRoom(_extras.memories);
            }
            _roomObj.setPlaylist(DSet.newDSet(_extras.playlist));
            playNextSong(true);
        } finally {
            _roomObj.commitTransaction();
        }

        // load up any pets that are "let out" in this room scene
        _petMan.loadRoomPets(_roomObj, _scene.getId());

        // we're done with our auxiliary scene information, let's let it garbage collect
        _extras = null;
    }

    @Override // from PlaceManager
    protected void bodyEntered (final int bodyOid)
    {
        super.bodyEntered(bodyOid);

        DObject body = _omgr.getObject(bodyOid);
        if (body instanceof MemberObject) {
            MemberObject member = (MemberObject) body;
            ensureAVRGamePropertySpace(member);

            // update some panopticon tracking that we'll log at the end of their session
            MsoySceneModel model = (MsoySceneModel) getScene().getSceneModel();
            boolean isMemberScene = (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER);
            member.getLocal(MemberLocal.class).metrics.room.init(isMemberScene, model.ownerId);

            // update stats for badge/passport reasons
            if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                member.getLocal(MemberLocal.class).stats.addToSetStat(
                    StatType.WHIRLEDS_VISITED, model.ownerId);
            }

            // log this room entry to panopticon for future grindery
            boolean isWhirled = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);
            _eventLog.roomEntered(member.getMemberId(), isWhirled, member.getVisitorId());

            // update this user's experiences re: visiting this room (unless it's their home)
            if (member.homeSceneId != model.sceneId) {
                _memberMan.addExperience(member,
                    new MemberExperience(new Date(), HomePageItem.ACTION_ROOM, model.sceneId));
            }
        }
    }

    @Override // from PlaceManager
    protected void bodyUpdated (OccupantInfo info)
    {
        super.bodyUpdated(info);

        // if this occupant just disconnected, reassign their controlled entities
        if (info.status == OccupantInfo.DISCONNECTED) {
            reassignControllers(info.bodyOid);
        }
    }

    @Override // from PlaceManager
    protected void bodyLeft (int bodyOid)
    {
        // start metrics
        DObject body = _omgr.getObject(bodyOid);
        if (body instanceof MemberObject) {
            MemberObject member = (MemberObject)body;
            if (!member.isViewer()) {
                member.getLocal(MemberLocal.class).metrics.room.save(member);

                // get the last known occupancy length - this might have been measured above,
                // or by the peer serialization code if we're moving across servers
                int secondsInRoom =
                    member.getLocal(MemberLocal.class).metrics.room.getLastOccupancyLength();
                MsoySceneModel model = (MsoySceneModel)getScene().getSceneModel();
                boolean isWhirled = (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP);
                _eventLog.roomLeft(member.getMemberId(), model.sceneId, isWhirled,
                                   secondsInRoom, _roomObj.occupants.size(), member.getVisitorId());
            }
        }

        super.bodyLeft(bodyOid);

        // reassign this occupant's controlled entities
        reassignControllers(bodyOid);

        // remove this body from our actor list, and possibly unstone someone
        int actorIndex = _actors.indexOf(bodyOid);
        if (-1 != actorIndex) {
            _actors.remove(actorIndex);
            // if they were unstoned and there are stoned people behind them, unstone the next
            if ((actorIndex < ACTOR_RENDERING_LIMIT) && (_actors.size() >= ACTOR_RENDERING_LIMIT)) {
                final int unstoneOid = _actors.get(ACTOR_RENDERING_LIMIT - 1);
                updateOccupantInfo(unstoneOid, new ActorInfo.Updater<ActorInfo>() {
                    public boolean update (ActorInfo info) {
                        info.updateMedia((MsoyBodyObject)_omgr.getObject(unstoneOid));
                        return true;
                    }
                });
            }
        }
    }

    @Override // from PlaceManager
    protected boolean shouldDeclareEmpty (OccupantInfo leaver)
    {
        for (OccupantInfo info : _plobj.occupantInfo) {
            // if we find either a real member (not a puppet)
            if (((info instanceof MemberInfo) && !(info.username instanceof PuppetName)) ||
                    // or an observer
                    (info instanceof ObserverInfo)) {
                return false; // ... then we're not empty! There's a human still here!
            }
        }
        return true;
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        _roomObj.removeListener(_roomListener);

        super.didShutdown();

        // clear out our peer hosting information
        _peerMan.roomDidShutdown(_scene.getId());

        // shut our pets down
        _petMan.shutdownRoomPets(_roomObj);

        // flush any modified memory records to the database
        flushMemories(_invoker, _memoryRepo, _roomObj.memories);

        // flush modified property spaces and destroy dobjects
        for (RoomPropertiesEntry entry : _roomObj.propertySpaces) {
            RoomPropertiesObject properties = (RoomPropertiesObject)_omgr.getObject(entry.propsOid);
            flushAVRGamePropertySpace(entry.ownerId, properties);
            _omgr.destroyObject(entry.propsOid);
        }
    }

    /**
     * If the given member is playing an AVRG, make sure the {@link RoomObject#propertySpaces}
     * contains an entry for its game. If the new object is created, load persistent properties
     * from the database.
     */
    protected void ensureAVRGamePropertySpace (MemberObject member)
    {
        if (member.game == null || !member.game.avrGame) {
            return;
        }

        final int gameId = member.game.gameId;
        if (_roomObj.propertySpaces.containsKey(gameId)) {
            return;
        }

        if (_pendingGameIds.contains(gameId)) {
            log.warning("Room property resolution already pending", "gameId", gameId,
                        "sceneId", _scene.getId(), "memberId", member.getMemberId());
            return;
        }

        final RoomPropertiesObject props = new RoomPropertiesObject();
        final PropertySpaceHandler propertyService = new PropertySpaceHandler(props) {
            @Override protected void validateUser (ClientObject caller)
                throws InvocationException {
                if (!WindowClientObject.isForGame(caller, gameId)) {
                    throw new InvocationException(InvocationCodes.ACCESS_DENIED);
                }
            }};
        final WhirledGameMessageHandler messageService =
            new WhirledGameMessageHandler(props) {
            @Override protected ClientObject getAudienceMember (int id)
                throws InvocationException {
                // We don't support private messages in rooms (the client should use
                // AVRGameObject.messageService instead)
                throw new InvocationException(InvocationCodes.ACCESS_DENIED);
            }

            @Override protected boolean isAgent (ClientObject caller) {
                return WindowClientObject.isForGame(caller, gameId);
            }

            @Override protected void validateSender (ClientObject caller)
                throws InvocationException {
                // Allow agents and players
                if (caller instanceof WindowClientObject) {
                    if (isAgent(caller)) {
                        return;
                    }
                } else if (caller instanceof MemberObject) {
                    if (((MemberObject)caller).game.gameId == gameId) {
                        return;
                    }
                }
                throw new InvocationException(InvocationCodes.ACCESS_DENIED);
            }
        };

        _pendingGameIds.add(gameId);
        _invoker.postUnit(new Invoker.Unit("loadProps") {
            public boolean invoke () {
                try {
                    _propRecs = _sceneRepo.loadProperties(gameId, _scene.getId());
                } catch (Exception e) {
                    log.warning("Failed to load room properties", "where", where(),
                                "gameId", gameId, e);
                }
                return true;
            }

            public void handleResult () {
                // Create map of loaded properties
                Map<String, byte[]> propRecsMap = Maps.newHashMap();
                if (_propRecs != null) {
                    for (RoomPropertyRecord propRec : _propRecs) {
                        propRecsMap.put(propRec.name, propRec.value);
                    }
                }

                // Create the dobj
                _omgr.registerObject(props);

                // Populate
                PropertySpaceHelper.initWithProperties(
                    props, PropertySpaceHelper.recordsToProperties(propRecsMap), true);

                // Set members
                props.setPropertiesService(
                    _invmgr.registerDispatcher(new PropertySpaceDispatcher(propertyService)));
                props.setMessageService(
                    _invmgr.registerDispatcher(new WhirledGameMessageDispatcher(messageService)));

                // Add to room
                RoomPropertiesEntry entry = new RoomPropertiesEntry();
                entry.ownerId = gameId;
                entry.propsOid = props.getOid();
                _roomObj.addToPropertySpaces(entry);

                // Clear from pending
                _pendingGameIds.remove(gameId);

                log.info("Added property space", "roomOid", _roomObj.getOid(), "gameId", gameId,
                    "sceneId", _scene.getId(), "propsOid", props.getOid());
            }

            Collection<RoomPropertyRecord> _propRecs;
        });
    }

    /**
     * Write changed room properties to the database.
     */
    protected void flushAVRGamePropertySpace  (final int ownerId, RoomPropertiesObject properties)
    {
        log.info("Flushing avrg room properties", "roomOid", _roomObj.getOid(),
            "propsOid", properties.getOid());

        final Map<String, byte[]> encodedMap =
            PropertySpaceHelper.encodeDirtyStateForStore(properties);
        final int sceneId = _scene.getId();
        _invoker.postUnit(new WriteOnlyUnit("saveRoomProps") {
            public void invokePersist() throws Exception {
                for (Map.Entry<String, byte[]> entry : encodedMap.entrySet()) {
                    _sceneRepo.storeProperty(new RoomPropertyRecord(
                        ownerId, sceneId, entry.getKey(), entry.getValue()));
                }
            }});

        _invmgr.clearDispatcher(properties.propertiesService);
        _invmgr.clearDispatcher(properties.messageService);
    }

    @Override // documentation inherited
    protected SceneLocation computeEnteringLocation (BodyObject body, Portal from, Portal entry)
    {
        // If we have explicitly set the starting location for some reason, use that
        Location loc = _startingLocs.remove(body.getOid());
        if (loc != null) {
            return new SceneLocation(loc, body.getOid());
        }

        // if the from portal has a destination location, use that
        if (from instanceof MsoyPortal && ((MsoyPortal)from).dest != null) {
            return new SceneLocation(((MsoyPortal)from).dest, body.getOid());
        }

        // otherwise if we have a destination portal (the scene's default entrace) use that
        if (entry != null) {
            return super.computeEnteringLocation(body, from, entry);
        }

        // fallback if there is no portal
        return new SceneLocation(new MsoyLocation(.5, 0, .5, (short) 0), body.getOid());
    }

    /**
     * Performs the given updates.
     * @param user may be null if unavailable.
     */
    protected void doRoomUpdate (
        final SceneUpdate update, final int memberId, final MemberObject user)
    {
        // TODO: complicated verification of changes, including verifying that the user owns any
        // item they're adding, etc.

        final Runnable doUpdateScene = new Runnable() {
            public void run () {
                // initialize and record this update to the scene management system (which will
                // persist it, send it to the client for application to the scene, etc.)
                update.init(_scene.getId(), _scene.getVersion());
                recordUpdate(update);

                // let the registry know that rooms be gettin' updated
                if (user != null) {
                    ((MsoySceneRegistry)_screg).memberUpdatedRoom(user, (MsoyScene)_scene);
                }
            }
        };

        final MsoyScene mScene = (MsoyScene) _scene;
        if (update instanceof SceneAttrsUpdate) {
            SceneAttrsUpdate up = (SceneAttrsUpdate) update;

            // massage the room name to make sure it's kosher
            up.name = StringUtil.truncate(up.name, MsoySceneModel.MAX_NAME_LENGTH);

            // update our room object
            _roomObj.setName(up.name);
            _roomObj.setAccessControl(up.accessControl);

            // if the name or access controls were modified, we need to update our HostedPlace
            boolean nameChange = !mScene.getName().equals(up.name);
            if (nameChange || mScene.getAccessControl() != up.accessControl) {
                _peerMan.roomUpdated(mScene.getId(), up.name,
                    mScene.getThemeId(), mScene.getOwnerId(),
                    mScene.getOwnerType(), up.accessControl);
            }

            // TODO: if playlistControl changed, remove all inappropriate songs?

            // if decor was modified, we should mark new decor as used, and clear the old one
            Decor decor = mScene.getDecor();
            if (decor.itemId != up.decor.itemId) { // modified?
                _itemMan.updateItemUsage(
                    Item.DECOR, Item.UsedAs.BACKGROUND, memberId, _scene.getId(),
                    decor.itemId, up.decor.itemId, new ComplainingListener<Void>(
                        log, "Unable to update decor usage"));
                if (decor.itemId != 0) {
                    removeAndFlushMemories(decor.getIdent());
                }
                if (up.decor.itemId != 0) {
                    resolveMemories(up.decor.getIdent(), doUpdateScene);
                    return; // <--- Be careful, we're returning here...
                }
            }
            // NOTE: nothing else after decor, since it may return;

        } else if (update instanceof SceneOwnershipUpdate) {
            SceneOwnershipUpdate sou = (SceneOwnershipUpdate) update;
            byte accessControl = sou.lockToOwner ?
                MsoySceneModel.ACCESS_OWNER_ONLY : mScene.getAccessControl();
            _peerMan.roomUpdated(
                mScene.getId(), mScene.getName(), mScene.getThemeId(),
                sou.ownerId, sou.ownerType, accessControl);

            // update our room object
            _roomObj.setOwner(sou.ownerName);
            _roomObj.setAccessControl(accessControl);
        }

        // furniture modification updates require us to mark item usage
        if (update instanceof FurniUpdate.Remove) {
            // mark this item as no longer in use
            FurniData data = ((FurniUpdate)update).data;
            _itemMan.updateItemUsage(
                data.itemType, Item.UsedAs.NOTHING, memberId, mScene.getId(),
                data.itemId, 0, new ComplainingListener<Void>(
                    log, "Unable to clear furni item usage"));

            // clear out any memories that were loaded for this item
            if (data.itemType != Item.NOT_A_TYPE) {
                removeAndFlushMemories(data.getItemIdent());
            }

        } else if (update instanceof FurniUpdate.Add) {
            final FurniData data = ((FurniUpdate)update).data;

            Runnable markAndResolve = new Runnable() {
                public void run () {
                    // mark this item as in use
                    _itemMan.updateItemUsage(
                        data.itemType, Item.UsedAs.FURNITURE, memberId, mScene.getId(),
                        0, data.itemId, new ComplainingListener<Void>(
                            log, "Unable to set furni item usage"));

                    // and resolve any memories it may have, calling the scene updater when it's done
                    resolveMemories(data.getItemIdent(), doUpdateScene);
                    // don't fall through here
                }
            };

            // if the scene is themed, make sure the item is stamped
            final int themeId = mScene.getThemeId();
            if (themeId != 0) {
                validateStamp(themeId, data.itemType, data.itemId, markAndResolve);
                return;
            }

            // otherwise just proceed to usage marking & memory resolution
            markAndResolve.run();
            return;
        }

        doUpdateScene.run();
    }

    /**
     * Determine the actor oid that corresponds to the specified ItemIdent, or return 0 if none
     * found.
     */
    protected int findActorOid (ItemIdent item)
    {
        // see if it's an avatar
        Integer oid = _avatarIdents.get(item);
        if (oid != null) {
            return oid.intValue();
        }

        // otherwise, scan all occupant infos. Perhaps we should keep a mapping for non-avatar
        // actors as well?
        for (OccupantInfo info : _roomObj.occupantInfo) {
            if (info instanceof ActorInfo) {
                ActorInfo ainfo = (ActorInfo)info;
                if (ainfo.getItemIdent().equals(item)) {
                    return ainfo.getBodyOid();
                }
            }
        }

        return 0; // never found it..
    }

    /**
     * Reassigns all scene entities controlled by the specified client to new controllers.
     */
    protected void reassignControllers (int bodyOid)
    {
        // determine which items were under the control of this user
        List<Controllable> items = Lists.newArrayList();
        for (EntityControl ctrl : _roomObj.controllers) {
            if (ctrl.controllerOid == bodyOid) {
                items.add(ctrl.controlled);
            }
        }
        if (items.size() == 0) {
            return;
        }

        // clear out the old controller mappings
        _roomObj.startTransaction();
        try {
            for (Controllable item : items) {
                _roomObj.removeFromControllers(item);
            }
        } finally {
            _roomObj.commitTransaction();
        }

        // assign new mappings to remaining users
        assignControllers(items);
    }

    /**
     * Handles a request to select a controller for the supplied set of items.
     */
    protected boolean assignControllers (Collection<Controllable> ctrlables)
    {
        // determine the available controllers
        HashIntMap<Controller> controllers = new HashIntMap<Controller>();
        for (OccupantInfo info : _roomObj.occupantInfo) {
            if ((info instanceof MemberInfo) && (info.status != OccupantInfo.DISCONNECTED) &&
                    !(info.username instanceof PuppetName)) {
                controllers.put(info.bodyOid, new Controller(info.bodyOid));
            }
        }

        // if we have no potential controllers, the controllables will remain uncontrolled (which
        // is much better than them being out of control :)
        if (controllers.size() == 0) {
            return false;
        }

        // note the current load of these controllers
        for (EntityControl ctrl : _roomObj.controllers) {
            Controller owner = controllers.get(ctrl.controllerOid);
            if (owner != null) {
                owner.load++;
            }
        }

        // choose the least loaded controller that is compatible with the controllable, remove the
        // controller from the set, assign them control of the controllable, add them back to the
        // set, then finally move to the next item
        try {
            _roomObj.startTransaction();
            TreeSet<Controller> set = new TreeSet<Controller>(controllers.values());
            for (Controllable ctrlable : ctrlables) {
                for (Controller ctrl : set) {
                    MemberObject mobj = (MemberObject)_omgr.getObject(ctrl.bodyOid);
                    if (mobj == null || !ctrlable.isControllableBy(mobj)) {
                        continue;
                    }
                    set.remove(ctrl);
                    ctrl.load++;
                    //log.info("Assigning control", "item", ctrlable, "to", ctrl.bodyOid);
                    _roomObj.addToControllers(new EntityControl(ctrlable, ctrl.bodyOid));
                    set.add(ctrl);
                    break;
                }
            }

        } finally {
            _roomObj.commitTransaction();
        }
        return true;
    }

    /**
     * Checks if an item is stamped for a theme, and if so, runs the given callback. We log
     * a generic error otherwise (this should only happened with hacked clients or internal
     * errors).
     */
    protected void validateStamp (
        final int themeId, final byte itemType, final int itemId, final Runnable onSuccess)
    {
        _invoker.postUnit(new RepositoryUnit("validateStamp") {
            public void invokePersist () throws Exception {
                _success = _itemLogic.getRepository(itemType).isThemeStamped(themeId, itemId);
            }
            public void handleSuccess () {
                if (_success && onSuccess != null) {
                    onSuccess.run();
                }
            }
            protected boolean _success;
        });
    }

    /**
     * Loads up the specified memories and places them into the room object.
     */
    protected void resolveMemories (ItemIdent ident, Runnable onSuccess)
    {
        resolveMemories(Collections.singleton(ident), onSuccess);
    }

    /**
     * Loads up all specified memories and places them into the room object.
     */
    protected void resolveMemories (final Collection<ItemIdent> idents, final Runnable onSuccess)
    {
        _invoker.postUnit(new RepositoryUnit("resolveMemories") {
            public void invokePersist () throws Exception {
                _mems = _memoryRepo.loadMemories(idents);
            }
            public void handleSuccess () {
                addMemoriesToRoom(_mems);
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }
            protected Collection<MemoriesRecord> _mems;
        });
    }

    protected void addMemoriesToRoom (Collection<MemoriesRecord> memories)
    {
        _roomObj.startTransaction();
        try {
            for (MemoriesRecord mrec : memories) {
                _roomObj.putMemories(mrec.toEntry());
            }
        } finally {
            _roomObj.commitTransaction();
        }
    }

    protected void removeAndFlushMemories (ItemIdent item)
    {
        EntityMemories removed = _roomObj.takeMemories(item);
        if (removed != null) {
            // persist any of the old memories that were modified
            flushMemories(_invoker, _memoryRepo, Collections.singleton(removed));
        }
    }

    /**
     * Helper function for {@link #sendPostcard}.
     */
    protected void sendPostcardMail (MemberObject sender, String[] recips, String subject,
                                     String caption, String snapURL)
    {
        String memmail = sender.username.toString();
        for (String recip : recips) {
            _mailer.sendTemplateEmail(
                MailSender.By.HUMAN, recip, memmail, "postcard", "sender", sender.memberName,
                "sender_email", memmail, "sender_id", sender.getMemberId(), "subject", subject,
                "caption", caption, "snap_url", snapURL, "title", getScene().getName(),
                "scene_id", getScene().getId(), "server_url", DeploymentConfig.serverURL);
        }
    }

    /**
     * Get the specified mob.
     */
    protected MobObject getMob (int gameId, String mobId)
    {
        Map<String, MobObject> map = _mobs.get(gameId);
        return (map == null) ? null : map.get(mobId);
    }

    /**
     * Store a mob.
     */
    protected void putMob (int gameId, String mobId, MobObject mob)
    {
        Map<String, MobObject> map = _mobs.get(gameId);
        if (map == null) {
            map = Maps.newHashMap();
            _mobs.put(gameId, map);
        }
        map.put(mobId, mob);
    }

    /**
     * Remove a mob.
     */
    protected MobObject removeMob (int gameId, String mobId)
    {
        Map<String, MobObject> map = _mobs.get(gameId);
        if (map == null) {
            return null;
        }
        MobObject retval = map.remove(mobId);
        if (map.isEmpty()) {
            _mobs.remove(gameId);
        }
        return retval;
    }

    /**
     * Count the number of mobs in use by the specified game.
     */
    protected int countMobs (int gameId)
    {
        Map<String, MobObject> map = _mobs.get(gameId);
        return (map == null) ? 0 : map.size();
    }

    /**
     * Part two of modifyPlaylist.
     */
    protected void addToPlaylist2 (
        final MemberObject who, final Audio item, final InvocationService.ConfirmListener listener)
    {
        // make sure they own it
        if (item.ownerId != who.getMemberId()) {
            // TODO: log this?
            listener.requestFailed(InvocationCodes.E_ACCESS_DENIED);
            return;
        }

        final boolean realManager = isStrictlyManager(who);
        if (!realManager && (!item.used.forAnything())) {
            // we need to make no changes to usage: so just do it!
            addToPlaylist3(who, item, listener);

        } else {
            int oldItemId = realManager ? 0 : item.itemId;
            int newItemId = realManager ? item.itemId : 0;
            // we need to update the item usage
            _itemMan.updateItemUsage(Item.AUDIO, Item.UsedAs.BACKGROUND, who.getMemberId(),
                _scene.getId(), oldItemId, newItemId,
                new ConfirmAdapter(listener) {
                    @Override public void requestCompleted (Void nothing) {
                        if (realManager) {
                            item.used = Item.UsedAs.BACKGROUND;
                            item.location = _scene.getId();
                        } else {
                            item.used = Item.UsedAs.NOTHING;
                            item.location = 0;
                        }
                        addToPlaylist3(who, item, listener);
                    }
                });
        }
    }

    /**
     * Finish adding music to the playlist.
     */
    protected void addToPlaylist3 (
        MemberObject who, Audio item, InvocationService.ConfirmListener listener)
    {
        // update the lastTouched time, here in the runtime.
        // - If we just updated the item usage (the song is either a manager's addition,
        //   or it's a visitor's who pulled it from another room) then this time will
        //   closely match the time stored in the database.
        // - If the usage was not updated (because a visitor added an already-unused song)
        //   then this is just a made-up lastTouched time, but fine for tracking addition
        //   time, which is used for playlist ordering.
        item.lastTouched = System.currentTimeMillis();

        _roomObj.startTransaction();
        try {
            // add the song if it's not already there
            if (!_roomObj.playlist.contains(item)) {
                _roomObj.addToPlaylist(item);
            }
            // start it playing now if there's not already something playing
            if (_roomObj.playCount == -1 || !_roomObj.playlist.containsKey(
                    new ItemIdent(Item.AUDIO, _roomObj.currentSongId))) {
                playNextSong(true);
            }
        } finally {
            _roomObj.commitTransaction();
        }
        listener.requestProcessed();
    }

    /**
     * Remove all the songs from the specified member from the playlist.
     */
    protected void removeVisitorSongs (MemberObject member)
    {
        List<Comparable<?>> removeKeys = null;
        int memberId = member.getMemberId();
        boolean removedPlaying = false;
        for (Audio song : _roomObj.playlist) {
            if ((song.ownerId == memberId) && !song.isUsed()) { // don't remove if added as mgr
                if (removeKeys == null) {
                    removeKeys = Lists.newArrayList();
                }
                removeKeys.add(song.getKey());
                if (song.itemId == _roomObj.currentSongId) {
                    removedPlaying = true;
                }
            }
        }
        if (removeKeys == null) {
            return;
        }

        _roomObj.startTransaction();
        try {
            for (Comparable<?> key : removeKeys) {
                _roomObj.removeFromPlaylist(key);
            }
            if (removedPlaying) {
                playNextSong(false);
            }
        } finally {
            _roomObj.commitTransaction();
        }
    }

    /**
     * Play the next song in the playlist.
     *
     * @param firstOnRestart if the previously-playing song is not found,
     *        play the first song, otherwise the last.
     */
    protected void playNextSong (boolean firstOnRestart)
    {
        int size = _roomObj.playlist.size();
        if (size == 0) {
            _roomObj.setPlayCount(-1);
            return; // nothing to play
        }

        // else, make a list of all the songs, sort them according to "playlist order"
        // (by lastTouched ordering, oldest first) and try to move to the next song
        Audio[] songs = _roomObj.playlist.toArray(new Audio[size]);
        QuickSort.rsort(songs);

        // find the index of the currently playing song
        int curDex = -1;
        for (int ii = 0; ii < size; ii++) {
            if (songs[ii].itemId == _roomObj.currentSongId) {
                curDex = ii;
                break;
            }
        }

        if (firstOnRestart || (curDex != -1)) {
            curDex = (curDex + 1) % size; // play the first/next song

        } else {
            curDex = size - 1; // play the last song in the list..
        }
        playSong(songs[curDex].itemId);
    }

    /**
     * Play this song. The id must be valid.
     */
    protected void playSong (int songId)
    {
        _roomObj.startTransaction();
        try {
            _roomObj.setCurrentSongId(songId);
            _roomObj.setPlayCount(_roomObj.playCount + 1);
        } finally {
            _roomObj.commitTransaction();
        }
    }

    /**
     * Is the specified body that of the owner?
     * If the owner is a group, this fails, etc.
     */
    protected boolean isOwnerMember (BodyObject body)
    {
        MsoySceneModel msm = (MsoySceneModel)getScene().getSceneModel();
        return ((msm.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) &&
                msm.ownerName.equals(body.getVisibleName()));
    }

    /**
     * Activate the puppet.
     */
    protected void activatePuppet ()
    {
        MsoySceneModel msm = (MsoySceneModel)getScene().getSceneModel();
        if (msm.ownerType != MsoySceneModel.OWNER_TYPE_MEMBER || msm.noPuppet) {
            return;
        }
        final int ownerId = msm.ownerId;
        final PuppetName pupName = new PuppetName(msm.ownerName.toString(), ownerId);
        _puppetInRoom = true; // assume we're going to add it
        _invoker.postUnit(new RepositoryUnit("loadPuppetInfo") {
            public void invokePersist () throws Exception {
                MemberRecord memrec = _memberRepo.loadMember(ownerId);
                if (memrec == null) {
                    return;
                }
                _isGuest = memrec.isPermaguest();
                // below here is all success.
                // we've identified the user, let's see how much avatar we can load.
                if (memrec.avatarId == 0) {
                    return;
                }
                AvatarRecord avrec = _itemLogic.getAvatarRepository().loadItem(memrec.avatarId);
                if (avrec == null) {
                    return;
                }
                _avatar = (Avatar) avrec.toItem();
                MemoriesRecord mrec = _memoryRepo.loadMemory(Item.AVATAR, memrec.avatarId);
                if (mrec != null) {
                    _mem = mrec.toEntry();
                }
            }
            public void handleSuccess () {
                // We only proceed if we found the owner member (isGuest != null)
                // and the owner hasn't since entered the room
                if ((_isGuest != null) && (null == _roomObj.getOccupantInfo(pupName))) {
                    activatePuppet(pupName, _avatar, _isGuest, _mem);
                } else {
                    _puppetInRoom = false; // oh, we're not actually going to add it
                }
            }
            protected Avatar _avatar;
            protected Boolean _isGuest;
            protected EntityMemories _mem;
        });
    }

    /**
     * Activate the puppet, providing we already know some things from the db.
     */
    protected void activatePuppet (
        PuppetName pupName, Avatar avatar, boolean ownerIsGuest, EntityMemories mem)
    {
        _puppetInRoom = true;
        MemberInfo pupInfo = new MemberInfo();
        pupInfo.bodyOid = PUPPET_OID;
        pupInfo.username = pupName;
        pupInfo.configureAvatar(avatar, ownerIsGuest);
        _roomObj.startTransaction();
        try {
            if (mem != null) {
                _roomObj.addToMemories(mem);
            }
            _roomObj.addToOccupantLocs(new SceneLocation(calcPuppetLocation(), PUPPET_OID));
            _roomObj.addToOccupantInfo(pupInfo);
        } finally {
            _roomObj.commitTransaction();
        }
        _occInfo.put(PUPPET_OID, pupInfo);
    }

    /**
     * Calculate a location for the puppet.
     */
    protected MsoyLocation calcPuppetLocation ()
    {
        MsoyLocation entrance = ((MsoySceneModel)getScene().getSceneModel()).entrance;
        // if it's near the center, arbitrarily move it to one side, otherwise mirror it
        float x = (entrance.x > 0.4f && entrance.x < 0.6f) ? 0.1f : (1.0f - entrance.x);
        // face the avatar 45 degrees forward toward the center
        short orient = (short) ((x > 0.5f) ? 315 : 45);
        return new MsoyLocation(x, entrance.y, entrance.z, orient);
    }

    /**
     * Deactivate the puppet, if present.
     */
    protected void deactivatePuppet ()
    {
        MemberInfo info = (MemberInfo) _occInfo.remove(PUPPET_OID);
        if (info != null) {
            _roomObj.startTransaction();
            try {
                // remove the memory if it is present
                EntityMemories mems = _roomObj.memories.get(info.getItemIdent());
                if (mems != null) {
                    if (mems.modified) {
                        // This is purely for informational purposes- there's probably no problem.
                        // This can be removed. (If so, we could probably remove the shutdown())
                        log.info("Puppet memories have been modified", "ident", mems.ident);
                    }
                    _roomObj.removeFromMemories(mems.ident);
                }
                _roomObj.removeFromOccupantInfo(PUPPET_OID);
                _roomObj.removeFromOccupantLocs(PUPPET_OID);
            } finally {
                _roomObj.commitTransaction();
            }
        }
        _puppetInRoom = false;
    }

    /**
     * Have the puppet greet the specified body.
     */
    protected void puppetGreet (int oid)
    {
        OccupantInfo puppet = _roomObj.occupantInfo.get(PUPPET_OID);
        OccupantInfo toGreet = _roomObj.occupantInfo.get(oid);
        if ((puppet == null) || (toGreet == null)) {
            return; // the user or puppet has since left
        }
        SpeakUtil.sendSpeak(_roomObj, puppet.username, MsoyCodes.NPC_MSGS,
            MessageBundle.tcompose("m.hello", toGreet.username));
    }

    /** Listens to the room. */
    protected class RoomListener
        implements SetListener<OccupantInfo>
    {
        // from SetListener
        public void entryAdded (EntryAddedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(null, event.getEntry());
            }
        }

        // from SetListener
        public void entryUpdated (EntryUpdatedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                if (event.getEntry() instanceof MemberInfo) {
                    MemberInfo entry = (MemberInfo)event.getEntry();
                    MemberInfo oldEntry = (MemberInfo)event.getOldEntry();

                    // see if they actually switched avatars
                    if (!entry.getItemIdent().equals(oldEntry.getItemIdent())) {
                        updateAvatarIdent(oldEntry, entry);
                        removeAndFlushMemories(oldEntry.getItemIdent());
                    }
                }
            }
        }

        // from SetListener
        public void entryRemoved (EntryRemovedEvent<OccupantInfo> event)
        {
            String name = event.getName();
            if (name == PlaceObject.OCCUPANT_INFO) {
                updateAvatarIdent(event.getOldEntry(), null);
            }
        }

        /**
         * Maintain a mapping of ItemIdent -> oid for all MemberInfos.
         */
        protected void updateAvatarIdent (OccupantInfo oldInfo, OccupantInfo newInfo)
        {
            // we only track MemberInfo, as those are the only things that represent MemberObjects
            if (oldInfo instanceof MemberInfo) {
                _avatarIdents.remove(((MemberInfo)oldInfo).getItemIdent());
            }
            if (newInfo instanceof MemberInfo) {
                _avatarIdents.put(((MemberInfo)newInfo).getItemIdent(), newInfo.bodyOid);
            }
        }
    }

    /** Used during the process of controller assignment. */
    protected static class Controller implements Comparable<Controller>
    {
        public final int bodyOid;
        public int load;

        public Controller (int bodyOid) {
            this.bodyOid = bodyOid;
        }
        public boolean equals (Object other) {
            if (other instanceof Controller) {
                Controller that = (Controller) other;
                return (this.bodyOid == that.bodyOid);
            } else {
                return false;
            }
        }
        public int hashCode () {
            return bodyOid;
        }
        public int compareTo (Controller other) {
            // sort first by load, then by body oid
            int diff = Comparators.compare(load, other.load);
            if (diff == 0) {
                diff = Comparators.compare(bodyOid, other.bodyOid);
            }
            return diff;
        }
    } // End: static class Controller

    /** The room object. */
    protected RoomObject _roomObj;

    /** Extra data from scene resolution. */
    protected RoomExtras _extras;

    /** Game ids of properties we are currently loading. */
    protected ArrayIntSet _pendingGameIds = new ArrayIntSet();

    /** If non-null, a list of memberId blocked from the room. */
    protected ArrayIntSet _booted;

    /** True if the puppet is in the room, OR is believed to be on the way. */
    protected boolean _puppetInRoom;

    /** Listens to the room object. */
    protected RoomListener _roomListener = new RoomListener();

    /** A list of the body oids of all actors in the room, ordered by when they entered. */
    protected List<Integer> _actors = Lists.newArrayList();

    /** Mapping to keep track of spawned mobs. */
    protected HashIntMap<Map<String, MobObject>> _mobs = new HashIntMap<Map<String, MobObject>>();

    /** Mapping to keep track of starting location of added bodies. */
    protected HashIntMap<Location> _startingLocs = new HashIntMap<Location>();

    /** For all MemberInfo's, a mapping of ItemIdent to the member's oid. */
    protected Map<ItemIdent, Integer> _avatarIdents = Maps.newHashMap();

    /** The throttle imposed on jumping to a new song. */
    protected Throttle _songJumpThrottle = new Throttle(1, 4000);

    /** The RoomLocal installed on actors in this room. */
    protected RoomLocal _roomLocal = new RoomLocal() {
        public boolean useStaticMedia (MsoyBodyObject body) {
            return _actors.indexOf(body.getOid()) >= ACTOR_RENDERING_LIMIT;
        }
        public boolean isManager (MsoyBodyObject body) {
            return (body instanceof MemberObject) && canManage((MemberObject)body);
        }
    };

    /** After this level of occupancy is reached, actors are made static. */
    protected static final int ACTOR_RENDERING_LIMIT = 20;

    /** The maximum number of mobs a game may have in this room. */
    protected static final int MAX_MOBS_PER_GAME = 99;

    /** The maximum number of songs in the playlist. */
    protected static final int MAX_PLAYLIST_SIZE = 99;

    /** The puppet oid. Global, immutable. */
    protected static final Integer PUPPET_OID = Integer.valueOf(0);

    /**
     * We allow access as in {@link CrowdObjectAccess#PLACE} but also give full subscription
     * powers to {@link WindowClientObject} instances; these are the world server representatives
     * of server-side agents. We need this for AVRGs to be able to access room data.
     */
    protected static AccessController ROOM = new AccessController()
    {
        // documentation inherited from interface
        public boolean allowSubscribe (DObject object, Subscriber<?> sub)
        {
            if (sub instanceof ProxySubscriber) {
                ClientObject co = ((ProxySubscriber)sub).getClientObject();
                if (co instanceof WindowClientObject) {
                    return true;
                }
            }
            return CrowdObjectAccess.PLACE.allowSubscribe(object, sub);
        }

        // documentation inherited from interface
        public boolean allowDispatch (DObject object, DEvent event)
        {
            return CrowdObjectAccess.PLACE.allowDispatch(object, event);
        }
    };

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ItemManager _itemMan;
    @Inject protected LocationManager _locmgr;
    @Inject protected MailSender _mailer;
    @Inject protected MemberLocator _locator;
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected PartyRegistry _partyReg;
    @Inject protected PetManager _petMan;
    @Inject protected SceneRegistry _screg;
    @Inject protected ThemeRepository _themeRepo;
}
