//
// $Id$

package com.threerings.msoy.room.server;

import javax.annotation.Generated;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.client.RoomService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

/**
 * Defines the server-side of the {@link RoomService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from RoomService.java.")
public interface RoomProvider extends InvocationProvider
{
    /**
     * Handles a {@link RoomService#addOrRemoveSong} request.
     */
    void addOrRemoveSong (ClientObject caller, int arg1, boolean arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#bootDj} request.
     */
    void bootDj (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#changeLocation} request.
     */
    void changeLocation (ClientObject caller, ItemIdent arg1, Location arg2);

    /**
     * Handles a {@link RoomService#despawnMob} request.
     */
    void despawnMob (ClientObject caller, int arg1, String arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#editRoom} request.
     */
    void editRoom (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#jumpToSong} request.
     */
    void jumpToSong (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#moveMob} request.
     */
    void moveMob (ClientObject caller, int arg1, String arg2, Location arg3, InvocationService.InvocationListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#publishRoom} request.
     */
    void publishRoom (ClientObject caller, InvocationService.InvocationListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#quitDjing} request.
     */
    void quitDjing (ClientObject caller);

    /**
     * Handles a {@link RoomService#rateRoom} request.
     */
    void rateRoom (ClientObject caller, byte arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#rateTrack} request.
     */
    void rateTrack (ClientObject caller, int arg1, boolean arg2);

    /**
     * Handles a {@link RoomService#requestControl} request.
     */
    void requestControl (ClientObject caller, ItemIdent arg1);

    /**
     * Handles a {@link RoomService#sendPostcard} request.
     */
    void sendPostcard (ClientObject caller, String[] arg1, String arg2, String arg3, String arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#sendSpriteMessage} request.
     */
    void sendSpriteMessage (ClientObject caller, ItemIdent arg1, String arg2, byte[] arg3, boolean arg4);

    /**
     * Handles a {@link RoomService#sendSpriteSignal} request.
     */
    void sendSpriteSignal (ClientObject caller, String arg1, byte[] arg2);

    /**
     * Handles a {@link RoomService#setActorState} request.
     */
    void setActorState (ClientObject caller, ItemIdent arg1, int arg2, String arg3);

    /**
     * Handles a {@link RoomService#setTrackIndex} request.
     */
    void setTrackIndex (ClientObject caller, int arg1, int arg2);

    /**
     * Handles a {@link RoomService#songEnded} request.
     */
    void songEnded (ClientObject caller, int arg1);

    /**
     * Handles a {@link RoomService#spawnMob} request.
     */
    void spawnMob (ClientObject caller, int arg1, String arg2, String arg3, Location arg4, InvocationService.InvocationListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#updateMemory} request.
     */
    void updateMemory (ClientObject caller, ItemIdent arg1, String arg2, byte[] arg3, InvocationService.ResultListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link RoomService#updateRoom} request.
     */
    void updateRoom (ClientObject caller, SceneUpdate arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
