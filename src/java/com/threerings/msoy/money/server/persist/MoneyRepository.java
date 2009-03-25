//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CacheInvalidator.TraverseWithFilter;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.Arithmetic;
import com.samskivert.depot.operator.SQLOperator;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic.And;
import com.samskivert.jdbc.DatabaseLiaison;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.persist.CountRecord;

import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.server.NotEnoughMoneyException;

import static com.threerings.msoy.Log.log;

/**
 * Interface for retrieving and persisting entities in the money service.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
@Singleton
@BlockingThread
@NotThreadSafe
public class MoneyRepository extends DepotRepository
{
    /**
     * Thrown when accumulate*() or deduct*() are called with an invalid memberId.
     */
    public static class NoSuchMemberException extends DatabaseException
    {
        public NoSuchMemberException (int memberId)
        {
            super("Member does not have a money record (" + memberId + ")");
        }
    }

    @Inject
    public MoneyRepository (final PersistenceContext ctx)
    {
        super(ctx);

        _ctx.registerMigration(MoneyTransactionRecord.class, new SchemaMigration(6) {
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                String ixName = _tableName + "_ixCurrency";
                if (liaison.tableContainsIndex(conn, _tableName, ixName)) {
                    log.info("Dropping index " + ixName + " using liaison: " + liaison);
                    liaison.dropIndex(conn, _tableName, ixName);
                    return 1;
                }
                log.warning(_tableName + " does not contain index " + ixName + " for dropping.");
                return 0;
            }
        });

        if (!DeploymentConfig.devDeployment) {
            registerMigration(new DataMigration("2009_02_12_dumpBarsIntoExchange") {
                @Override public void invoke () throws DatabaseException {
                    adjustBarPool(15000);
                }
            });
        }
    }

    /**
     * Create a MemberAccountRecord when we create an account.
     */
    public MemberAccountRecord create (int memberId)
    {
        MemberAccountRecord rec = new MemberAccountRecord(memberId);
        insert(rec);
        return rec;
    }

    /**
     * Retrieves a member's account info by member ID.
     */
    public MemberAccountRecord load (int memberId)
    {
        return load(MemberAccountRecord.class, memberId);
    }

    /**
     * Retries all member accounts from the given list of member IDs.
     */
    public List<MemberAccountRecord> loadAll (Set<Integer> memberIds)
    {
        return loadAll(MemberAccountRecord.class, memberIds);
    }

    /**
     * Accumulate money, return a partially-populated MoneyTransactionRecord for
     * storing.
     *
     * @param updateAcc true if this transaction is "accumulating", meaning that they
     * earned the coins rather than got them through change, for example.
     */
    protected MoneyTransactionRecord accumulate (
        int memberId, Currency currency, int amount, boolean updateAcc)
    {
        Preconditions.checkArgument(amount >= 0, "Amount to accumulate must be 0 or greater.");

        ColumnExp currencyCol = MemberAccountRecord.getColumn(currency);
        ImmutableMap.Builder<ColumnExp, SQLExpression> builder = ImmutableMap.builder();
        builder.put(currencyCol, new Arithmetic.Add(currencyCol, amount));
        if (updateAcc) {
            ColumnExp currencyAccCol = MemberAccountRecord.getAccColumn(currency);
            builder.put(currencyAccCol, new Arithmetic.Add(currencyAccCol, amount));
        }

        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(memberId);
        int count = updateLiteral(MemberAccountRecord.class, key, key, builder.build());
        if (count == 0) {
            // the accumulate should always work, so if we mod'd 0 rows, it means there's
            // no member.
            throw new NoSuchMemberException(memberId);
        }
        // TODO: be able to get the balance at the same time as the update, pending Depot changes
        int balance = load(MemberAccountRecord.class, key).getAmount(currency);

        return new MoneyTransactionRecord(memberId, currency, amount, balance);
    }

    /**
     * Deduct money from the specified member's money.
     * If the user does not have enough money, a NotEnoughMoneyException will be thrown.
     *
     * @return a partially filled-out MTR that should later either be passed to
     * rollbackDeduction() or filled-in and passed to storeTransaction().
     */
    public MoneyTransactionRecord deduct (
        int memberId, Currency currency, int amount, boolean allowFree)
        throws NotEnoughMoneyException
    {
        Preconditions.checkArgument(amount >= 0, "Amount to deduct must be 0 or greater.");

        ColumnExp currencyCol = MemberAccountRecord.getColumn(currency);
        Map<ColumnExp, SQLExpression> updates = new ImmutableMap.Builder<ColumnExp, SQLExpression>()
            .put(currencyCol, new Arithmetic.Sub(currencyCol, amount))
            .build();
        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(memberId);
        Where where = new Where(new And(
            new Conditionals.Equals(MemberAccountRecord.MEMBER_ID, memberId),
            new Conditionals.GreaterThanEquals(currencyCol, amount)));

        int count = updateLiteral(MemberAccountRecord.class, where, key, updates);
        // TODO: be able to get the balance at the same time as the update, pending Depot changes
        MemberAccountRecord mar = load(MemberAccountRecord.class, key);
        if (mar == null) {
            throw new NoSuchMemberException(memberId);
        }
        int balance = mar.getAmount(currency);
        if (count == 0 && !allowFree) {
            throw new NotEnoughMoneyException(memberId, currency, amount, balance);
        }

        // Return the amount reserved, or 0 if it didn't work, but allowFree==true
        return new MoneyTransactionRecord(memberId, currency, (count == 0) ? 0 : -amount, balance);
    }

    /**
     * Deduct money from the specified member's money and immediately store the
     * transaction.
     */
    public MoneyTransactionRecord deductAndStoreTransaction (
        int memberId, Currency currency, int amount,
        TransactionType type, String description, Object subject)
        throws NotEnoughMoneyException
    {
        MoneyTransactionRecord tx = deduct(memberId, currency, amount, false);
        tx.fill(type, description, subject);
        storeTransaction(tx);
        return tx;
    }

    /**
     * Accumulate and store the specified MoneyTransaction.
     */
    public MoneyTransactionRecord accumulateAndStoreTransaction (
        int memberId, Currency currency, int amount,
        TransactionType type, String description, Object subject, boolean updateAcc)
    {
        MoneyTransactionRecord tx = accumulate(memberId, currency, amount, updateAcc);
        tx.fill(type, description, subject);
        storeTransaction(tx);
        return tx;
    }

    /**
     * Accumulate and store the specified MoneyTransaction.
     */
    public MoneyTransactionRecord accumulateAndStoreTransaction (
        int memberId, Currency currency, int amount,
        TransactionType type, String description, Object subject,
        int referenceTxId, int referenceMemberId, boolean updateAcc)
    {
        MoneyTransactionRecord tx = accumulate(memberId, currency, amount, updateAcc);
        tx.fill(type, description, subject);
        tx.referenceTxId = referenceTxId;
        tx.referenceMemberId = referenceMemberId;
        storeTransaction(tx);
        return tx;
    }

    /**
     * Rollback a deduction. The transaction is not saved, it is merely used to reference
     * the values.
     */
    public void rollbackDeduction (MoneyTransactionRecord deduction)
    {
        Preconditions.checkArgument(deduction.amount <= 0,
            "Only deductions can be rolled back.");
        Preconditions.checkArgument(deduction.id == 0,
            "Transaction has already been inserted!");

        ColumnExp currencyCol = MemberAccountRecord.getColumn(deduction.currency);
        Map<ColumnExp, SQLExpression> updates = new ImmutableMap.Builder<ColumnExp, SQLExpression>()
            .put(currencyCol, new Arithmetic.Add(currencyCol, -deduction.amount))
            .build();
        Key<MemberAccountRecord> key = MemberAccountRecord.getKey(deduction.memberId);

        int count = updateLiteral(MemberAccountRecord.class, key, key, updates);
        if (count == 0) {
            // This should never happen, because the deduction must have already worked!
            throw new NoSuchMemberException(deduction.memberId);
        }
    }

    /**
     * Store a fully-populated transaction that has come from deduct or accumulate.
     */
    public void storeTransaction (MoneyTransactionRecord transaction)
    {
        Preconditions.checkArgument(transaction.id == 0,
            "Transaction has already been inserted!");
        Preconditions.checkArgument(transaction.transactionType != null,
            "TransactionType must be populated.");
        Preconditions.checkArgument(transaction.description != null,
            "Description must be populated.");

        insert(transaction);
    }

    public List<MoneyTransactionRecord> getTransactions (
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency,
        int start, int count, boolean descending)
    {
        // select * from MemberAccountRecord where type = ? and transactionType in (?)
        // and memberId=? order by timestamp
        List<QueryClause> clauses = Lists.newArrayList();
        populateSearch(clauses, memberId, transactionTypes, currency);

        clauses.add(descending ?
                    OrderBy.descending(MoneyTransactionRecord.TIMESTAMP) :
                    OrderBy.ascending(MoneyTransactionRecord.TIMESTAMP));

        if (count != Integer.MAX_VALUE) {
            clauses.add(new Limit(start, count));
        }

        return findAll(MoneyTransactionRecord.class, clauses);
    }

    public int getTransactionCount (
        int memberId, EnumSet<TransactionType> transactionTypes, Currency currency)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(MoneyTransactionRecord.class));
        populateSearch(clauses, memberId, transactionTypes, currency);
        return load(CountRecord.class, clauses).count;
    }

    public int deleteOldTransactions (Currency currency, long maxAge)
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - maxAge);
        Where where = new Where(
            new And(new Conditionals.Equals(MoneyTransactionRecord.CURRENCY, currency),
                    new Conditionals.LessThan(MoneyTransactionRecord.TIMESTAMP, cutoff)));
        return deleteAll(MoneyTransactionRecord.class, where, null /* no cache invalidation */);
    }

    /**
     * Loads recent transactions that were inserted with the given subject.
     * @param subject the subject of the transaction to search for
     * @param from offset in the list of transactions to return first
     * @param count maximum number of transactions to return
     * @param descending if set, transactions are ordered newest to oldest
     */
    public List<MoneyTransactionRecord> getTransactionsForSubject (
        Object subject, int from, int count, boolean descending)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(makeSubjectSearch(subject));

        clauses.add(descending ?
            OrderBy.descending(MoneyTransactionRecord.TIMESTAMP) :
            OrderBy.ascending(MoneyTransactionRecord.TIMESTAMP));

        if (count != Integer.MAX_VALUE) {
            clauses.add(new Limit(from, count));
        }

        return findAll(MoneyTransactionRecord.class, clauses);
    }

    /**
     * Loads the number of recent transactions that were inserted with the given subject.
     * @param subject the subject of the transaction to search for
     */
    public int getTransactionCountForSubject (Object subject)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(MoneyTransactionRecord.class));
        clauses.add(makeSubjectSearch(subject));
        return load(CountRecord.class, clauses).count;
    }

    /**
     * Get the number of bars and the coin balance in the bar pool.
     */
    public int[] getBarPool (int defaultBarPoolSize)
    {
        BarPoolRecord bpRec = load(BarPoolRecord.class, BarPoolRecord.KEY);
        if (bpRec == null) {
            bpRec = createBarPoolRecord(defaultBarPoolSize);
        }
        return new int[] { bpRec.barPool, bpRec.coinBalance };
    }

    /**
     * Adjust the bar pool as a result of an exchange.
     *
     * @param barDelta a positive number if bars were used to purchase coin-listed stuff
     *                 a negative number if coins were used to purchase bar-listed stuff.
     */
    public void recordExchange (int barDelta, int coinDelta, float rate, int referenceTxId)
    {
        Map<ColumnExp, SQLExpression> updates = new ImmutableMap.Builder<ColumnExp, SQLExpression>()
            .put(BarPoolRecord.BAR_POOL, new Arithmetic.Add(BarPoolRecord.BAR_POOL, barDelta))
            .put(BarPoolRecord.COIN_BALANCE,
                new Arithmetic.Add(BarPoolRecord.COIN_BALANCE, coinDelta))
            .build();
        updateLiteral(BarPoolRecord.class, BarPoolRecord.KEY, BarPoolRecord.KEY, updates);

        insert(new ExchangeRecord(barDelta, coinDelta, rate, referenceTxId));
    }

    public List<ExchangeRecord> getExchangeData (int start, int count)
    {
        return findAll(ExchangeRecord.class, OrderBy.descending(ExchangeRecord.TIMESTAMP),
            new Limit(start, count));
    }

    /**
     * Adjust the bar pool.
     * This is used in two circumstances:
     * 1. When we react to adjustments in the *size* of the target bar pool size, we automatically
     * remove or add bars.
     * 2. Sometimes Daniel dumps more bars in, cuz we're crazy like that. These are done manually.
     */
    public void adjustBarPool (int delta)
    {
        Map<ColumnExp, SQLExpression> updates = new ImmutableMap.Builder<ColumnExp, SQLExpression>()
            .put(BarPoolRecord.BAR_POOL, new Arithmetic.Add(BarPoolRecord.BAR_POOL, delta))
            .build();
        updateLiteral(BarPoolRecord.class, BarPoolRecord.KEY, BarPoolRecord.KEY, updates);
    }

    public int getExchangeDataCount ()
    {
        return load(CountRecord.class, new FromOverride(ExchangeRecord.class)).count;
    }

    public int deleteOldExchangeRecords (long maxAge)
    {
        final long oldestTimestamp = System.currentTimeMillis() - maxAge;
        return deleteAll(ExchangeRecord.class, new Where(new Conditionals.LessThan(
            ExchangeRecord.TIMESTAMP, new Timestamp(oldestTimestamp))));
    }

    /**
     * Loads the current money configuration record, optionally locking on the record.
     *
     * @param lock If true, the record will be selected using SELECT ... FOR UPDATE to grab
     * a lock on the record.  If another process has already locked this record, this will return
     * null.
     * @return The money configuration, or null if we tried locking the record, but it was already
     * locked.
     */
    public MoneyConfigRecord getMoneyConfig (final boolean lock)
    {
        if (lock) {
            // Update the record, setting locked = true, but only if locked = false currently.
            int count = updatePartial(MoneyConfigRecord.class,
                new Where(MoneyConfigRecord.LOCKED, false),
                MoneyConfigRecord.getKey(MoneyConfigRecord.RECORD_ID),
                MoneyConfigRecord.LOCKED, true);
            if (count == 0) {
                // Record is already locked, bail out.
                return null;
            }
        }

        MoneyConfigRecord confRecord = load(MoneyConfigRecord.class);
        if (confRecord == null) {
            // Create a new money config record with the current date for the last time bling
            // was distributed and save it.  If something else slips in and adds it, this will
            // throw an exception -- just attempt to retrieve it at that point.
            confRecord = new MoneyConfigRecord();
            try {
                store(confRecord);
            } catch (DuplicateKeyException dke) {
                confRecord = load(MoneyConfigRecord.class);
            }
        }
        return confRecord;
    }

    /**
     * Sets the last time bling was distributed to the given date.  This will also unlock the
     * record; this should only be called if we have the lock on the record and wish to release it.
     *
     * @param lastDistributedBling Date the bling was last distributed.
     */
    public void completeBlingDistribution (Date lastDistributedBling)
    {
        updatePartial(MoneyConfigRecord.getKey(MoneyConfigRecord.RECORD_ID),
            MoneyConfigRecord.LOCKED, false,
            MoneyConfigRecord.LAST_DISTRIBUTED_BLING, lastDistributedBling);
    }

    /**
     * Commits a bling cash out request.  This will only update the CashOutRecord -- bling deduction
     * must be handled separately.
     *
     * @param memberId ID of the member to commit.
     * @param actualAmount Actual amount of centibling that was cashed out.
     * @return Number of records updated, either 0 or 1.  Can be zero if the member is not currently
     * cashing out any bling.
     */
    public int commitBlingCashOutRequest (int memberId, int actualAmount)
    {
        Where where = new Where(new And(
            new Conditionals.Equals(BlingCashOutRecord.MEMBER_ID, memberId),
            new Conditionals.IsNull(BlingCashOutRecord.TIME_FINISHED)));
        return updatePartial(BlingCashOutRecord.class, where,
            new ActiveCashOutInvalidator(memberId),
            BlingCashOutRecord.TIME_FINISHED, new Timestamp(System.currentTimeMillis()),
            BlingCashOutRecord.ACTUAL_CASHED_OUT, actualAmount,
            BlingCashOutRecord.SUCCESSFUL, false);
    }

    /**
     * Cancels a request to cash out bling.
     *
     * @param memberId ID of the member whose bling has been cashed out.
     * @param reason The reason the cash out failed.
     * @return The number of records, either 0 or 1.  If 0, there are no active cash outs for the
     * user.
     */
    public int cancelBlingCashOutRequest (int memberId, String reason)
    {
        Where where = new Where(new And(
            new Conditionals.Equals(BlingCashOutRecord.MEMBER_ID, memberId),
            new Conditionals.IsNull(BlingCashOutRecord.TIME_FINISHED)));
        return updatePartial(BlingCashOutRecord.class, where,
            new ActiveCashOutInvalidator(memberId),
            BlingCashOutRecord.TIME_FINISHED, new Timestamp(System.currentTimeMillis()),
            BlingCashOutRecord.CANCEL_REASON, reason,
            BlingCashOutRecord.SUCCESSFUL, false);
    }

    /**
     * Creates a new CashOutRecord that indicates the specified member requested a cash out.
     *
     * @param memberId id of the member cashing out.
     * @param blingAmount amount of centibling, to cash out.
     * @param blingWorth worth of each bling at the time the request was made.
     * @param info billing information indicating how the member should be paid.
     *
     * @return the newly created cash out record.
     */
    public BlingCashOutRecord createCashOut (int memberId, int blingAmount, int blingWorth,
        CashOutBillingInfo info)
    {
        BlingCashOutRecord cashOut = new BlingCashOutRecord(
            memberId, blingAmount, blingWorth, info);
        insert(cashOut);
        return cashOut;
    }

    /**
     * Retrieves the current cash out request for the specified user.
     *
     * @param memberId ID of the member to retrieve a cash out record for.
     * @return The current cash out record, or null if the user is not currently cashing out.
     */
    public BlingCashOutRecord getCurrentCashOutRequest (int memberId)
    {
        return load(BlingCashOutRecord.class, new Where(new And(
            new Conditionals.IsNull(BlingCashOutRecord.TIME_FINISHED),
            new Conditionals.Equals(BlingCashOutRecord.MEMBER_ID, memberId))));
    }

    /**
     * Retrieves all cash out records for members that are currently cashing out some amount
     * of their bling.
     */
    public List<BlingCashOutRecord> getAccountsCashingOut ()
    {
        // select * from CashOutRecord where timeCompleted is null
        return findAll(BlingCashOutRecord.class, new Where(
            new Conditionals.IsNull(BlingCashOutRecord.TIME_FINISHED)));
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(MemberAccountRecord.class,
                  new Where(new Conditionals.In(MemberAccountRecord.MEMBER_ID, memberIds)));
        deleteAll(MoneyTransactionRecord.class,
                  new Where(new Conditionals.In(MoneyTransactionRecord.MEMBER_ID, memberIds)));
    }

    public int countBroadcastsSince (long time)
    {
        // round down to the nearest minute for better cachability
        time -= time % (60 * 1000);
        Timestamp limit = new Timestamp(time);
        return load(CountRecord.class,
            new Where(new Conditionals.GreaterThan(BroadcastHistoryRecord.TIME_SENT, limit)),
            new FromOverride(BroadcastHistoryRecord.class)).count;
    }

    /**
     * Create the singleton BarPoolRecord in the database.
     */
    protected BarPoolRecord createBarPoolRecord (int defaultBarPoolSize)
    {
        BarPoolRecord bpRec = new BarPoolRecord();
        bpRec.id = BarPoolRecord.RECORD_ID;
        bpRec.barPool = defaultBarPoolSize;
        try {
            insert(bpRec);
            // log a warning, hopefully we ever only do this once.
            log.warning("Populated initial exchange bar pool");
        } catch (Exception e) {
            // hmm, beaten to the punch?
            bpRec = load(BarPoolRecord.class, BarPoolRecord.KEY);
            if (bpRec == null) {
                throw new DatabaseException("What in the whirled? Can't populate BarPoolRecord.");
            }
        }
        return bpRec;
    }

    /** Helper method to setup a query for a transaction history search. */
    protected void populateSearch (
        List<QueryClause> clauses, int memberId,
        EnumSet<TransactionType> transactionTypes, Currency currency)
    {
        List<SQLOperator> where = Lists.newArrayList();

        where.add(new Conditionals.Equals(MoneyTransactionRecord.MEMBER_ID, memberId));
        if (transactionTypes != null) {
            where.add(
                new Conditionals.In(MoneyTransactionRecord.TRANSACTION_TYPE, transactionTypes));
        }
        if (currency != null) {
            where.add(new Conditionals.Equals(MoneyTransactionRecord.CURRENCY, currency));
        }

        clauses.add(new Where(new And(where)));
    }

    protected Where makeSubjectSearch (Object subject)
    {
        MoneyTransactionRecord.Subject subj = new MoneyTransactionRecord.Subject(subject);
        List<SQLOperator> where = Lists.newArrayList();
        where.add(new Conditionals.Equals(MoneyTransactionRecord.SUBJECT_TYPE, subj.type));
        where.add(new Conditionals.Equals(MoneyTransactionRecord.SUBJECT_ID_TYPE, subj.idType));
        where.add(new Conditionals.Equals(MoneyTransactionRecord.SUBJECT_ID, subj.id));
        return new Where(new And(where));
    }

    @Override
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(MemberAccountRecord.class);
        classes.add(MoneyTransactionRecord.class);
        classes.add(MoneyConfigRecord.class);
        classes.add(BlingCashOutRecord.class);
        classes.add(BarPoolRecord.class);
        classes.add(ExchangeRecord.class);
        classes.add(BroadcastHistoryRecord.class);
    }

    /** Cache invalidator that invalidates a member's current cash out record. */
    protected static class ActiveCashOutInvalidator extends TraverseWithFilter<BlingCashOutRecord>
    {
        public ActiveCashOutInvalidator (int memberId)
        {
            super(BlingCashOutRecord.class);
            _memberId = memberId;
        }

        protected boolean testForEviction (Serializable key, BlingCashOutRecord record)
        {
            return record.memberId == _memberId && record.timeFinished == null;
        }

        protected final int _memberId;
    }
}
