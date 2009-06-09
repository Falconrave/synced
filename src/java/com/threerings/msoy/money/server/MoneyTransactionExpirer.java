//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.samskivert.util.Lifecycle;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.server.persist.MoneyTransactionRecord;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.server.persist.BatchInvoker;

import static com.threerings.msoy.Log.log;

/**
 * Manages expiration of {@link MoneyTransactionRecord}s.  Coin records should
 * be removed from the database if they are more than 10 days old (by default).
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class MoneyTransactionExpirer
    implements Lifecycle.Component
{
    /**
     * Starts the expirer.  By default, it will use a single-threaded scheduled executor,
     * and check once every hour for coins history records that are at least 10 days old.
     */
    @Inject public MoneyTransactionExpirer (Lifecycle cycle)
    {
        cycle.addComponent(this);

        // note: this Interval doesn't post to the omgr: it doesn't need to.
        _interval = new Interval() {
            @Override public void expired () {
                _batchInvoker.postUnit(new Invoker.Unit("MoneyTransactionExpirer") {
                    @Override public boolean invoke () {
                        doPurge();
                        return false;
                    }
                    @Override public long getLongThreshold () {
                        return 10 * 1000;
                    }
                });
            }
        };
    }

    // from interface Lifecycle.Component
    public void init ()
    {
        _interval.schedule(PURGE_INTERVAL, true);
    }

    // from interface Lifecycle.Component
    public void shutdown ()
    {
        _interval.cancel();
    }

    /**
     * Actually do the purging.
     */
    protected void doPurge ()
    {
        int coins = _repo.deleteOldTransactions(Currency.COINS, COIN_MAX_AGE);
        int bars = _repo.deleteOldTransactions(Currency.BARS, BAR_MAX_AGE);
        int bling = _repo.deleteOldTransactions(Currency.BLING, BAR_MAX_AGE);
        int exchange = _repo.deleteOldExchangeRecords(EXCHANGE_MAX_AGE);
        if (coins > 0 || bars > 0 || bling > 0 || exchange > 0) {
            log.info("Removed old money transaction records.",
                 "coins", coins, "bars", bars, "bling", bling, "exchange", exchange);
        }
    }

    protected final Interval _interval;

    // dependencies
    @Inject protected MoneyRepository _repo;
    @Inject protected @BatchInvoker Invoker _batchInvoker;

    protected static final long DAY = 24L * 60 * 60 * 1000L; // the length of 99.4% of days
    protected static final long COIN_MAX_AGE = 10L * DAY; // 10 days
    protected static final long BAR_MAX_AGE = 365L * DAY; // approx 1 year
    protected static final long EXCHANGE_MAX_AGE = 30L * DAY; // 30 days

    protected static final long PURGE_INTERVAL = 60 * 60 * 1000; // 1 hour
}
