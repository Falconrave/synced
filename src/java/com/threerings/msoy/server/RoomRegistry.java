//
// $Id$

package com.threerings.msoy.server;

import static com.threerings.msoy.Log.log;

import java.util.Iterator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.util.Lifecycle;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.BodyLocator;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;
import com.threerings.msoy.bureau.data.ThaneCodes;
import com.threerings.msoy.bureau.data.WindowClientObject;
import com.threerings.msoy.bureau.server.ThaneWorldDispatcher;
import com.threerings.msoy.bureau.server.ThaneWorldProvider;

import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.server.RoomManager;

import com.threerings.msoy.game.data.ParlorGameConfig;
import com.threerings.msoy.game.server.PlayerLocator;

import com.threerings.presents.client.InvocationService.ResultListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

/**
 * Adds msoy functionality to the place registry.
 */
@Singleton
public class RoomRegistry extends PlaceRegistry
    implements ThaneWorldProvider
{
    @Inject public RoomRegistry (Lifecycle cycle, InvocationManager invmgr)
    {
        super(cycle);

        // register thane world service
        invmgr.registerDispatcher(new ThaneWorldDispatcher(this), ThaneCodes.THANE_GROUP);
    }

    // from ThaneWorldProvider
    public void locateRoom (ClientObject caller, int sceneId, ResultListener resultListener)
        throws InvocationException
    {
        if (!(caller instanceof WindowClientObject)) {
            throw new InvocationException(InvocationCodes.E_ACCESS_DENIED);
        }

        PlaceObject plobj = null;
        Iterator<PlaceManager> iter = enumeratePlaceManagers();
        while (iter.hasNext()) {
            PlaceManager pmgr = iter.next();
            if (!(pmgr instanceof RoomManager)) {
                continue;
            }
            RoomManager rmgr = (RoomManager)pmgr;
            if (rmgr.getScene() == null || rmgr.getScene().getId() != sceneId) {
                continue;
            }
            if (rmgr.getPlaceObject() == null) {
                log.warning("RoomManager for scene has no place obj", "scene", sceneId);
            }
            plobj = rmgr.getPlaceObject();
            break;
        }

        if (plobj == null) {
            resultListener.requestFailed("Place object not found");

        } else if (!(plobj instanceof RoomObject)) {
            resultListener.requestFailed("Place is not a room");

        } else {
            resultListener.requestProcessed(plobj.getOid());
        }
    }

    @Override // from PlaceRegistry
    protected BodyLocator selectLocator (PlaceConfig config)
    {
        if (config instanceof ParlorGameConfig) { // TODO: avrgs?
            return _plocator;
        } else {
            return super.selectLocator(config);
        }
    }

    // TODO
//     @Override protected PlaceManager createPlaceManager (PlaceConfig config) throws Exception {
//         ClassLoader loader = _hostedMan.getClassLoader(config);
//         if (loader == null) {
//             return super.createPlaceManager(config);
//         }
//         return (PlaceManager)Class.forName(
//             config.getManagerClassName(), true, loader).newInstance();
//     }

//     @Inject protected HostedGameManager _hostedMan;

    @Inject protected PlayerLocator _plocator;
}
