//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.msoy.party.client.PartyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.util.Byte;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>PartyService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PartyMarshaller extends InvocationMarshaller
    implements PartyService
{
    /** The method id used to dispatch <code>assignLeader</code> requests. */
    public static const ASSIGN_LEADER :int = 1;

    // from interface PartyService
    public function assignLeader (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ASSIGN_LEADER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>bootMember</code> requests. */
    public static const BOOT_MEMBER :int = 2;

    // from interface PartyService
    public function bootMember (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, BOOT_MEMBER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>disbandParty</code> requests. */
    public static const DISBAND_PARTY :int = 3;

    // from interface PartyService
    public function disbandParty (arg1 :Client, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, DISBAND_PARTY, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>inviteMember</code> requests. */
    public static const INVITE_MEMBER :int = 4;

    // from interface PartyService
    public function inviteMember (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_MEMBER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>moveParty</code> requests. */
    public static const MOVE_PARTY :int = 5;

    // from interface PartyService
    public function moveParty (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, MOVE_PARTY, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>setGame</code> requests. */
    public static const SET_GAME :int = 6;

    // from interface PartyService
    public function setGame (arg1 :Client, arg2 :int, arg3 :int, arg4 :int, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_GAME, [
            Integer.valueOf(arg2), Byte.valueOf(arg3), Integer.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch <code>updateRecruitment</code> requests. */
    public static const UPDATE_RECRUITMENT :int = 7;

    // from interface PartyService
    public function updateRecruitment (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_RECRUITMENT, [
            Byte.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>updateStatus</code> requests. */
    public static const UPDATE_STATUS :int = 8;

    // from interface PartyService
    public function updateStatus (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_STATUS, [
            arg2, listener3
        ]);
    }
}
}
