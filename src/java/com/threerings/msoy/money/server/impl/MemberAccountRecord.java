//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.sql.Timestamp;
import java.util.Date;

import net.jcip.annotations.NotThreadSafe;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.msoy.money.server.MemberMoney;
import com.threerings.msoy.money.server.MoneyType;

/**
 * Domain model for the current status of a member's account, including the amount of
 * each money type currently in their account.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity
@NotThreadSafe
public class MemberAccountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberAccountRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #coins} field. */
    public static final String COINS = "coins";

    /** The qualified column identifier for the {@link #coins} field. */
    public static final ColumnExp COINS_C =
        new ColumnExp(MemberAccountRecord.class, COINS);

    /** The column identifier for the {@link #bars} field. */
    public static final String BARS = "bars";

    /** The qualified column identifier for the {@link #bars} field. */
    public static final ColumnExp BARS_C =
        new ColumnExp(MemberAccountRecord.class, BARS);

    /** The column identifier for the {@link #bling} field. */
    public static final String BLING = "bling";

    /** The qualified column identifier for the {@link #bling} field. */
    public static final ColumnExp BLING_C =
        new ColumnExp(MemberAccountRecord.class, BLING);

    /** The column identifier for the {@link #dateLastUpdated} field. */
    public static final String DATE_LAST_UPDATED = "dateLastUpdated";

    /** The qualified column identifier for the {@link #dateLastUpdated} field. */
    public static final ColumnExp DATE_LAST_UPDATED_C =
        new ColumnExp(MemberAccountRecord.class, DATE_LAST_UPDATED);

    /** The column identifier for the {@link #versionId} field. */
    public static final String VERSION_ID = "versionId";

    /** The qualified column identifier for the {@link #versionId} field. */
    public static final ColumnExp VERSION_ID_C =
        new ColumnExp(MemberAccountRecord.class, VERSION_ID);
    // AUTO-GENERATED: FIELDS END
    
    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MemberAccountRecord}
     * with the supplied key values.
     */
    public static Key<MemberAccountRecord> getKey (final int memberId)
    {
        return new Key<MemberAccountRecord>(
                MemberAccountRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
    
    public static final int SCHEMA_VERSION = 1;
    
    /**
     * Creates a new blank record for the given member.  All account balances are set to 0.
     * 
     * @param memberId ID of the member to create the record for.
     */
    public MemberAccountRecord (final int memberId)
    {
        this.memberId = memberId;
        this.coins = 0;
        this.bars = 0;
        this.bling = 0.0;
        this.dateLastUpdated = new Timestamp(new Date().getTime());
        this.versionId = 1;
    }
    
    /** For depot's eyes only.  Not part of the API. */
    public MemberAccountRecord ()
    {
    }
    
    /**
     * Adds the given number of bars to the member's account.
     * 
     * @param bars Number of bars to add.
     * @return Account history record for this transaction.
     */
    public MemberAccountHistoryRecord buyBars (final int bars)
    {
        this.bars += bars;
        this.dateLastUpdated = new Timestamp(new Date().getTime());
        return new MemberAccountHistoryRecord(this.memberId, this.dateLastUpdated, MoneyType.BARS,
            bars, false, "Purchased " + bars + " bars.");
    }
    
    public int getMemberId ()
    {
        return memberId;
    }

    public int getCoins ()
    {
        return coins;
    }

    public int getBars ()
    {
        return bars;
    }

    public double getBling ()
    {
        return bling;
    }

    public Date getDateLastUpdated ()
    {
        return dateLastUpdated;
    }

    public long getVersionId ()
    {
        return versionId;
    }
    
    public MemberMoney getMemberMoney ()
    {
        return new MemberMoney(memberId, coins, bars, bling);
    }
    
    /** ID of the member this account record is for.  Note: this is not part of the API, do not use it. */
    @Id
    public int memberId;
    
    /** Coins currently in the account.  Note: this is not part of the API, do not use it. */
    public int coins;
    
    /** Bars currently in the account.  Note: this is not part of the API, do not use it. */
    public int bars;
    
    /** Bling currently in the account.  Note: this is not part of the API, do not use it. */
    public double bling;
    
    /** Date last updated.  Note: this is not part of the API, do not use it.  Also, why does depot force 
     * this dependency on java.sql in the entity object?  :-( */
    public Timestamp dateLastUpdated;
    
    /** ID of the version of this account.  Note: this is not part of the API, do not use it. */
    public long versionId;
}
