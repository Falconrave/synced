//
// $Id: GroupCard.java 8844 2008-04-15 17:05:43Z nathan $

package com.threerings.msoy.web.data;

import java.util.Collections;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Detailed information on a single Group/Whirled for the "My Whirleds" page.
 */
public class MyGroupCard
    implements IsSerializable
{
    /** Default sort by current population, then by latest thread, then name */
    public static final byte SORT_BY_PEOPLE_ONLINE = 0;
    /** Alternate sort by name */
    public static final byte SORT_BY_NAME = 1;
    /** Default sort by whether I manage, then by population, then latest thread, then name */
    public static final byte SORT_BY_MANAGER = 2;
    /** Alternate sort by latest post, then population, then name */
    public static final byte SORT_BY_NEWEST_POST = 3;
    
    /** The group's name. */
    public GroupName name;

    /** The groups's logo (or the default). */
    public MediaDesc logo = Group.getDefaultGroupLogoMedia();

    /** This group's brief description. */
    public String blurb;

    /** The scene id of this group's hall. */
    public int homeSceneId;

    /** The number of people online in this group's scenes (as of the last snapshot). */
    public int population;
    
    /** Total number of threads in this group's discussions */
    public int numThreads;
    
    /** Total number of posts to all threads in this group's discussions */
    public int numPosts;
    
    /** The member's rank in the group. */
    public byte rank;
    
    /** Most recent thread for this Whirled on the My Discussions page */
    public ForumThread latestThread;
    
    
}
