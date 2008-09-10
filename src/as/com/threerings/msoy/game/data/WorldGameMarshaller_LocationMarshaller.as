//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.msoy.game.client.WorldGameService_LocationListener;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;

/**
 * Marshalls instances of the WorldGameService_LocationMarshaller interface.
 */
public class WorldGameMarshaller_LocationMarshaller
    extends InvocationMarshaller_ListenerMarshaller
{
    /** The method id used to dispatch <code>gameLocated</code> responses. */
    public static const GAME_LOCATED :int = 1;

    // from InvocationMarshaller_ListenerMarshaller
    override public function dispatchResponse (methodId :int, args :Array) :void
    {
        switch (methodId) {
        case GAME_LOCATED:
            (listener as WorldGameService_LocationListener).gameLocated(
                (args[0] as String), (args[1] as int));
            return;

        default:
            super.dispatchResponse(methodId, args);
            return;
        }
    }
}
}
