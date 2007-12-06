//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService.InvocationListener;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.MobInfo;
import com.threerings.msoy.world.data.RoomPropertyEntry;

/**
 * Service requests for rooms.
 */
public interface RoomService extends InvocationService
{
    /**
     * Requests that the specified item be assigned a controller. Other distributed state modifying
     * services will automatically assign a controller to an uncontrolled item the first time they
     * are requested, but if an entity simply wishes to start ticking itself locally, it must first
     * request control to ensure that the right client handles the ticking.
     */
    public void requestControl (Client client, ItemIdent item);

    /**
     * Requests to send a sprite message.
     *
     * @param item the identifier of the item on which to trigger the event, or null if it should
     * be delivered to all items.
     * @param name the message name.
     * @param arg the data
     * @param isAction if the message is a "action".
     */
    public void sendSpriteMessage (Client client, ItemIdent item, String name, byte[] arg,
                                   boolean isAction);

    /**
     * Requests to send a sprite signal.
     *
     * @param name the message name.
     * @param arg the data
     */
    public void sendSpriteSignal (Client client, String name, byte[] arg);

    /**
     * Requests to update an actor's state.
     */
    public void setActorState (Client client, ItemIdent item, int actorOid, String state);

    /**
     * Requests to edit the client's current room.
     *
     * @param listener will be informed with an array of items in the room.
     */
    public void editRoom (Client client, ResultListener listener);

    /**
     * Request to apply the specified scene updates to the room.
     */
    public void updateRoom (Client client, SceneUpdate[] updates, InvocationListener listener);

    /**
     * Request to purchase a new room.
     */
    public void purchaseRoom (Client client, ResultListener listener);

    /**
     * Issues a request to update the memory of the specified entity (which is associated with a
     * particular item).
     */
    public void updateMemory (Client client, EntityMemoryEntry entry);

    /**
     * Issues a request to update a property in the shared room state.
     */
    public void setRoomProperty (Client client, RoomPropertyEntry entry);

    /**
     * Issues a request to update the current scene location of the specified item. This is called
     * by Pets and other MOBs that want to move around the room.
     */
    public void changeLocation (Client client, ItemIdent item, Location newloc);

    /**
     * Requests the placement of a MOB in the current scene location.
     *
     * @see MobInfo
     */
    public void spawnMob (Client caller, int gameId, String mobId, InvocationListener listener);

    /**
     * Requests the removal of a MOB from the current scene location.
     *
     * @see MobInfo
     */
    public void despawnMob (Client caller, int gameId, String mobId, InvocationListener listener);
}
