//
// $Id$

package com.threerings.msoy.data;

import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

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
    /** The method id used to dispatch {@link #alterFriend} requests. */
    public static final int ALTER_FRIEND = 1;

    // from interface MemberService
    public void alterFriend (Client arg1, int arg2, boolean arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, ALTER_FRIEND, new Object[] {
            Integer.valueOf(arg2), Boolean.valueOf(arg3), listener4
        });
    }

    /** The method id used to dispatch {@link #getMemberHomeId} requests. */
    public static final int GET_MEMBER_HOME_ID = 2;

    // from interface MemberService
    public void getMemberHomeId (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_MEMBER_HOME_ID, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setAvatar} requests. */
    public static final int SET_AVATAR = 3;

    // from interface MemberService
    public void setAvatar (Client arg1, int arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_AVATAR, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #setDisplayName} requests. */
    public static final int SET_DISPLAY_NAME = 4;

    // from interface MemberService
    public void setDisplayName (Client arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_DISPLAY_NAME, new Object[] {
            arg2, listener3
        });
    }
}
