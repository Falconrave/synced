//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.whirled.spot.data.Portal;

/**
 * In Whirled, portals include the location in the destination scene at which to arrive rather than
 * requiring that portals be bound to another portal in the target room.
 */
public class MsoyPortal extends Portal
{
    /** The location at which to arrive in the target scene. May be null in which case the body is
     * placed at the scene's default entrance. */
    public MsoyLocation dest;

    /** Used when unserializing. */
    public MsoyPortal ()
    {
    }

    /**
     * Constructs a portal from the supplied furni data record.
     */
    public MsoyPortal (FurniData furni)
        throws IllegalArgumentException
    {
        String[] vals = furni.actionData.split(":");
        portalId = furni.id;
        loc = furni.loc;
        targetSceneId = Integer.parseInt(vals[0]);
        targetPortalId = (short) -1;

        // parse our destination location if we have one
        if (vals.length > 5) {
            dest = new MsoyLocation();
            dest.x = Float.parseFloat(vals[1]);
            dest.y = Float.parseFloat(vals[2]);
            dest.z = Float.parseFloat(vals[3]);
            dest.orient = Short.parseShort(vals[4]);
        }
    }
}
