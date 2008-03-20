//
// $Id$

package com.threerings.msoy.world.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
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
    public static const CHANGE_LOCATION :int = 1;

    // from interface RoomService
    public function changeLocation (arg1 :Client, arg2 :ItemIdent, arg3 :Location) :void
    {
        sendRequest(arg1, CHANGE_LOCATION, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch {@link #despawnMob} requests. */
    public static const DESPAWN_MOB :int = 2;

    // from interface RoomService
    public function despawnMob (arg1 :Client, arg2 :int, arg3 :String, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, DESPAWN_MOB, [
            Integer.valueOf(arg2), arg3, listener4
        ]);
    }

    /** The method id used to dispatch {@link #editRoom} requests. */
    public static const EDIT_ROOM :int = 3;

    // from interface RoomService
    public function editRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, EDIT_ROOM, [
            listener2
        ]);
    }

    /** The method id used to dispatch {@link #purchaseRoom} requests. */
    public static const PURCHASE_ROOM :int = 4;

    // from interface RoomService
    public function purchaseRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, PURCHASE_ROOM, [
            listener2
        ]);
    }

    /** The method id used to dispatch {@link #requestControl} requests. */
    public static const REQUEST_CONTROL :int = 5;

    // from interface RoomService
    public function requestControl (arg1 :Client, arg2 :ItemIdent) :void
    {
        sendRequest(arg1, REQUEST_CONTROL, [
            arg2
        ]);
    }

    /** The method id used to dispatch {@link #sendSpriteMessage} requests. */
    public static const SEND_SPRITE_MESSAGE :int = 6;

    // from interface RoomService
    public function sendSpriteMessage (arg1 :Client, arg2 :ItemIdent, arg3 :String, arg4 :ByteArray, arg5 :Boolean) :void
    {
        sendRequest(arg1, SEND_SPRITE_MESSAGE, [
            arg2, arg3, arg4, langBoolean.valueOf(arg5)
        ]);
    }

    /** The method id used to dispatch {@link #sendSpriteSignal} requests. */
    public static const SEND_SPRITE_SIGNAL :int = 7;

    // from interface RoomService
    public function sendSpriteSignal (arg1 :Client, arg2 :String, arg3 :ByteArray) :void
    {
        sendRequest(arg1, SEND_SPRITE_SIGNAL, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch {@link #setActorState} requests. */
    public static const SET_ACTOR_STATE :int = 8;

    // from interface RoomService
    public function setActorState (arg1 :Client, arg2 :ItemIdent, arg3 :int, arg4 :String) :void
    {
        sendRequest(arg1, SET_ACTOR_STATE, [
            arg2, Integer.valueOf(arg3), arg4
        ]);
    }

    /** The method id used to dispatch {@link #setRoomProperty} requests. */
    public static const SET_ROOM_PROPERTY :int = 9;

    // from interface RoomService
    public function setRoomProperty (arg1 :Client, arg2 :RoomPropertyEntry) :void
    {
        sendRequest(arg1, SET_ROOM_PROPERTY, [
            arg2
        ]);
    }

    /** The method id used to dispatch {@link #spawnMob} requests. */
    public static const SPAWN_MOB :int = 10;

    // from interface RoomService
    public function spawnMob (arg1 :Client, arg2 :int, arg3 :String, arg4 :String, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SPAWN_MOB, [
            Integer.valueOf(arg2), arg3, arg4, listener5
        ]);
    }

    /** The method id used to dispatch {@link #updateMemory} requests. */
    public static const UPDATE_MEMORY :int = 11;

    // from interface RoomService
    public function updateMemory (arg1 :Client, arg2 :EntityMemoryEntry) :void
    {
        sendRequest(arg1, UPDATE_MEMORY, [
            arg2
        ]);
    }

    /** The method id used to dispatch {@link #updateRoom} requests. */
    public static const UPDATE_ROOM :int = 12;

    // from interface RoomService
    public function updateRoom (arg1 :Client, arg2 :SceneUpdate, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_ROOM, [
            arg2, listener3
        ]);
    }
}
}
