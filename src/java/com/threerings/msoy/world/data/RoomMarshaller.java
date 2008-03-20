//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

/**
 * Provides the implementation of the {@link RoomService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class RoomMarshaller extends InvocationMarshaller
    implements RoomService
{
    /** The method id used to dispatch {@link #changeLocation} requests. */
    public static final int CHANGE_LOCATION = 1;

    // from interface RoomService
    public void changeLocation (Client arg1, ItemIdent arg2, Location arg3)
    {
        sendRequest(arg1, CHANGE_LOCATION, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #despawnMob} requests. */
    public static final int DESPAWN_MOB = 2;

    // from interface RoomService
    public void despawnMob (Client arg1, int arg2, String arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, DESPAWN_MOB, new Object[] {
            Integer.valueOf(arg2), arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #editRoom} requests. */
    public static final int EDIT_ROOM = 3;

    // from interface RoomService
    public void editRoom (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, EDIT_ROOM, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #purchaseRoom} requests. */
    public static final int PURCHASE_ROOM = 4;

    // from interface RoomService
    public void purchaseRoom (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, PURCHASE_ROOM, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #requestControl} requests. */
    public static final int REQUEST_CONTROL = 5;

    // from interface RoomService
    public void requestControl (Client arg1, ItemIdent arg2)
    {
        sendRequest(arg1, REQUEST_CONTROL, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #sendSpriteMessage} requests. */
    public static final int SEND_SPRITE_MESSAGE = 6;

    // from interface RoomService
    public void sendSpriteMessage (Client arg1, ItemIdent arg2, String arg3, byte[] arg4, boolean arg5)
    {
        sendRequest(arg1, SEND_SPRITE_MESSAGE, new Object[] {
            arg2, arg3, arg4, Boolean.valueOf(arg5)
        });
    }

    /** The method id used to dispatch {@link #sendSpriteSignal} requests. */
    public static final int SEND_SPRITE_SIGNAL = 7;

    // from interface RoomService
    public void sendSpriteSignal (Client arg1, String arg2, byte[] arg3)
    {
        sendRequest(arg1, SEND_SPRITE_SIGNAL, new Object[] {
            arg2, arg3
        });
    }

    /** The method id used to dispatch {@link #setActorState} requests. */
    public static final int SET_ACTOR_STATE = 8;

    // from interface RoomService
    public void setActorState (Client arg1, ItemIdent arg2, int arg3, String arg4)
    {
        sendRequest(arg1, SET_ACTOR_STATE, new Object[] {
            arg2, Integer.valueOf(arg3), arg4
        });
    }

    /** The method id used to dispatch {@link #setRoomProperty} requests. */
    public static final int SET_ROOM_PROPERTY = 9;

    // from interface RoomService
    public void setRoomProperty (Client arg1, RoomPropertyEntry arg2)
    {
        sendRequest(arg1, SET_ROOM_PROPERTY, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #spawnMob} requests. */
    public static final int SPAWN_MOB = 10;

    // from interface RoomService
    public void spawnMob (Client arg1, int arg2, String arg3, String arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SPAWN_MOB, new Object[] {
            Integer.valueOf(arg2), arg3, arg4, listener5
        });
    }

    /** The method id used to dispatch {@link #updateMemory} requests. */
    public static final int UPDATE_MEMORY = 11;

    // from interface RoomService
    public void updateMemory (Client arg1, EntityMemoryEntry arg2)
    {
        sendRequest(arg1, UPDATE_MEMORY, new Object[] {
            arg2
        });
    }

    /** The method id used to dispatch {@link #updateRoom} requests. */
    public static final int UPDATE_ROOM = 12;

    // from interface RoomService
    public void updateRoom (Client arg1, SceneUpdate arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_ROOM, new Object[] {
            arg2, listener3
        });
    }
}
