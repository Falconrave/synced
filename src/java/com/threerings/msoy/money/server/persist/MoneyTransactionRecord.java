//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.annotation.Transient;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.CatalogIdent;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.TransactionType;

/**
 * Domain object representing an entry in a member's account history. The account history keeps
 * track of all changes to money in a member's account over time.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
@Entity(indices=@Index(name="ixSubject"))
public abstract class MoneyTransactionRecord extends PersistentRecord
{
    /** Stores the data for the subject of a transaction. */
    public static class Subject
    {
        /** The type of the subject. */
        public byte type;

        /** The type of id held. */
        public MsoyItemType idType;

        /** The subject's actual id. */
        public int id;

        /** Creates a new subject. An exception is thrown if the subject does not fit one of our
         * subject categories. */
        public Subject (Object subject) {
            type = SUBJECT_NONE;

            if (subject != null) {
                if (subject instanceof CatalogIdent) {
                    type = SUBJECT_CATALOG_IDENT;
                    CatalogIdent ident = (CatalogIdent) subject;
                    idType = ident.type;
                    id = ident.catalogId;

                } else if (subject instanceof ItemIdent) {
                    type = SUBJECT_ITEM_IDENT;
                    ItemIdent ident = (ItemIdent) subject;
                    idType = ident.type;
                    id = ident.itemId;

                } else {
                    throw new RuntimeException("Unknown subject: " + subject);
                }
            }
        }
    }

    // AUTO-GENERATED: FIELDS START
    public static final Class<MoneyTransactionRecord> _R = MoneyTransactionRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp TIMESTAMP = colexp(_R, "timestamp");
    public static final ColumnExp TRANSACTION_TYPE = colexp(_R, "transactionType");
    public static final ColumnExp AMOUNT = colexp(_R, "amount");
    public static final ColumnExp BALANCE = colexp(_R, "balance");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp SUBJECT_TYPE = colexp(_R, "subjectType");
    public static final ColumnExp SUBJECT_ID_TYPE = colexp(_R, "subjectIdType");
    public static final ColumnExp SUBJECT_ID = colexp(_R, "subjectId");
    public static final ColumnExp REFERENCE_TX_ID = colexp(_R, "referenceTxId");
    public static final ColumnExp REFERENCE_MEMBER_ID = colexp(_R, "referenceMemberId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this if you change this object's schema. */
    public static final int SCHEMA_VERSION = 1;

    /** Value of {@link #subjectType} when there is no subject. */
    public static final int SUBJECT_NONE = 0;

    /** Value of {@link #subjectType} when the transaction was regarding a catalog item. */
    public static final int SUBJECT_CATALOG_IDENT = 1;

    /** Value of {@link #subjectType} when the transaction was regarding an item. */
    public static final int SUBJECT_ITEM_IDENT = 2;

    /** Transforms a MoneyTransactionRecord into a MoneyTransaction. */
    public static Function<MoneyTransactionRecord, MoneyTransaction> TO_TRANSACTION =
        new Function<MoneyTransactionRecord, MoneyTransaction>() {
        public MoneyTransaction apply (MoneyTransactionRecord record) {
            return record.toMoneyTransaction();
        }
    };

    /** Transforms a MoneyTransactionRecord into a MoneyTransaction, for support personel. */
    public static Function<MoneyTransactionRecord, MoneyTransaction> TO_TRANSACTION_SUPPORT =
        new Function<MoneyTransactionRecord, MoneyTransaction>() {
        public MoneyTransaction apply (MoneyTransactionRecord record) {
            return record.toMoneyTransaction(true);
        }
    };

    /** ID of this record. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    /** ID of the member this record is for. */
    @Index(name="ixMemberId")
    public int memberId;

    /** Time this transaction was performed. */
    @Index(name="ixTimestamp")
    public Timestamp timestamp;

    /** Type of transaction this history record was for. */
    @Column(defaultValue = "0") @Index(name="ixTransactionType")
    public TransactionType transactionType;

    /** Amount debited/credited. */
    public int amount;

    /** The member's balance of this currency after this transaction. */
    public int balance;

    /** A translatable description of the transaction. */
    public String description;

    /** A code indicating the type of the subject of this transaction. */
    public byte subjectType;

    /** An optional divider of the subjectId. */
    public MsoyItemType subjectIdType = MsoyItemType.NOT_A_TYPE;

    /** The id of the subject of this transaction. */
    public int subjectId;

    /** For certain types of transactions, the reference transaction this was in response to. */
    public int referenceTxId;

    /** For some transactions, there may be a memberId of the other member. */
    public int referenceMemberId;

    /** Set to true if this transaction affected the accumulated total for the type of money. Note
     * that this field will only be set if the record was obtained during a transaction. */
    @Transient public boolean accAffected;

    /** The accumulated balance of the type of money affected. Note that this field will only be
     * set if the record was obtained during a transaction. */
    @Transient public long accBalance;

    /**
     * Concrete subclasses identify which currency they represent through this method.
     */
    public abstract Currency getCurrency ();

    /**
     * Create a new MoneyTransactionRecord.
     */
    public MoneyTransactionRecord (
        int memberId, int amount, int balance, boolean accAffected, long accBalance)
    {
        this.memberId = memberId;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.amount = amount;
        this.balance = balance;
        this.accAffected = accAffected;
        this.accBalance = accBalance;
    }

    /**
     * A convenience method to fill in the specified fields.
     */
    public void fill (TransactionType transType, String description, Object subject)
    {
        this.transactionType = transType;
        this.description = description;

        Subject subj = new Subject(subject);
        this.subjectType = subj.type;
        this.subjectIdType = (subj.idType != null) ? subj.idType : MsoyItemType.NOT_A_TYPE;
        this.subjectId = subj.id;
    }

    /** Suitable for unserialization. */
    public MoneyTransactionRecord ()
    {
    }

    public MoneyTransaction toMoneyTransaction ()
    {
        return new MoneyTransaction(
            memberId, timestamp, transactionType, getCurrency(), amount, balance, description);
    }

    public MoneyTransaction toMoneyTransaction (boolean forSupport)
    {
        MoneyTransaction mtx = toMoneyTransaction();
        if (forSupport) {
            mtx.referenceTxId = referenceTxId;
            mtx.referenceMemberName = MemberName.makeKey(referenceMemberId);
        }
        return mtx;
    }

}
