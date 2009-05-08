//
// $Id$

package com.threerings.msoy.admin.server.persist;

import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Logic;
import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.gwt.ABTest;

/**
 * Maintains persistent data for a/b tests
 */
@Singleton @BlockingThread
public class ABTestRepository extends DepotRepository
{
    @Inject public ABTestRepository (PersistenceContext perCtx)
    {
        super(perCtx);

        _ctx.registerMigration(ABTestRecord.class, new SchemaMigration.Drop(5, "affiliate"));
        _ctx.registerMigration(ABTestRecord.class, new SchemaMigration.Drop(5, "vector"));
        _ctx.registerMigration(ABTestRecord.class, new SchemaMigration.Drop(5, "creative"));

        // These were once Date and were changed to Timestamp, MySQL treats the 2 types differently
        _ctx.registerMigration(ABTestRecord.class,
            new SchemaMigration.Retype(7, ABTestRecord.STARTED));
        _ctx.registerMigration(ABTestRecord.class,
            new SchemaMigration.Retype(7, ABTestRecord.ENDED));

        _ctx.registerMigration(ABTestRecord.class, new SchemaMigration.Drop(9, "abTestId"));
    }

    /**
     * Loads all test information with newest tests first
     */
    public List<ABTestRecord> loadTests ()
    {
        return findAll(ABTestRecord.class, OrderBy.descending(ABTestRecord.TEST_ID));
    }

    /**
     * Loads up all tests that are enabled and require a cookie to be assigned on landing.
     */
    public List<ABTestRecord> loadTestsWithLandingCookies ()
    {
        return findAll(ABTestRecord.class, new Where(new Logic.And(
            ABTestRecord.ENABLED, ABTestRecord.LANDING_COOKIE)));
    }

    /**
     * Loads a single test by the unique string identifier (name)
     */
    public ABTestRecord loadTestByName (String name)
    {
        return load(ABTestRecord.class, new Where(ABTestRecord.NAME, name));
    }

    /**
     * Inserts the supplied record into the database.
     */
    public void insertABTest (ABTest test)
    {
        try {
            ABTestRecord record = ABTestRecord.class.newInstance();
            record.fromABTest(test);
            insert(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the supplied record in the database.
     */
    public void updateABTest (ABTest test)
    {
        try {
            ABTestRecord record = ABTestRecord.class.newInstance();
            record.fromABTest(test);
            update(record);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ABTestRecord.class);
    }
}
