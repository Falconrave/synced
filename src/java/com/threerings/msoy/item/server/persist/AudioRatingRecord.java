//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

/** Rating records for Audios. */
@Entity
@Table
public class AudioRatingRecord extends RatingRecord<AudioRecord>
{

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #AudioRatingRecord}
     * with the supplied key values.
     */
    public static Key<AudioRatingRecord> getKey (int itemId, int memberId)
    {
        return new Key<AudioRatingRecord>(
                AudioRatingRecord.class,
                new String[] { ITEM_ID, MEMBER_ID },
                new Comparable[] { itemId, memberId });
    }
    // AUTO-GENERATED: METHODS END
}
