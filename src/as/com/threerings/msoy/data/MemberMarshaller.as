//
// $Id$

package com.threerings.msoy.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.

import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * Provides the implementation of the {@link MemberService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MemberMarshaller extends InvocationMarshaller
    implements MemberService
{
    /** The method id used to dispatch {@link #acknowledgeNotification} requests. */
    public static const ACKNOWLEDGE_NOTIFICATION :int = 1;

    // from interface MemberService
    public function acknowledgeNotification (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ACKNOWLEDGE_NOTIFICATION, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #alterFriend} requests. */
    public static const ALTER_FRIEND :int = 2;

    // from interface MemberService
    public function alterFriend (arg1 :Client, arg2 :int, arg3 :Boolean, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, ALTER_FRIEND, [
            Integer.valueOf(arg2), langBoolean.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch {@link #getDisplayName} requests. */
    public static const GET_DISPLAY_NAME :int = 3;

    // from interface MemberService
    public function getDisplayName (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_DISPLAY_NAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #getGroupName} requests. */
    public static const GET_GROUP_NAME :int = 4;

    // from interface MemberService
    public function getGroupName (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_GROUP_NAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #getHomeId} requests. */
    public static const GET_HOME_ID :int = 5;

    // from interface MemberService
    public function getHomeId (arg1 :Client, arg2 :int, arg3 :int, arg4 :InvocationService_ResultListener) :void
    {
        var listener4 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_HOME_ID, [
            Byte.valueOf(arg2), Integer.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch {@link #purchaseRoom} requests. */
    public static const PURCHASE_ROOM :int = 6;

    // from interface MemberService
    public function purchaseRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, PURCHASE_ROOM, [
            listener2
        ]);
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static const SET_AVATAR :int = 7;

    // from interface MemberService
    public function setAvatar (arg1 :Client, arg2 :int, arg3 :Number, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_AVATAR, [
            Integer.valueOf(arg2), Float.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch {@link #setDisplayName} requests. */
    public static const SET_DISPLAY_NAME :int = 8;

    // from interface MemberService
    public function setDisplayName (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_DISPLAY_NAME, [
            arg2, listener3
        ]);
    }
}
}
