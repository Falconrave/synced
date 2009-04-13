//
// $Id$

package com.threerings.msoy.group.gwt;

import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * Extends the {@link MemberCard} with information on a group member's rank.
 */
public class GroupMemberCard extends MemberCard
{
    /** The member's rank in the group. */
    public Rank rank;

    /** When this member's rank was assigned in millis since the epoch. */
    public long rankAssigned;
}
