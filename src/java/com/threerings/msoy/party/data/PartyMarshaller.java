//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.party.client.PartyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link PartyService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PartyMarshaller extends InvocationMarshaller
    implements PartyService
{
    /** The method id used to dispatch {@link #assignLeader} requests. */
    public static final int ASSIGN_LEADER = 1;

    // from interface PartyService
    public void assignLeader (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ASSIGN_LEADER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #bootMember} requests. */
    public static final int BOOT_MEMBER = 2;

    // from interface PartyService
    public void bootMember (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, BOOT_MEMBER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #inviteMember} requests. */
    public static final int INVITE_MEMBER = 3;

    // from interface PartyService
    public void inviteMember (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_MEMBER, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #moveParty} requests. */
    public static final int MOVE_PARTY = 4;

    // from interface PartyService
    public void moveParty (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, MOVE_PARTY, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setGame} requests. */
    public static final int SET_GAME = 5;

    // from interface PartyService
    public void setGame (Client arg1, int arg2, boolean arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_GAME, new Object[] {
            Integer.valueOf(arg2), Boolean.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #updateRecruitment} requests. */
    public static final int UPDATE_RECRUITMENT = 6;

    // from interface PartyService
    public void updateRecruitment (Client arg1, byte arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_RECRUITMENT, new Object[] {
            Byte.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #updateStatus} requests. */
    public static final int UPDATE_STATUS = 7;

    // from interface PartyService
    public void updateStatus (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_STATUS, new Object[] {
            arg2, listener3
        });
    }
}
