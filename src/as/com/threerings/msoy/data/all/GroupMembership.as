//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;
import com.threerings.util.Long;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Summarizes a person's membership in a group.
 */
public class GroupMembership
    implements Streamable, DSet_Entry
{
    /** Unused rank code. This is not ever stored in a GroupMembership record, but is useful for
     * methods that return a user's rank as a byte. */
    public static const RANK_NON_MEMBER :int = 0;

    /** Rank code for a member. */
    public static const RANK_MEMBER :int = 1;

    /** Rank code for a manager. */
    public static const RANK_MANAGER :int = 2;

    /** The name and id of the member of the group. <em>Note:</em> this will be null in the records
     * maintained in a member's MemberObject. */
    public var member :MemberName;

    /** The group's identity. <em>Note:</em> this will be null in the records contained in a
     * GroupDetail.members list.*/
    public var group :GroupName;

    /** The member's rank in the group. */
    public var rank :int;

    /** The date this member's rank was assigned, as represented by java.util.Date.getTime() */
    public var rankAssignedDate :Long;

    /**
     * Returns true if the supplied rank is a valid rank (not {@link #RANK_NON_MEMBER} or an
     * otherwise invalid number.
     */
    public static function isValidRank (rank :int) :Boolean
    {
        return rank >= RANK_MEMBER && rank <= RANK_MANAGER;
    }

    public function GroupMembership ()
    {
    }

    /**
     * Get the date this member's rank was assigned on as a Date object.
     */
    public function getRankAssignedDate () :Date
    {
        throw new Error("Not implemented");
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return group;
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        member = (ins.readObject() as MemberName);
        group = (ins.readObject() as GroupName);
        rank = ins.readByte();
        rankAssignedDate = new Long(ins.readInt(), ins.readInt());
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(member);
        out.writeObject(group);
        out.writeByte(rank);
        out.writeInt(rankAssignedDate == null ? 0 : rankAssignedDate.low);
        out.writeInt(rankAssignedDate == null ? 0 : rankAssignedDate.high);
    }
}
}
