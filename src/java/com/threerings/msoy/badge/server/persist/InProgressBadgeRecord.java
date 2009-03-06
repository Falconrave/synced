//
// $Id$

package com.threerings.msoy.badge.server.persist;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.InProgressBadge;

public class InProgressBadgeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<InProgressBadgeRecord> _R = InProgressBadgeRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp BADGE_CODE = colexp(_R, "badgeCode");
    public static final ColumnExp NEXT_LEVEL = colexp(_R, "nextLevel");
    public static final ColumnExp PROGRESS = colexp(_R, "progress");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** Before being stored in the database, InProgressBadgeRecords should have their progress
     * rounded down to a multiple of PROGRESS_INCREMENT (to prevent hammering the database with
     * insignificant badge progress updates). See {@link #quantizeProgress}. */
    public static final float PROGRESS_INCREMENT = 0.1f;

    /** Transforms a persistent record to a runtime record. */
    public static Function<InProgressBadgeRecord, InProgressBadge> TO_BADGE =
        new Function<InProgressBadgeRecord, InProgressBadge>() {
        public InProgressBadge apply (InProgressBadgeRecord record) {
            return record.toBadge();
        }
    };

    /** The id of the member that holds this badge. */
    @Id
    public int memberId;

    /** The code that uniquely identifies the badge type. */
    @Id
    public int badgeCode;

    /** The badge level that the player is currently working towards. */
    public int nextLevel;

    /** The progress that has been made on the badge, in [0, 1). */
    public float progress;

    /**
     * Constructs an empty InProgressBadgeRecord.
     */
    public InProgressBadgeRecord ()
    {
    }

    /**
     * Constructs an InProgressBadgeRecord from an InProgressBadge.
     */
    public InProgressBadgeRecord (int memberId, InProgressBadge badge)
    {
        Preconditions.checkArgument(memberId > 0, "memberId must be positive.");
        this.memberId = memberId;
        this.badgeCode = badge.badgeCode;
        this.nextLevel = badge.level;
        this.progress = badge.progress;
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public InProgressBadge toBadge ()
    {
        BadgeType type = BadgeType.getType(badgeCode);
        BadgeType.Level level = type.getLevel(nextLevel);
        int coinReward = level != null ? level.coinValue : 0;
        String levelUnits = BadgeType.getRequiredUnitsString(badgeCode, nextLevel);
        float progress = type.progressValid(nextLevel) ? this.progress : -1;
        return new InProgressBadge(badgeCode, nextLevel, levelUnits, coinReward, progress);
    }

    public static float quantizeProgress (float progress)
    {
        return (float)(Math.floor(progress / PROGRESS_INCREMENT) * PROGRESS_INCREMENT);
    }

    /**
     * @return a String representation of the record.
     */
    @Override
    public String toString ()
    {
        return "memberId=" + memberId + " BadgeType=" + BadgeType.getType(badgeCode) +
            " nextLevel=" + nextLevel + " progress=" + progress;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link InProgressBadgeRecord}
     * with the supplied key values.
     */
    public static Key<InProgressBadgeRecord> getKey (int memberId, int badgeCode)
    {
        return new Key<InProgressBadgeRecord>(
                InProgressBadgeRecord.class,
                new ColumnExp[] { MEMBER_ID, BADGE_CODE },
                new Comparable[] { memberId, badgeCode });
    }
    // AUTO-GENERATED: METHODS END
}
