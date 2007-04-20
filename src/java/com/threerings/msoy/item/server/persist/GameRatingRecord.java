//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Games. */
@Entity
@Table
public class GameRatingRecord extends RatingRecord<GameRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameRatingRecord}
     * with the supplied key values.
     */
    public static Key<GameRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<GameRatingRecord>(
                GameRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
