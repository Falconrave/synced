//
// $Id$

package com.threerings.msoy.group.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains extra information about a group.  This should be used to hold information that is only
 * needed on the GroupView page itself, and not in other places that Groups are fetched.
 */
public class GroupExtras
    implements Streamable, IsSerializable
{
    /** The group's charter, or null if one has yet to be set. */
    public String charter;

    /** The URL of the group's homepage. */
    public String homepageUrl;

    /** The catalog category to link to. */
    public byte catalogItemType;

    /** The catalog tag to link to. */
    public String catalogTag;
}
