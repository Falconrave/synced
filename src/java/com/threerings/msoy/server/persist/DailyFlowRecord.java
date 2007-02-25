//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Summarizes the amount of each type of flow spent on any given day.
 */
@Entity
public class DailyFlowRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(DailyFlowRecord.class, TYPE);

    /** The column identifier for the {@link #date} field. */
    public static final String DATE = "date";

    /** The qualified column identifier for the {@link #date} field. */
    public static final ColumnExp DATE_C =
        new ColumnExp(DailyFlowRecord.class, DATE);

    /** The column identifier for the {@link #amount} field. */
    public static final String AMOUNT = "amount";

    /** The qualified column identifier for the {@link #amount} field. */
    public static final ColumnExp AMOUNT_C =
        new ColumnExp(DailyFlowRecord.class, AMOUNT);
    // AUTO-GENERATED: FIELDS END

    /** The type of grant or expenditure  summarized by this entry. */
    @Id
    public String type;

    /** The date for which this entry is a summary. */
    @Id
    public Date date;

    /** The total amount of flow spent or granted. */
    public int amount;
}
