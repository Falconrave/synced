//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity(indices={
    @Index(name="ixLastSession", fields={ MemberRecord.LAST_SESSION }),
    @Index(name="ixName", fields={ MemberRecord.NAME }),
    @Index(name="ixInvitingFriend", fields={ MemberRecord.INVITING_FRIEND_ID })
    // Note: PERMA_NAME and ACCOUNT_NAME are automatically indexed by their uniqueness constraint
},
fullTextIndexes={
    @FullTextIndex(name=MemberRecord.FTS_NAME, fieldNames={ MemberRecord.NAME })
})
public class MemberRecord extends PersistentRecord
{
    /** Flags used in the {@link #flags} field. */
    public static enum Flag
    {
        /** A flag denoting this user as having support privileges. */
        SUPPORT(1 << 0),

        /** A flag denoting this user as having admin privileges. */
        ADMIN(1 << 1),

        /** A flag denoting this user has having elected to see mature content. */
        SHOW_MATURE(1 << 2),

        /** A flag denoting this user does not want to receive real email for Whirled mail. */
        NO_WHIRLED_MAIL_TO_EMAIL(1 << 3),

        /** A flag denoting this user does not want to receive announcement mail. */
        NO_ANNOUNCE_EMAIL(1 << 4);

        public int getBit () {
            return _bit;
        }

        Flag (int bit) {
            _bit = bit;
        }

        protected int _bit;
    }

    /** Experiences used in the {@link #experiences} field. */
    public static enum Experience
    {
        /** Indicates whether this user has played a game. */
        PLAYED_GAME(1 << 0),

        /** Indicates whether this user has entered a Whirled. */
        EXPLORED_WHIRLED(1 << 1),

        /** Indicates whether this user has decorated their home. */
        DECORATED_HOME(1 << 2),

        /** A place holder experience that is not used and reminds us that we can't have more than
         * 32 experiences. */
        NOT_USED(1 << 32);

        public int getBit () {
            return _bit;
        }

        Experience (int bit) {
            _bit = bit;
        }

