//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link WorldGameService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class WorldGameMarshaller extends InvocationMarshaller
    implements WorldGameService
{
    /**
     * Marshalls results to implementations of {@link WorldGameService.LocationListener}.
     */
    public static class LocationMarshaller extends ListenerMarshaller
        implements LocationListener
    {
        /** The method id used to dispatch {@link #gameLocated}
         * responses. */
        public static final int GAME_LOCATED = 1;

        // from interface LocationMarshaller
        public void gameLocated (String arg1, int arg2, boolean arg3)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, GAME_LOCATED,
                               new Object[] { arg1, Integer.valueOf(arg2), Boolean.valueOf(arg3) }, transport));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case GAME_LOCATED:
                ((LocationListener)listener).gameLocated(
                    (String)args[0], ((Integer)args[1]).intValue(), ((Boolean)args[2]).booleanValue());
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #getTablesWaiting} requests. */
    public static final int GET_TABLES_WAITING = 1;

    // from interface WorldGameService
    public void getTablesWaiting (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_TABLES_WAITING, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #inviteFriends} requests. */
    public static final int INVITE_FRIENDS = 2;

    // from interface WorldGameService
    public void inviteFriends (Client arg1, int arg2, int[] arg3)
    {
        sendRequest(arg1, INVITE_FRIENDS, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #locateGame} requests. */
    public static final int LOCATE_GAME = 3;

    // from interface WorldGameService
    public void locateGame (Client arg1, int arg2, WorldGameService.LocationListener arg3)
    {
        WorldGameMarshaller.LocationMarshaller listener3 = new WorldGameMarshaller.LocationMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_GAME, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
