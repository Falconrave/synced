//
// $Id$

package com.threerings.msoy.server;

import java.sql.Timestamp;
import java.util.Collections;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.stats.data.IntSetStat;
import com.threerings.stats.data.IntSetStatAdder;
import com.threerings.stats.data.IntStat;
import com.threerings.stats.data.IntStatIncrementer;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatModifier;
import com.threerings.stats.data.StatSet;
import com.threerings.stats.server.persist.StatRepository;

import com.threerings.msoy.badge.data.BadgeProgress;
import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.server.BadgeLogic;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.DeploymentConfig;

import static com.threerings.msoy.Log.log;

/**
 * Services for modifying a member's stats from servlet code.
 */
@BlockingThread @Singleton
public class StatLogic
{
    /**
     * Increments an integer statistic for the specified player.
     *
     * @exception ClassCastException thrown if the registered type of the specified stat is not a
     * {@link IntStat}.
     */
    public void incrementStat (int memberId, Stat.Type type, int delta)
    {
        updateStat(memberId, new IntStatIncrementer(type, delta));
    }

    /**
     * Adds an integer to an IntSetStat for the specified player.
     *
     * @exception ClassCastException thrown if the registered type of the specified stat is not a
     * {@link IntSetStat}.
     */
    public void addToSetStat (int memberId, Stat.Type type, int value)
    {
        updateStat(memberId, new IntSetStatAdder(type, value));
    }

    /**
     * Attempts to apply the given stat modification. If the stat modification fails MAX_TRIES
     * times, a warning will be logged; otherwise, a MemberNodeAction will be posted.
     */
    public void updateStat (int memberId, StatModifier<? extends Stat> modifier)
    {
        if (!DeploymentConfig.devDeployment) {
            return; // TODO remove this when the Passport system goes live
        }

        Stat updatedStat = null;
        try {
            // first update the stat in the database
            updatedStat = _statRepo.updateStat(memberId, modifier);
            if (updatedStat != null) {
                MemberNodeActions.statUpdated(memberId, modifier);
                log.info("updateStat succeeded", "memberId", memberId,
                    "statType", modifier.getType().name());
            }
        } catch (PersistenceException pe) {
            log.warning("updateStat failed", "memberId", memberId, "type", modifier.getType(), pe);
        }

        // if the stat was updated, find any badges that may have been affected by the stat
        // modification, and update them as well
        if (updatedStat != null && updatedStat.getType() instanceof StatType) {
            final StatType statType = (StatType)updatedStat.getType();
            // Create a StatSet that holds just this one stat, so that we can pass it to
            // BadgeType.getProgress(). Log a warning if the BadgeType tries to access any Stat
            // other than the one we're dealing with now.
            StatSet singleStatSet = new StatSet(Collections.singleton(updatedStat)) {
                @Override protected Stat getStat (Stat.Type type) {
                    Stat stat = super.getStat(type);
                    if (stat == null) {
                        log.warning("BadgeType tried to access a non-existent Stat",
                            "Stat.Type", type, "Existing StatType", statType);
                    }
                    return stat;
                }
            };
            for (BadgeType badgeType : BadgeType.values()) {
                if (badgeType.getRelevantStat() == statType) {
                    updateBadge(memberId, badgeType, singleStatSet);
                }
            }
        }
    }

    /**
     * Called by updateStat for each BadgeType that needs to be updated in tandem with a particular
     * stat.
     */
    protected void updateBadge (int memberId, BadgeType badgeType, StatSet updatedStat)
    {
        BadgeProgress progress = badgeType.getProgress(updatedStat);

        if (progress.highestLevel >= 0) {
            // we've earned a badge
            EarnedBadgeRecord earnedBadge;
            try {
                earnedBadge = _badgeRepo.loadEarnedBadge(memberId, badgeType.getCode());
            } catch (PersistenceException pe) {
                log.warning("loadEarnedBadge failed", "memberId", memberId, "BadgeType", badgeType);
                return;
            }

            if (earnedBadge == null) {
                earnedBadge = new EarnedBadgeRecord();
                earnedBadge.memberId = memberId;
                earnedBadge.badgeCode = badgeType.getCode();
                earnedBadge.level = -1;
            }

            if (earnedBadge.level < progress.highestLevel) {
                earnedBadge.level = progress.highestLevel;
                earnedBadge.whenEarned = new Timestamp(System.currentTimeMillis());
                try {
                    _badgeLogic.awardBadge(earnedBadge, true);
                    log.info("awardBadge succeeded", "BadgeType", badgeType, "EarnedBadgeRecord",
                        earnedBadge);
                } catch (PersistenceException pe) {
                    log.warning("awardBadge failed", "BadgeType", badgeType,  "EarnedBadgeRecord",
                        earnedBadge);
                    return;
                }
            }
        }

        if (progress.highestLevel >= badgeType.getNumLevels() - 1) {
            // If we've reached the highest badge level, delete the InProgressBadgeRecord.
            // Note - no MemberNodeAction is sent here; MemberObject removes obsolete
            // InProgressBadgeRecords when badges are awarded.
            try {
                _badgeRepo.deleteInProgressBadge(memberId, badgeType.getCode());
                log.info("deleteInProgressBadge succeeded", "memberId", memberId, "BadgeType",
                    badgeType);
            } catch (PersistenceException pe) {
                log.warning("deleteInProgressBadge failed", "memberId", memberId, "BadgeType",
                    badgeType);
            }

        } else {
            // otherwise, update the InProgressBadgeRecord
            InProgressBadgeRecord inProgressBadge;
            try {
                inProgressBadge = _badgeRepo.loadInProgressBadge(memberId, badgeType.getCode());
            } catch (PersistenceException pe) {
                log.warning("loadInProgressBadge failed", "memberId", memberId, "BadgeType",
                    badgeType);
                return;
            }

            if (inProgressBadge == null) {
                inProgressBadge = new InProgressBadgeRecord();
                inProgressBadge.memberId = memberId;
                inProgressBadge.badgeCode = badgeType.getCode();
                inProgressBadge.nextLevel = -1;
            }

            // we store badge progress in quantized increments to prevent excess database
            // traffic for minor progress bumps
            float quantizedProgress =
                (float)(Math.floor(progress.getNextLevelProgress() / MIN_BADGE_PROGRESS_INCREMENT) *
                        MIN_BADGE_PROGRESS_INCREMENT);

            if (progress.highestLevel >= inProgressBadge.nextLevel ||
                    quantizedProgress > inProgressBadge.progress) {
                inProgressBadge.nextLevel = progress.highestLevel + 1;
                inProgressBadge.progress = quantizedProgress;
                try {
                    _badgeRepo.storeInProgressBadge(inProgressBadge);
                    log.info("storeInProgressBadge succeeded", "BadgeType", badgeType,
                        "InProgressBadgeRecord", inProgressBadge);
                } catch (PersistenceException pe) {
                    log.warning("storeInProgressBadge failed", "BadgeType", badgeType,
                        "InProgressBadgeRecord", inProgressBadge);
                    return;
                }

                // if the player is logged in, let their MemberObject know what happened
                MemberNodeActions.inProgressBadgeUpdated(inProgressBadge);
            }
        }
    }

    @Inject StatRepository _statRepo;
    @Inject BadgeRepository _badgeRepo;
    @Inject BadgeLogic _badgeLogic;

    protected static final float MIN_BADGE_PROGRESS_INCREMENT = 0.1f;
}
