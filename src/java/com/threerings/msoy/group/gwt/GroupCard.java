//
// $Id$

package com.threerings.msoy.group.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.data.all.Group;

/**
 * Contains a group's name, logo and home scene.
 */
public class GroupCard
    implements IsSerializable
{
    /** The group's name. */
    public GroupName name;

    /** The groups's logo. Note: Could be null! Use getLogo()! */
    public MediaDesc logo;

    /** This group's brief description. */
    public String blurb;

    /** The scene id of this group's hall. */
    public int homeSceneId;

    /** The canonical image from the group's home scene. This is not resolved by default. */
    public MediaDesc homeSnapshot;

    /** The number of people online in this group's scenes (from the popular places snapshot). */
    public int population;

    /** The number of members in this group. */
    public int memberCount;

    /** If the group is an official whirled group. */
    public boolean official;

    /**
     * Get the group's logo.
     */
    public MediaDesc getLogo ()
    {
        return Group.logo(logo);
    }
}
