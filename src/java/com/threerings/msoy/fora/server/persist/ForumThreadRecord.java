//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.FullTextIndex;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.fora.data.ForumThread;

/**
 * Contains information on a forum thread.
 */
@Entity(indices={
    @Index(name="ixGroupId", fields={ ForumThreadRecord.GROUP_ID }),
    @Index(name="ixMostRecentPostId", fields={ ForumThreadRecord.MOST_RECENT_POST_ID }),
    @Index(name="ixSticky", fields={ ForumThreadRecord.STICKY })
}, fullTextIndices={
    @FullTextIndex(name=ForumThreadRecord.FTS_SUBJECT, fields={ ForumThreadRecord.SUBJECT })
})
public class ForumThreadRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #threadId} field. */
    public static final String THREAD_ID = "threadId";

    /** The qualified column identifier for the {@link #threadId} field. */
    public static final ColumnExp THREAD_ID_C =
        new ColumnExp(ForumThreadRecord.class, THREAD_ID);

    /** The column identifier for the {@link #groupId} field. */
    public static final String GROUP_ID = "groupId";

    /** The qualified column identifier for the {@link #groupId} field. */
    public static final ColumnExp GROUP_ID_C =
        new ColumnExp(ForumThreadRecord.class, GROUP_ID);

    /** The column identifier for the {@link #flags} field. */
    public static final String FLAGS = "flags";

    /** The qualified column identifier for the {@link #flags} field. */
    public static final ColumnExp FLAGS_C =
        new ColumnExp(ForumThreadRecord.class, FLAGS);

    /** The column identifier for the {@link #subject} field. */
    public static final String SUBJECT = "subject";

    /** The qualified column identifier for the {@link #subject} field. */
    public static final ColumnExp SUBJECT_C =
        new ColumnExp(ForumThreadRecord.class, SUBJECT);

    /** The column identifier for the {@link #mostRecentPostId} field. */
    public static final String MOST_RECENT_POST_ID = "mostRecentPostId";

    /** The qualified column identifier for the {@link #mostRecentPostId} field. */
    public static final ColumnExp MOST_RECENT_POST_ID_C =
        new ColumnExp(ForumThreadRecord.class, MOST_RECENT_POST_ID);

    /** The column identifier for the {@link #mostRecentPostTime} field. */
    public static final String MOST_RECENT_POST_TIME = "mostRecentPostTime";

    /** The qualified column identifier for the {@link #mostRecentPostTime} field. */
    public static final ColumnExp MOST_RECENT_POST_TIME_C =
        new ColumnExp(ForumThreadRecord.class, MOST_RECENT_POST_TIME);

    /** The column identifier for the {@link #mostRecentPosterId} field. */
    public static final String MOST_RECENT_POSTER_ID = "mostRecentPosterId";

    /** The qualified column identifier for the {@link #mostRecentPosterId} field. */
    public static final ColumnExp MOST_RECENT_POSTER_ID_C =
        new ColumnExp(ForumThreadRecord.class, MOST_RECENT_POSTER_ID);

    /** The column identifier for the {@link #posts} field. */
    public static final String POSTS = "posts";

    /** The qualified column identifier for the {@link #posts} field. */
    public static final ColumnExp POSTS_C =
        new ColumnExp(ForumThreadRecord.class, POSTS);

    /** The column identifier for the {@link #sticky} field. */
    public static final String STICKY = "sticky";

    /** The qualified column identifier for the {@link #sticky} field. */
    public static final ColumnExp STICKY_C =
        new ColumnExp(ForumThreadRecord.class, STICKY);
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on {@link #subject} */
    public static final String FTS_SUBJECT = "SUBJECT";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 6;

    /** A unique identifier for this forum thread. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int threadId;

    /** The id of the group to which this forum thread belongs. */
    public int groupId;

    /** Flags indicating attributes of this thread: {@link ForumThread#FLAG_ANNOUNCEMENT}, etc. */
    public int flags;

    /** The subject of this thread. */
    @Column(length=ForumThread.MAX_SUBJECT_LENGTH)
    public String subject;

    /** The id of the most recent message posted to this thread. */
    public int mostRecentPostId;

    /** The time at which the most recent message was posted to this thread. */
    public Timestamp mostRecentPostTime;

    /** The member id of the author of the message most recently posted to this thread. */
    public int mostRecentPosterId;

    /** The number of posts in this thread. */
    public int posts;

    /** Whether or not this thread is sticky. Used for sorting. */
    public boolean sticky;

    /**
     * Converts this persistent record to a runtime record.
     *
     * @param members a mapping from memberId to {@link MemberName} that should contain a mapping
     * for {@link #mostRecentPosterId}.
     */
    public ForumThread toForumThread (Map<Integer,MemberName> members, Map<Integer,GroupName> groups)
    {
        ForumThread record = new ForumThread();
        record.threadId = threadId;
        record.group = groups.get(groupId);
        record.flags = flags;
        record.subject = subject;
        record.mostRecentPostId = mostRecentPostId;
        record.mostRecentPostTime = new Date(mostRecentPostTime.getTime());
        record.mostRecentPoster = members.get(mostRecentPosterId);
        record.posts = posts;
        // sticky is only used for database sorting
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ForumThreadRecord}
     * with the supplied key values.
     */
    public static Key<ForumThreadRecord> getKey (int threadId)
    {
        return new Key<ForumThreadRecord>(
                ForumThreadRecord.class,
                new String[] { THREAD_ID },
                new Comparable[] { threadId });
    }
    // AUTO-GENERATED: METHODS END
}
