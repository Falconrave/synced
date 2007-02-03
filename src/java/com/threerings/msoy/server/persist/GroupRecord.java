//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.io.PersistenceException;

import com.samskivert.util.StringUtil;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;

/**
 * Contains the details of a group.
 */
@Entity(indices={
    @Index(name="searchIndex", type="fulltext",
           columns={GroupRecord.NAME, GroupRecord.BLURB, GroupRecord.CHARTER })})
public class GroupRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 13;

    public static final String GROUP_ID = "groupId";
    public static final String NAME = "name";
    public static final String HOMEPAGE_URL = "homepageUrl";
    public static final String BLURB = "blurb";
    public static final String CHARTER = "charter";
    public static final String LOGO_MIME_TYPE = "logoMimeType";
    public static final String LOGO_MEDIA_HASH = "logoMediaHash";
    public static final String LOGO_MEDIA_CONSTRAINT = "logoMediaConstraint";
    public static final String BACKGROUND_CONTROL = "backgroundControl";
    public static final String INFO_BACKGROUND_MIME_TYPE = "infoBackgroundMimeType";
    public static final String INFO_BACKGROUND_HASH = "infoBackgroundHash";
    public static final String INFO_BACKGROUND_THUMB_CONSTRAINT = "infoBackgroundThumbConstraint";
    public static final String DETAIL_BACKGROUND_MIME_TYPE = "detailBackgroundMimeType";
    public static final String DETAIL_BACKGROUND_HASH = "detailBackgroundHash";
    public static final String DETAIL_BACKGROUND_THUMB_CONSTRAINT = 
        "detailBackgroundThumbConstraint";
    public static final String DETAIL_BACKGROUND_WIDTH = "detailBackgroundWidth";
    public static final String DETAIL_AREA_HEIGHT = "detailAreaHeight";
    public static final String PEOPLE_BACKGROUND_MIME_TYPE = "peopleBackgroundMimeType";
    public static final String PEOPLE_BACKGROUND_HASH = "peopleBackgroundHash";
    public static final String PEOPLE_BACKGROUND_THUMB_CONSTRAINT = 
        "peopleBackgroundThumbConstraint";
    public static final String PEOPLE_UPPER_CAP_MIME_TYPE = "peopleUpperCapMimeType";
    public static final String PEOPLE_UPPER_CAP_HASH = "peopleUpperCapHash";
    public static final String PEOPLE_UPPER_CAP_HEIGHT = "peopleUpperCapHeight";
    public static final String PEOPLE_LOWER_CAP_MIME_TYPE = "peopleLowerCapMimeType";
    public static final String PEOPLE_LOWER_CAP_HASH = "peopleLowerCapHash";
    public static final String PEOPLE_LOWER_CAP_HEIGHT = "peopleLowerCapHeight";
    public static final String CREATOR_ID = "creatorId";
    public static final String HOME_SCENE_ID = "homeSceneId";
    public static final String CREATION_DATE = "creationDate";
    public static final String POLICY = "policy";
    public static final String MEMBER_COUNT = "memberCount";

    /** The unique id of this group. */
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int groupId;

    /** The name of the group. */
    @Column(unique=true)
    public String name;

    /** The URL for the grou's homepage. */
    @Column(nullable=true)
    public String homepageUrl;

    /** The blurb for the group. */
    @Column(length=80, nullable=true)
    public String blurb;

    /** The group's charter, or null if one has yet to be set. */
    @Column(length=2048, nullable=true)
    public String charter;

    /** A hash code identifying the media for this group's logo. */
    @Column(nullable=true)
    public byte[] logoMediaHash;

    /** The MIME type of this group's logo. */
    public byte logoMimeType;

    /** The constraint for the logo image. */
    public byte logoMediaConstraint;

    /** Flag to indicate page flow control */
    public int backgroundControl;

    /** The MIME type for the background of the info area. */
    public byte infoBackgroundMimeType;

    /** A hash code identifying the media for the background of the info area. */
    @Column(nullable=true)
    public byte[] infoBackgroundHash;

    /** The constraint for the thumbnail of this image. */
    public byte infoBackgroundThumbConstraint;

    /** The MIME type for the background of the detail area. */
    public byte detailBackgroundMimeType;

    /** A hash code identifying the media for the background of the detail area. */
    @Column(nullable=true)
    public byte[] detailBackgroundHash;

    /** The constraint for the thumbnail of this image. */
    public byte detailBackgroundThumbConstraint;

    /** The width of the detail background image. */
    public int detailBackgroundWidth;

    /** The height that the detail area should be forced to in the constrained mode. */
    public int detailAreaHeight;

    /** The MIME type for the background of the people area. */
    public byte peopleBackgroundMimeType;

    /** A hash code identifying the media for the background of the people area. */
    @Column(nullable=true)
    public byte[] peopleBackgroundHash;

    /** The constraint for the thumbnail of this image. */
    public byte peopleBackgroundThumbConstraint;

    /** The mime type for the upper cap image on the people area */
    public byte peopleUpperCapMimeType;
    
    /** The upper cap for the people area */
    @Column(nullable=true)
    public byte[] peopleUpperCapHash;

    /** The height of the upper cap image */
    public int peopleUpperCapHeight;

    /** The mime type for the lower cap image on the people area */
    public byte peopleLowerCapMimeType;

    /** The lower cap for the people area */
    @Column(nullable=true)
    public byte[] peopleLowerCapHash;

    /** the height of the lower cap image */
    public int peopleLowerCapHeight;

    /** The member id of the person who created the group. */
    public int creatorId;

    /** The home scene of this group. */
    public int homeSceneId;

    /** The date and time this group was created. */
    public Timestamp creationDate;

    /** The group may be public, invite-only or exclusive as per {@link Group}. */
    public byte policy;

    /** The number of people that are currently members of this group. */
    public int memberCount;

    /**
     * Creates a web-safe version of this group.
     */
    public Group toGroupObject ()
    {
        Group group = new Group();
        group.groupId = groupId;
        group.name = name;
        group.blurb = blurb;
        group.logo = logoMediaHash == null ? Group.getDefaultGroupLogoMedia() :
            new MediaDesc(logoMediaHash.clone(), logoMimeType, logoMediaConstraint);
        group.creatorId = creatorId;
        group.creationDate = new Date(creationDate.getTime());
        group.policy = policy;
        group.memberCount = memberCount;
        return group;
    }

    /**
     * Creates a web-safe version of the extras in this group.
     */
    public GroupExtras toExtrasObject ()
    {
        GroupExtras extras = new GroupExtras();
        extras.backgroundControl = backgroundControl;
        extras.infoBackground = infoBackgroundHash == null ? null :
            new MediaDesc(infoBackgroundHash.clone(), infoBackgroundMimeType,
            infoBackgroundThumbConstraint);
        extras.detailBackground = detailBackgroundHash == null ? null :
            new MediaDesc(detailBackgroundHash.clone(), detailBackgroundMimeType,
            detailBackgroundThumbConstraint);
        extras.detailBackgroundWidth = detailBackgroundWidth;
        extras.detailAreaHeight = detailAreaHeight;
        extras.peopleBackground = peopleBackgroundHash == null ? null :
            new MediaDesc(peopleBackgroundHash.clone(), peopleBackgroundMimeType,
            peopleBackgroundThumbConstraint);
        extras.peopleUpperCap = peopleUpperCapHash == null ? null :
            new MediaDesc(peopleUpperCapHash.clone(), peopleUpperCapMimeType);
        extras.peopleUpperCapHeight = peopleUpperCapHeight;
        extras.peopleLowerCap = peopleLowerCapHash == null ? null :
            new MediaDesc(peopleLowerCapHash.clone(), peopleLowerCapMimeType);
        extras.peopleLowerCapHeight = peopleLowerCapHeight;
        extras.charter = charter;
        extras.homepageUrl = homepageUrl;
        return extras;
    }

    /**
     * Checks over the object definitions and will return a map of field, value pairs that contains
     * all of the entries that are not null, and are different from what's in this object
     * currently.  Returns null if the group is not found.
     */
    public Map<String, Object> findUpdates (Group groupDef, GroupExtras extrasDef) 
        throws PersistenceException
    {
        HashMap<String, Object> updates = new HashMap<String, Object>();
        if (groupDef.name != null && !groupDef.name.equals(name)) {
            updates.put(NAME, groupDef.name);
        }
        if (groupDef.blurb != null && !groupDef.blurb.equals(blurb)) {
            updates.put(BLURB, groupDef.blurb);
        }
        if (groupDef.logo != null && (logoMediaHash == null || 
            !groupDef.logo.equals(new MediaDesc(logoMediaHash, logoMimeType, 
            logoMediaConstraint)))) {
            updates.put(LOGO_MEDIA_HASH, groupDef.logo.hash);
            updates.put(LOGO_MIME_TYPE, groupDef.logo.mimeType);
            updates.put(LOGO_MEDIA_CONSTRAINT, groupDef.logo.constraint);
        }
        if (groupDef.policy != policy) {
            updates.put(POLICY, groupDef.policy);
        }
        if (extrasDef.backgroundControl != backgroundControl) {
            updates.put(BACKGROUND_CONTROL, extrasDef.backgroundControl);
        }
        if (extrasDef.infoBackground != null && (infoBackgroundHash == null ||
            !extrasDef.infoBackground.equals(new MediaDesc(infoBackgroundHash, 
            infoBackgroundMimeType, infoBackgroundThumbConstraint)))) {
            updates.put(INFO_BACKGROUND_HASH, extrasDef.infoBackground.hash);
            updates.put(INFO_BACKGROUND_MIME_TYPE, extrasDef.infoBackground.mimeType);
            // the thumbnail constraint (instead of the photo constraint) is stored in these
            // MediaDescs - see GroupEdit
            updates.put(INFO_BACKGROUND_THUMB_CONSTRAINT, extrasDef.infoBackground.constraint);
        }
        if (extrasDef.detailBackground != null && (detailBackgroundHash == null ||
            !extrasDef.detailBackground.equals(new MediaDesc(detailBackgroundHash,
            detailBackgroundMimeType, detailBackgroundThumbConstraint)))) {
            updates.put(DETAIL_BACKGROUND_HASH, extrasDef.detailBackground.hash);
            updates.put(DETAIL_BACKGROUND_MIME_TYPE, extrasDef.detailBackground.mimeType);
            updates.put(DETAIL_BACKGROUND_THUMB_CONSTRAINT, extrasDef.detailBackground.constraint);
            updates.put(DETAIL_BACKGROUND_WIDTH, extrasDef.detailBackgroundWidth);
            updates.put(DETAIL_AREA_HEIGHT, extrasDef.detailAreaHeight);
        }
        if (extrasDef.peopleBackground != null && (peopleBackgroundHash == null ||
            !extrasDef.peopleBackground.equals(new MediaDesc(peopleBackgroundHash,
            peopleBackgroundMimeType, peopleBackgroundThumbConstraint)))) {
            updates.put(PEOPLE_BACKGROUND_HASH, extrasDef.peopleBackground.hash);
            updates.put(PEOPLE_BACKGROUND_MIME_TYPE, extrasDef.peopleBackground.mimeType);
            updates.put(PEOPLE_BACKGROUND_THUMB_CONSTRAINT, extrasDef.peopleBackground.constraint);
        }
        if (extrasDef.peopleUpperCap != null && (peopleUpperCapHash == null || 
            !extrasDef.peopleUpperCap.equals(new MediaDesc(peopleUpperCapHash,
            peopleUpperCapMimeType)))) {
            updates.put(PEOPLE_UPPER_CAP_HASH, extrasDef.peopleUpperCap.hash);
            updates.put(PEOPLE_UPPER_CAP_MIME_TYPE, extrasDef.peopleUpperCap.mimeType);
            updates.put(PEOPLE_UPPER_CAP_HEIGHT, extrasDef.peopleUpperCapHeight);
        }
        if (extrasDef.peopleLowerCap != null && (peopleLowerCapHash == null || 
            !extrasDef.peopleLowerCap.equals(new MediaDesc(peopleLowerCapHash,
            peopleLowerCapMimeType)))) {
            updates.put(PEOPLE_LOWER_CAP_HASH, extrasDef.peopleLowerCap.hash);
            updates.put(PEOPLE_LOWER_CAP_MIME_TYPE, extrasDef.peopleLowerCap.mimeType);
            updates.put(PEOPLE_LOWER_CAP_HEIGHT, extrasDef.peopleLowerCapHeight);
        }
        if (extrasDef.charter != null && !extrasDef.charter.equals(charter)) {
            updates.put(CHARTER, extrasDef.charter);
        }
        if (extrasDef.homepageUrl != null && !extrasDef.homepageUrl.equals(homepageUrl)) {
            updates.put(HOMEPAGE_URL, extrasDef.homepageUrl);
        }
    
        return updates;
    }

    /**
     * Generates a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        StringBuilder buf = new StringBuilder("[");
        StringUtil.fieldsToString(buf, this);
        return buf.append("]").toString();
    }
}
