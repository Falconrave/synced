//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2006 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.msoy.server;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.CrowdServer;
import com.threerings.crowd.server.PlaceManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.msoy.Log;
import com.threerings.msoy.data.SimpleChatConfig;

/**
 * Msoy server class.
 */
public class MsoyServer extends CrowdServer
{
    /** The oid of the global chat room. */
    public static int chatOid;

    // documentation inherited
    public void init ()
        throws Exception
    {
        super.init();

        // set up the right client class
        clmgr.setClientClass(MsoyClient.class);

        // create the global chat place
        plreg.createPlace(new SimpleChatConfig(),
            new PlaceRegistry.CreationObserver() {
                public void placeCreated (PlaceObject place, PlaceManager plmgr)
                {
                    chatOid = place.getOid();
                }
            });

        Log.info("Msoy server initialized.");
    }

    public static void main (String[] args)
    {
        MsoyServer server = new MsoyServer();
        try {
            server.init();
            server.run();
        } catch (Exception e) {
            Log.warning("Unable to initialize server.");
            Log.logStackTrace(e);
        }
    }
}
