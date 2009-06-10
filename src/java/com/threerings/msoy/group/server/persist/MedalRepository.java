//
// $Id$

package com.threerings.msoy.group.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.And;
import com.samskivert.depot.operator.Equals;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Manages the persistent store of Medal data.
 */
@Singleton @BlockingThread
public class MedalRepository extends DepotRepository
{
    @Inject public MedalRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * If the medal's medalId is valid, it will update that medalId's row in the database.
     * Otherwise, it will insert a new row.
     */
    public boolean storeMedal (MedalRecord medal)
    {
        return store(medal);
    }

    /**
     * Returns the required MedalRecord
     */
    public MedalRecord loadMedal (int medalId)
    {
        return load(MedalRecord.getKey(medalId));
    }

    public List<MedalRecord> loadMedals (Collection<Integer> medalIds)
    {
        if (medalIds.size() == 0) {
            return Collections.emptyList();
        }
        return loadAll(MedalRecord.class, medalIds);
    }

    /**
     * Returns true if the groupId and name combination is already in use.
     */
    public boolean groupContainsMedalName (int groupId, String name)
    {
        Equals groupIdEquals = MedalRecord.GROUP_ID.eq(groupId);
        Equals nameEquals = MedalRecord.NAME.eq(name);
        return load(MedalRecord.class, new Where(new And(groupIdEquals, nameEquals))) != null;
    }

    /**
     * Returns a list of MedalRecords for the medals that belong to the given group.
     */
    public List<MedalRecord> loadGroupMedals (int groupId)
    {
        return findAll(MedalRecord.class, new Where(MedalRecord.GROUP_ID, groupId));
    }

    public EarnedMedalRecord loadEarnedMedal (int memberId, int medalId)
    {
        return load(EarnedMedalRecord.getKey(medalId, memberId));
    }

    /**
     * Returns a list of the EarnedMedalRecords that have been earned by the given individual.
     */
    public List<EarnedMedalRecord> loadEarnedMedals (int memberId)
    {
        return findAll(EarnedMedalRecord.class, new Where(EarnedMedalRecord.MEMBER_ID, memberId));
    }

    /**
     * Returns a list of the recently earned EarnedMedalRecords.
     */
    public List<EarnedMedalRecord> loadRecentEarnedMedals (int memberId, int limit)
    {
        return findAll(EarnedMedalRecord.class, new Where(EarnedMedalRecord.MEMBER_ID, memberId),
            new Limit(0, limit), OrderBy.descending(EarnedMedalRecord.WHEN_EARNED));
    }

    /**
     * Returns a list of the EarnedMedalRecords that match the given set of medalIds.
     */
    public List<EarnedMedalRecord> loadEarnedMedals (Collection<Integer> medalIds)
    {
        if (medalIds.size() == 0) {
            return Collections.emptyList();
        }
        return findAll(EarnedMedalRecord.class,
            new Where(EarnedMedalRecord.MEDAL_ID.in(medalIds)));
    }

    /**
     * Awards the given medal to the given member.
     *
     * @throws DuplicateKeyException If this member has already earned that medal.
     */
    public void awardMedal (int memberId, int medalId)
    {
        EarnedMedalRecord earnedMedalRec = new EarnedMedalRecord();
        earnedMedalRec.memberId = memberId;
        earnedMedalRec.medalId = medalId;
        earnedMedalRec.whenEarned = new Timestamp(System.currentTimeMillis());
        insert(earnedMedalRec);
    }

    public boolean deleteEarnedMedal (int memberId, int medalId)
    {
        int result = delete(EarnedMedalRecord.getKey(medalId, memberId));
        return result > 0;
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // note: this will full table scan, but that might be OK
        deleteAll(EarnedMedalRecord.class,
                  new Where(EarnedMedalRecord.MEMBER_ID.in(memberIds)));
    }

    @Override
    protected void getManagedRecords(Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MedalRecord.class);
        classes.add(EarnedMedalRecord.class);
    }
}
