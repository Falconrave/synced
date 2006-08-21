//
// $Id$

package com.threerings.msoy.server;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.crowd.server.CrowdObjectAccess;

import com.threerings.parlor.game.server.GameManager;

/**
 * Contains standard access controllers for msoy.
 */
public class MsoyObjectAccess
{
    /** The default access controller. */
    public static AccessController DEFAULT = CrowdObjectAccess.DEFAULT;

    /** The user access controller. */
    public static AccessController USER = DEFAULT;

    /** The game access controller. */
    public static AccessController GAME = new AccessController() {
        public boolean allowSubscribe (DObject object, Subscriber sub)
        {
            return true;
        }

        public boolean allowDispatch (DObject object, DEvent event)
        {
            if (event instanceof MessageEvent) {
                // we actually block messages from clients
                return (event.getSourceOid() == -1);
            }

            return DEFAULT.allowDispatch(object, event);
        }
    };
}