        protected int _bit;
    }

    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(MemberRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #accountName} field. */
    public static final String ACCOUNT_NAME = "accountName";

    /** The qualified column identifier for the {@link #accountName} field. */
    public static final ColumnExp ACCOUNT_NAME_C =
        new ColumnExp(MemberRecord.class, ACCOUNT_NAME);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(MemberRecord.class, NAME);

    /** The column identifier for the {@link #permaName} field. */
    public static final String PERMA_NAME = "permaName";

    /** The qualified column identifier for the {@link #permaName} field. */
    public static final ColumnExp PERMA_NAME_C =
        new ColumnExp(MemberRecord.class, PERMA_NAME);

    /** The column identifier for the {@link #flow} field. */
    public static final String FLOW = "flow";

    /** The qualified column identifier for the {@link #flow} field. */
    public static final ColumnExp FLOW_C =
        new ColumnExp(MemberRecord.class, FLOW);

    /** The column identifier for the {@link #accFlow} field. */
    public static final String ACC_FLOW = "accFlow";

    /** The qualified column identifier for the {@link #accFlow} field. */
    public static final ColumnExp ACC_FLOW_C =
        new ColumnExp(MemberRecord.class, ACC_FLOW);

    /** The column identifier for the {@link #homeSceneId} field. */
    public static final String HOME_SCENE_ID = "homeSceneId";

    /** The qualified column identifier for the {@link #homeSceneId} field. */
    public static final ColumnExp HOME_SCENE_ID_C =
        new ColumnExp(MemberRecord.class, HOME_SCENE_ID);

    /** The column identifier for the {@link #avatarId} field. */
    public static final String AVATAR_ID = "avatarId";

    /** The qualified column identifier for the {@link #avatarId} field. */
    public static final ColumnExp AVATAR_ID_C =
        new ColumnExp(MemberRecord.class, AVATAR_ID);

    /** The column identifier for the {@link #created} field. */
    public static final String CREATED = "created";

    /** The qualified column identifier for the {@link #created} field. */
    public static final ColumnExp CREATED_C =
        new ColumnExp(MemberRecord.class, CREATED);

    /** The column identifier for the {@link #sessions} field. */
    public static final String SESSIONS = "sessions";

    /** The qualified column identifier for the {@link #sessions} field. */
    public static final ColumnExp SESSIONS_C =
        new ColumnExp(MemberRecord.class, SESSIONS);

    /** The column identifier for the {@link #sessionMinutes} field. */
    public static final String SESSION_MINUTES = "sessionMinutes";

    /** The qualified column identifier for the {@link #sessionMinutes} field. */
    public static final ColumnExp SESSION_MINUTES_C =
        new ColumnExp(MemberRecord.class, SESSION_MINUTES);

    /** The column identifier for the {@link #lastSession} field. */
    public static final String LAST_SESSION = "lastSession";

    /** The qualified column identifier for the {@link #lastSession} field. */
    public static final ColumnExp LAST_SESSION_C =
        new ColumnExp(MemberRecord.class, LAST_SESSION);

    /** The column identifier for the {@link #humanity} field. */
    public static final String HUMANITY = "humanity";

    /** The qualified column identifier for the {@link #humanity} field. */
    public static final ColumnExp HUMANITY_C =
        new ColumnExp(MemberRecord.class, HUMANITY);

    /** The column identifier for the {@link #lastHumanityAssessment} field. */
    public static final String LAST_HUMANITY_ASSESSMENT = "lastHumanityAssessment";

    /** The qualified column identifier for the {@link #lastHumanityAssessment} field. */
    public static final ColumnExp LAST_HUMANITY_ASSESSMENT_C =
        new ColumnExp(MemberRecord.class, LAST_HUMANITY_ASSESSMENT);

    /** The column identifier for the {@link #experiences} field. */
    public static final String EXPERIENCES = "experiences";

    /** The qualified column identifier for the {@link #experiences} field. */
    public static final ColumnExp EXPERIENCES_C =
        new ColumnExp(MemberRecord.class, EXPERIENCES);

    /** The column identifier for the {@link #flags} field. */
    public static final String FLAGS = "flags";

    /** The qualified column identifier for the {@link #flags} field. */
    public static final ColumnExp FLAGS_C =
        new ColumnExp(MemberRecord.class, FLAGS);

    /** The column identifier for the {@link #invitingFriendId} field. */
    public static final String INVITING_FRIEND_ID = "invitingFriendId";

    /** The qualified column identifier for the {@link #invitingFriendId} field. */
    public static final ColumnExp INVITING_FRIEND_ID_C =
        new ColumnExp(MemberRecord.class, INVITING_FRIEND_ID);

    /** The column identifier for the {@link #level} field. */
    public static final String LEVEL = "level";

    /** The qualified column identifier for the {@link #level} field. */
    public static final ColumnExp LEVEL_C =
        new ColumnExp(MemberRecord.class, LEVEL);
    // AUTO-GENERATED: FIELDS END

    /** The identifer for the full text index on the display name. */
    public static final String FTS_NAME = "ftixName";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 15;

    /** This member's unique id. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int memberId;

    /** The authentication account associated with this member. */
    @Column(unique=true)
    public String accountName;

    /** This member's display name. Is shown in the Whirled, can be changed any time. */
    public String name;

    /** This member's permanent name. Must be URL-safe; used to logon to wiki and forums. */
    @Column(nullable=true, unique=true)
    public String permaName;

    /** The quantity of flow possessed by this member. */
    public int flow;

    /** The total amount of flow ever accumulated by this member. */
    public int accFlow;

    /** The home scene for this member. */
    public int homeSceneId;

    /** The avatar of this user, or 0. */
    public int avatarId;

    /** The date on which this member record was created. */
    public Date created;

    /** The number of sessions this player has played. */
    public int sessions;

    /** The cumulative number of minutes spent playing. */
    public int sessionMinutes;

    /** The time at which the player ended their last session. */
    public Timestamp lastSession;

    /** This member's current humanity rating, between 0 and {@link MemberObject#MAX_HUMANITY}. */
    public int humanity;

    /** The time at which we last assessed this member's humanity. */
    public Timestamp lastHumanityAssessment;

    /** Bits tracking whether the user has had any of a set of "one time" experiences. */
    public int experiences;

    /** Various one bit data. */
    public int flags;

    /** The memberId of the person who invited this person. */
    @Column(defaultValue="0")
    public int invitingFriendId;

    /** The currently reported level of this user. */
    @Column(defaultValue="1")
    public int level = 1;

    /** A blank constructor used when loading records from the database. */
    public MemberRecord ()
    {
    }

    /** Constructs a blank member record for the supplied account. */
    public MemberRecord (String accountName)
    {
        this.accountName = accountName;
    }

    /**
     * Creates web credentials for this member record.
     */
    public WebCreds toCreds (String authtok)
    {
        WebCreds creds = new WebCreds();
        creds.token = authtok;
        creds.accountName = accountName;
        creds.name = getName();
        creds.permaName = permaName;
        creds.isSupport = isSupport();
        creds.isAdmin = isAdmin();
        return creds;
    }

    /**
     * Returns true if this member has support or higher privileges.
     */
    public boolean isSupport ()
    {
        return isSet(Flag.SUPPORT) || isSet(Flag.ADMIN);
    }

    /**
     * Returns true if this member has admin or higher privileges.
     */
    public boolean isAdmin ()
    {
        return isSet(Flag.ADMIN);
    }

    /**
     * Tests whether a given flag is set on this member.
     */
    public boolean isSet (Flag flag)
    {
        return (flags & flag.getBit()) != 0;
    }

    /**
     * Sets a given flag to on or off.
     */
    public void setFlag (Flag flag, boolean value)
    {
        flags = (value ? (flags | flag.getBit()) : (flags & ~flag.getBit()));
    }

    /**
     * Returns true if this member has had the specified experience.
     */
    public boolean isSet (Experience experience)
    {
        return (experiences & experience.getBit()) != 0;
    }

    /**
     * Sets a given experience to on or off.
     */
    public void setExperience (Experience exp, boolean value)
    {
        experiences = (value ? (experiences | exp.getBit()) : (experiences & ~exp.getBit()));
    }

    /** Returns this member's name as a proper {@link Name} instance. */
    public MemberName getName ()
    {
        return new MemberName(name, memberId);
    }

    /**
     * Returns a brief string containing our account name, member id and display name.
     */
    public String who ()
    {
        return accountName + " (" + memberId + ", " + name + ")";
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #MemberRecord}
     * with the supplied key values.
     */
    public static Key<MemberRecord> getKey (int memberId)
    {
        return new Key<MemberRecord>(
                MemberRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
