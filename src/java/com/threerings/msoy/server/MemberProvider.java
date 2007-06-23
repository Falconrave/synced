//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MemberService}.
 */
public interface MemberProvider extends InvocationProvider
{
    /**
     * Handles a {@link MemberService#acknowledgeNotification} request.
     */
    public void acknowledgeNotification (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#alterFriend} request.
     */
    public void alterFriend (ClientObject caller, int arg1, boolean arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getCurrentSceneId} request.
     */
    public void getCurrentSceneId (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getDisplayName} request.
     */
    public void getDisplayName (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getGroupName} request.
     */
    public void getGroupName (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#getHomeId} request.
     */
    public void getHomeId (ClientObject caller, byte arg1, int arg2, InvocationService.ResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setAvatar} request.
     */
    public void setAvatar (ClientObject caller, int arg1, float arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link MemberService#setDisplayName} request.
     */
    public void setDisplayName (ClientObject caller, String arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;
}
