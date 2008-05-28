//
// $Id$

package com.threerings.msoy.world.server;

import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.text.MessageUtil;

import com.threerings.util.Name;

import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.chat.server.SpeakUtil;

import com.threerings.whirled.client.SceneMoveAdapter;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.PetCodes;
import com.threerings.msoy.world.data.PetObject;

import static com.threerings.msoy.Log.log;

/**
 * Manages a Pet at runtime.
 */
public class PetHandler
{
    public PetHandler (PetManager petmgr, Pet pet, List<EntityMemoryEntry> memories)
    {
        _petobj = MsoyServer.omgr.registerObject(new PetObject());
        _petobj.memories = memories;
        _petobj.setUsername(new Name(pet.name));
        _petobj.pet = pet;
        _petmgr = petmgr;
        _petmgr.mapHandler(pet.itemId, this);
    }

    /**
     * Returns this pet's distributed object.
     */
    public PetObject getPetObject ()
    {
        return _petobj;
    }

    /**
     * Called when we are transferred to a new server, rewires up our following listener.
     */
    public void reinitFollowing (MemberObject owner)
    {
        if (_follow != null) {
            log.warning("Requested to reinit following but we're already following!? " +
                        "[pet=" + this + ", target=" + owner.who() + "].");
            return;
        }

        _follow = owner;
        _follow.setWalkingId(_petobj.pet.itemId);
        _follow.addListener(_follist = new ObjectDeathListener() {
            public void objectDestroyed (ObjectDestroyedEvent event) {
                // our followee logged off, shut ourselves down
                shutdown(false);
            }
        });
    }

    /**
     * Shuts down this pet, removing it from the world and cleaning up its handler.
     */
    public void shutdown (boolean roomDidShutdown)
    {
        // remove ourselves from the handler mapping
        _petmgr.clearHandler(_petobj.pet.itemId);

        // if we're not shutting down because our room shutdown...
        if (!roomDidShutdown) {
            // leave our current location (which will extract our memories)
            MsoyServer.screg.leaveOccupiedScene(_petobj);
            // and save them
            RoomManager.flushMemories(_petobj.memories);
        }

        // if we're following a member, clear that out
        stopFollowing();

        // finally, destroy our pet object
        MsoyServer.omgr.destroyObject(_petobj.getOid());
    }

    /**
     * Moves the pet into the specified room.
     */
    public void enterRoom (final int sceneId)
    {
        log.info("Entering room [pet=" + this + ", sceneId=" + sceneId + "].");

        // then enter the scene like a proper scene entity
        MsoyServer.screg.moveTo(_petobj, sceneId, Integer.MAX_VALUE, new SceneMoveAdapter() {
            public void requestFailed (String reason) {
                log.warning("Pet failed to enter scene [pet=" + this + ", scene=" + sceneId +
                            ", reason=" + reason + "].");
                shutdown(false);
            }
        });
    }

    /**
     * Places this pet in the owner's room and puts them in follow mode.
     */
    public void moveToOwner (MemberObject owner)
        throws InvocationException
    {
        validateOwnership(owner);
        // set ourselves to follow mode
        startFollowing(owner);
        // head to our destination
        enterRoom(owner.getSceneId());
    }

