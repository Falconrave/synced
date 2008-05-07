//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.game.client.LobbyService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java LobbyService interface.
 */
public interface LobbyService extends InvocationService
{
    // from Java interface LobbyService
    function identifyLobby (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface LobbyService
    function joinPlayerGame (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface LobbyService
    function playNow (arg1 :Client, arg2 :int, arg3 :int, arg4 :InvocationService_ResultListener) :void;
}
}
