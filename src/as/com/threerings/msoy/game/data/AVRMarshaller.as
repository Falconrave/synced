//
// $Id$

package com.threerings.msoy.game.data {

import flash.utils.ByteArray;
import com.threerings.util.*; // for Float, Integer, etc.
import com.threerings.io.TypedArray;

import com.threerings.msoy.game.client.AVRService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * Provides the implementation of the {@link AVRService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class AVRMarshaller extends InvocationMarshaller
    implements AVRService
{
    /** The method id used to dispatch {@link #activateGame} requests. */
    public static const ACTIVATE_GAME :int = 1;

    // from interface AVRService
    public function activateGame (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, ACTIVATE_GAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch {@link #deactivateGame} requests. */
    public static const DEACTIVATE_GAME :int = 2;

    // from interface AVRService
    public function deactivateGame (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DEACTIVATE_GAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }
}
}