    /**
     * Handles an order from the specified user on this pet.
     */
    public void orderPet (MemberObject owner, int order)
        throws InvocationException
    {
        // first validate the permissions
        if (order == Pet.ORDER_SLEEP) {
            validateRoomOrPetOwnership(owner);
        } else {
            validateOwnership(owner);
        }

        // then enact the order
        switch (order) {
        case Pet.ORDER_SLEEP:
            if (_petobj.pet.ownerId != owner.getMemberId()) {
                // a non-owner sent the pet to sleep, let's report that
                MemberObject realOwner = MsoyServer.lookupMember(_petobj.pet.ownerId);
                if (realOwner != null) {
                    SpeakUtil.sendInfo(realOwner, MsoyCodes.GENERAL_MSGS,
                        MessageUtil.tcompose("m.pet_ordered4_room_mgr", owner.getVisibleName()));
                }
            }
            stopFollowing();
            updateUsage(Item.UNUSED, 0);
            shutdown(false);
            break;

        case Pet.ORDER_FOLLOW:
            startFollowing(owner);
            break;

        case Pet.ORDER_GO_HOME:
            stopFollowing();
            updateUsage(Item.USED_AS_PET, owner.homeSceneId);
            if (_petobj.getSceneId() == owner.homeSceneId) {
                // we're already home, yay!
            } else if (MsoyServer.screg.getSceneManager(owner.homeSceneId) != null) {
                enterRoom(owner.homeSceneId);
            } else {
                // TODO: if home room is resolved (on any server), instruct it to resolve pet
                shutdown(false);
            }
            break;

        case Pet.ORDER_STAY:
            // make sure the requester is in a room that they own
            PlaceManager plmgr = MsoyServer.plreg.getPlaceManager(owner.getPlaceOid());
            if (!(plmgr instanceof RoomManager)) {
                log.info("Owner no longer in a room? [who=" + owner.who() + ", in=" + plmgr + "].");
                throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
            }
            ((RoomManager)plmgr).checkCanAddPet(owner);
            // potentially stop following our owner
            stopFollowing();
            // note that we want to autoload in this room
            updateUsage(Item.USED_AS_PET, ((RoomManager)plmgr).getScene().getId());
            break;

        default:
            log.warning("Received unknown pet order [from=" + owner.who() +
                        ", order=" + order + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }
    }

    @Override // from Object
    public String toString ()
    {
        return "[id=" + _petobj.pet.itemId + ", name=" + _petobj.pet.name + "]";
    }

    /**
     * Validate that the specified user is the owner of this pet.
     */
    protected void validateOwnership (MemberObject owner)
        throws InvocationException
    {
        // TODO: if pet is updated or changes hands, we need to update the resolved PetObject
        if (_petobj.pet.ownerId != owner.getMemberId()) {
            // TODO: allow support personnel?
            log.warning("Pet handling by non-owner [who=" + owner.who() + ", pet=" + this +
                        ", ownerId=" + _petobj.pet.ownerId + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Validate that the specified user is the room owner or the owner of the pet.
     */
    protected void validateRoomOrPetOwnership (MemberObject owner)
        throws InvocationException
    {
        // normally I always do the cheap compare first, but I'd rather not duplicate
        // the code from validateOwnership.
        PlaceManager plmgr = MsoyServer.plreg.getPlaceManager(owner.getPlaceOid());
        if ((plmgr instanceof RoomManager) && ((RoomManager) plmgr).canManage(owner)) {
            return; // they check out, they're room owners
        }
        // otherwise...
        validateOwnership(owner);
    }

    /**
     * Sets up the necessary bits to follow our owner. If this is not possible, we'll throw an
     * invocation exception and shut ourselves down.
     */
    protected void startFollowing (MemberObject owner)
        throws InvocationException
    {
        if (_follow != null) {
            log.warning("Asked to follow but we're already following! [pet=" + this +
                        ", target=" + owner.who() + ", following=" + _follow.who() + "].");
            throw new InvocationException(PetCodes.E_INTERNAL_ERROR);
        }

        if (owner.walkingId != 0) {
            shutdown(false);
            throw new InvocationException(PetCodes.E_ALREADY_WALKING);
        }

        reinitFollowing(owner);
    }

    /**
     * Clears out our following bits.
     */
    protected void stopFollowing ()
    {
        if (_follow == null) {
            return;
        }
        if (_follow.walkingId != _petobj.pet.itemId) {
            log.warning("Our owner is somehow not walking us? [pet=" + this +
                        ", owner=" + _follow.who() + ", walking=" + _follow.walkingId + "].");
        } else {
            _follow.setWalkingId(0);
        }
        if (_follist != null) {
            _follow.removeListener(_follist);
            _follist = null;
        }
        _follow = null;
    }

    /**
     * Update the marked usage and location of the pet we handle.
     */
    protected void updateUsage (final byte usageType, final int location)
    {
        final int itemId = _petobj.pet.itemId;
        MsoyServer.invoker.postUnit(new WriteOnlyUnit("updatePetUsage(" + itemId + ")") {
            public void invokePersist () throws Exception {
                MsoyServer.itemMan.getPetRepository().markItemUsage(
                    new int[] { itemId }, usageType, location);
            }
        });
    }

    protected PetManager _petmgr;
    protected PetObject _petobj;

    protected MemberObject _follow;
    protected ObjectDeathListener _follist;
}
