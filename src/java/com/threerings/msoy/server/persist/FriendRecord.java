//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Represents a friendship between two members.
 */
@Entity
@Table(uniqueConstraints =
       {@UniqueConstraint(columnNames={FriendRecord.INVITER_ID, FriendRecord.INVITEE_ID })})
public class FriendRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #inviterId} field. */
    public static final String INVITER_ID = "inviterId";

    /** The qualified column identifier for the {@link #inviterId} field. */
    public static final ColumnExp INVITER_ID_C =
        new ColumnExp(FriendRecord.class, INVITER_ID);

    /** The column identifier for the {@link #inviteeId} field. */
    public static final String INVITEE_ID = "inviteeId";

    /** The qualified column identifier for the {@link #inviteeId} field. */
    public static final ColumnExp INVITEE_ID_C =
        new ColumnExp(FriendRecord.class, INVITEE_ID);

    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** The member id of the inviter. */
    @Id
    public int inviterId;

    /** The member id of the invitee. */
    @Id
    public int inviteeId;
}
