//
// $Id$

package com.threerings.msoy.game.client {

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
}
}
