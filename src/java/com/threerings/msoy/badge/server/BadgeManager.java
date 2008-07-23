//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.EarnedBadge;
import com.threerings.msoy.data.MemberObject;

/**
 * Handles badge related services for the world server.
 */
@Singleton @EventThread
public class BadgeManager
{
    /**
     * Awards a badge of the specified type to the user if they don't already have it.
     */
    public void awardBadge (MemberObject user, BadgeType badgeType)
    {
        if (!user.badges.containsBadge(badgeType)) {
            List<BadgeType> badgeList = Lists.newArrayList();
            badgeList.add(badgeType);
            awardBadges(user, badgeList);
        }
    }

    /**
     * For each Badge type, awards the Badge to the user if the Badge's award conditions
     * have been met.
     */
    public void updateBadges (MemberObject user)
    {
        // guests are not awarded badges
        if (user.isGuest()) {
            return;
        }

        // iterate the list of badges to see if the player has won any new ones
        List<BadgeType> newBadges = null;
        for (BadgeType badgeType : BadgeType.values()) {
            if (!user.badges.containsBadge(badgeType) && badgeType.hasEarned(user)) {
                if (newBadges == null) {
                    newBadges = Lists.newArrayList();
                }
                newBadges.add(badgeType);
            }
        }

        if (newBadges != null) {
            awardBadges(user, newBadges);
        }
    }

    protected void awardBadges (final MemberObject user, final List<BadgeType> badgeTypes)
    {
        final long whenEarned = System.currentTimeMillis();

        // create badges and stick them in the MemberObject
        final List<EarnedBadge> badges = createBadges(badgeTypes, whenEarned);
        for (EarnedBadge badge : badges) {
            user.badges.addBadge(badge);
        }

        // stick the badges in the database
        _invoker.postUnit(new WriteOnlyUnit("awardBadges") {
            public void invokePersist () throws PersistenceException {
                for (BadgeType badgeType : badgeTypes) {
                    // BadgeLogic.awardBadge handles putting the badge in the repository and
                    // publishing a member feed about the event
                    _badgeLogic.awardBadge(user.getMemberId(), badgeType, whenEarned);
                }
            }
            public void handleFailure (Exception error) {
                // rollback the changes to the user's BadgeSet
                for (EarnedBadge badge : badges) {
                    user.badges.removeBadge(badge);
                }
                super.handleFailure(error);
            }
            protected String getFailureMessage () {
                StringBuilder builder = new StringBuilder("Failed to award badges: ");
                for (BadgeType badgeType : badgeTypes) {
                    builder.append(badgeType.name()).append(", ");
                }
                return builder.toString();
            }
        });
    }

    protected static List<EarnedBadge> createBadges (List<BadgeType> badgeTypes, long whenEarned)
    {
        List<EarnedBadge> badges = Lists.newArrayListWithExpectedSize(badgeTypes.size());
        for (BadgeType type : badgeTypes) {
            badges.add(new EarnedBadge(type, whenEarned));
        }
        return badges;
    }

    @Inject protected BadgeLogic _badgeLogic;
    @Inject protected @MainInvoker Invoker _invoker;
}
