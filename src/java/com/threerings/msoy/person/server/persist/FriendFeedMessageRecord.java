//
// $Id$

package com.threerings.msoy.person.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FriendFeedMessage;

/**
 * Contains persistent data on a feed message distributed to a member's friends.
 */
@Entity
public class FriendFeedMessageRecord extends FeedMessageRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FriendFeedMessageRecord> _R = FriendFeedMessageRecord.class;
    public static final ColumnExp ACTOR_ID = colexp(_R, "actorId");
    public static final ColumnExp MESSAGE_ID = colexp(_R, "messageId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp DATA = colexp(_R, "data");
    public static final ColumnExp POSTED = colexp(_R, "posted");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The member id of the originator of this message. */
    @Index(name="ixActorId")
    public int actorId;

    @Override // from FeedMessageRecord
    public void addReferences (IntSet memberIds, IntSet groupIds)
    {
        memberIds.add(actorId);
    }

    @Override // from FeedMessageRecord
    public FeedMessage toMessage (IntMap<MemberName> memberNames, IntMap<GroupName> groupNames)
    {
        return new FriendFeedMessage(getType(), memberNames.get(actorId), getData(), getPosted());
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FriendFeedMessageRecord}
     * with the supplied key values.
     */
    public static Key<FriendFeedMessageRecord> getKey (int messageId)
    {
        return newKey(_R, messageId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MESSAGE_ID); }
    // AUTO-GENERATED: METHODS END
}
