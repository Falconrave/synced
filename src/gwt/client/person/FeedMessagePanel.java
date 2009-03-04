//
//
// $Id: FeedPanel.java 12917 2008-10-28 20:10:30Z sarah $

package client.person;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlinePanel;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.FriendFeedMessage;
import com.threerings.msoy.person.gwt.GroupFeedMessage;
import com.threerings.msoy.person.gwt.SelfFeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageAggregator.AggregateMessage;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.NaviUtil;

/**
 * Display a single news feed item, formatted based on type.
 */
public class FeedMessagePanel extends FocusPanel
{
    /**
     * @param usePronouns If true, say "You updated the trophy" if current member is the actor.
     */
    public FeedMessagePanel (FeedMessage message, boolean usePronouns)
    {
        this._usePronouns = usePronouns;

        if (message instanceof FriendFeedMessage) {
            addFriendMessage((FriendFeedMessage)message);
        } else if (message instanceof GroupFeedMessage) {
            addGroupMessage((GroupFeedMessage)message);
        } else if (message instanceof SelfFeedMessage) {
            addSelfMessage((SelfFeedMessage)message);
        } else if (message instanceof AggregateMessage) {
            if (((AggregateMessage)message).left) {
                this.addLeftAggregateMessage(((AggregateMessage)message).messages);
            } else {
                this.addRightAggregateMessage(((AggregateMessage)message).messages);
            }
        } else {
            addMessage(message);
        }
    }

    protected void addFriendMessage (FriendFeedMessage message)
    {
        String friendLink = profileLink(message);
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendAddedFriend(friendLink,
                buildString(message))));
            break;

        case FRIEND_UPDATED_ROOM:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendUpdatedRoom(friendLink,
                buildString(message))));
            break;

        case FRIEND_WON_TROPHY:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonTrophy(
                            friendLink, buildString(message))));
            break;

        case FRIEND_LISTED_ITEM:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendListedItem(
                            friendLink, buildString(message))));
            break;

        case FRIEND_GAINED_LEVEL:
            add(new IconWidget("friend_gained_level", _pmsgs.friendGainedLevel(
                            friendLink, buildString(message))));

        case FRIEND_WON_BADGE:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonBadge(
                            friendLink, buildString(message))));
            break;

        case FRIEND_WON_MEDAL:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonMedal(
                            friendLink, buildString(message))));
            break;
        }
    }

    protected void addGroupMessage (GroupFeedMessage message)
    {
        switch (message.type) {
        case GROUP_ANNOUNCEMENT:
            String threadLink = Link.createHtml(
                message.data[1], Pages.GROUPS, Args.compose("t", message.data[2]));
            add(new ThumbnailWidget(buildMedia(message),
                _pmsgs.groupAnnouncement(message.data[0], threadLink)));
            break;

        case GROUP_UPDATED_ROOM:
            String groupLink = Link.createHtml(message.group.toString(), Pages.GROUPS,
                Args.compose("f", message.group.getGroupId()));
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendUpdatedRoom(
                groupLink, buildString(message))));
            break;

        default:
            break;
        }
    }

    protected void addSelfMessage (SelfFeedMessage message)
    {
        switch (message.type) {
        case SELF_ROOM_COMMENT:
            if (message.actor == null) {
                return; // TEMP: skip old pre-actor messages
            }
            String roomText = _pmsgs.selfRoomComment(profileLink(message),
                buildString(message));
            add(new ThumbnailWidget(buildMedia(message), roomText));
            break;

        case SELF_ITEM_COMMENT:
            String itemText = _pmsgs.selfItemComment(profileLink(message), buildString(message));
            add(new ThumbnailWidget(buildMedia(message), itemText));
            break;

        case SELF_FORUM_REPLY:
            String replyText = _pmsgs.selfForumReply(profileLink(message), buildString(message));
            add(new BasicWidget(replyText));
            break;
        }
    }

    protected void addMessage (FeedMessage message)
    {
        switch (message.type) {
        case GLOBAL_ANNOUNCEMENT:
            String threadLink = Link.createHtml(
                message.data[0], Pages.GROUPS, Args.compose("t", message.data[1]));
            add(new BasicWidget(_pmsgs.globalAnnouncement(threadLink)));
            break;
        }
    }

    protected String profileLink (FeedMessage message)
    {
        MemberName member;
        if (message instanceof FriendFeedMessage) {
            member = ((FriendFeedMessage)message).friend;
        } else if (message instanceof SelfFeedMessage) {
            member = ((SelfFeedMessage)message).actor;
        } else {
            member = null;
        }
        if (member == null) {
            // very old data may not include actor/friend
            return _pmsgs.feedProfileMemberUnknown();
        }
        return profileLink(member.toString(), String.valueOf(member.getMemberId()));
    }

    protected String profileLink (String name, String id)
    {
        if (_usePronouns && id.trim().equals(CShell.getMemberId() + "")) {
            return _pmsgs.feedProfileMemberYou();
        }
        return Link.createHtml(name, Pages.PEOPLE, id);
    }

    /**
     * Helper function which creates translated strings of a feed messages data.
     */
    protected String buildString (FeedMessage message)
    {
        switch (message.type.getCategory()) {
        case FRIENDINGS:
            return profileLink(message.data[0], message.data[1]);

        case ROOMS:
            return Link.createHtml(message.data[1], Pages.WORLD, "s" + message.data[0]);

        case TROPHIES:
            return Link.createHtml(message.data[0], Pages.GAMES,
                                   NaviUtil.gameDetail(Integer.valueOf(message.data[1]),
                                                       NaviUtil.GameDetails.TROPHIES));

        case LISTED_ITEMS:
            return _pmsgs.descCombine(
                _dmsgs.xlate("itemType" + message.data[1]),
                        Link.createHtml(message.data[0], Pages.SHOP,
                            Args.compose("l", message.data[1], message.data[2])));

        case LEVELS:
            return message.data[0];

        case BADGES:
            int badgeCode = Integer.parseInt(message.data[0]);
            int badgeLevel = Integer.parseInt(message.data[1]);
            String badgeHexCode = Integer.toHexString(badgeCode);
            String badgeName =
                _dmsgs.get("badge_" + badgeHexCode, Badge.getLevelName(badgeLevel));

            int memberId = ((FriendFeedMessage)message).friend.getMemberId();
            return Link.createHtml(badgeName, Pages.ME, Args.compose("passport", memberId));

        case MEDALS:
            memberId = ((FriendFeedMessage)message).friend.getMemberId();
            String medalLink =
                Link.createHtml(message.data[0], Pages.ME, Args.compose("medals", memberId));
            if (message.data.length < 4) {
                // legacy medal messages are missing group info.
                return _pmsgs.medalNoGroup(medalLink);
            }
            String groupLink =
                Link.createHtml(message.data[2], Pages.GROUPS, Args.compose("d", message.data[3]));
            return _pmsgs.medal(medalLink, groupLink);

        case COMMENTS:
            if (message.type == FeedMessageType.SELF_ROOM_COMMENT) {
                return Link.createHtml(message.data[1], Pages.ROOMS, Args.compose("room",
                    message.data[0]));

            } else if (message.type == FeedMessageType.SELF_ITEM_COMMENT) {
                return Link.createHtml(message.data[2], Pages.SHOP, Args.compose("l",
                    message.data[0], message.data[1]));
            }

        case FORUMS:
            return Link.createHtml(message.data[1], Pages.GROUPS, Args.compose("t",
                message.data[0]));
        }

        return null;
    }

    /**
     * Helper function which creates a clickable widget from the supplied media information.
     */
    protected Widget buildMedia (final FeedMessage message)
    {
        MediaDesc media;
        ClickListener clicker;
        switch (message.type.getCategory()) {
        case FRIENDINGS:
            if (message.data.length < 3) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[2]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender)
                {
                    Link.go(Pages.PEOPLE, message.data[1]);
                }
            };
            return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

        case ROOMS:
            if (message.data.length < 3) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[2]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender)
                {
                    Link.go(Pages.WORLD, "s" + message.data[0]);
                }
            };
            // snapshots are unconstrained at a set size; fake a width constraint for TINY_SIZE.
            media.constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
            return MediaUtil.createMediaView(media, MediaDesc.SNAPSHOT_TINY_SIZE, clicker);

        case TROPHIES:
            media = MediaDesc.stringToMD(message.data[2]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.GAMES, NaviUtil.gameDetail(Integer.valueOf(message.data[1]),
                                                            NaviUtil.GameDetails.TROPHIES));
                }
            };
            return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

        case LISTED_ITEMS:
            if (message.data.length < 4) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[3]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(
                        Pages.SHOP, Args.compose("l", message.data[1], message.data[2]));
                }
            };
            return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

        case BADGES:
            int badgeCode = Integer.parseInt(message.data[0]);
            int level = Integer.parseInt(message.data[1]);
            int memberId = ((FriendFeedMessage)message).friend.getMemberId();
            Image image = new Image(EarnedBadge.getImageUrl(badgeCode, level));
            image.setWidth(MediaDesc.getWidth(MediaDesc.HALF_THUMBNAIL_SIZE) + "px");
            image.setHeight(MediaDesc.getHeight(MediaDesc.HALF_THUMBNAIL_SIZE) + "px");
            image.addClickListener(Link.createListener(
                Pages.ME, Args.compose("passport", memberId)));
            return image;

        case MEDALS:
            media = MediaDesc.stringToMD(message.data[1]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.ME, Args.compose("medals",
                        ((FriendFeedMessage)message).friend.getMemberId()));
                }
            };
            return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

        case ANNOUNCEMENTS:
            if (message.data.length < 4) {
                return null;
            }
            media = MediaDesc.stringToMD(message.data[3]);
            if (media == null) {
                return null;
            }
            clicker = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Pages.GROUPS, Args.compose("t", message.data[2]));
                }
            };
            return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);

        case COMMENTS:
            if (message.type == FeedMessageType.SELF_ROOM_COMMENT) {
                if (message.data.length < 3) {
                    return null;
                }
                media = MediaDesc.stringToMD(message.data[2]);
                if (media == null) {
                    return null;
                }
                clicker = new ClickListener() {
                    public void onClick (Widget sender) {
                        Link.go(Pages.WORLD, Args.compose("s", message.data[0]));
                    }
                };
                // snapshots are unconstrained at a set size; fake a width constraint for TINY_SIZE.
                media.constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
                return MediaUtil.createMediaView(media, MediaDesc.SNAPSHOT_TINY_SIZE, clicker);
    
            } else if (message.type == FeedMessageType.SELF_ITEM_COMMENT) {
                if (message.data.length < 4) {
                    return null;
                }
                media = MediaDesc.stringToMD(message.data[3]);
                if (media == null) {
                    return null;
                }
                clicker = new ClickListener() {
                    public void onClick (Widget sender) {
                        Link.go(Pages.SHOP, Args.compose("l", message.data[0], message.data[1]));
                    }
                };
                return MediaUtil.createMediaView(media, MediaDesc.HALF_THUMBNAIL_SIZE, clicker);
            }
        }
        return null;
    }

    protected static class IconWidget extends FlexTable
    {
        public IconWidget (String icon, String html)
        {
            setStyleName("FeedWidget");
            setCellSpacing(0);
            setCellPadding(0);

            Image image = new Image("/images/whirled/" + icon + ".png");
            image.setStyleName("FeedIcon");
            setWidget(0, 0, image);
            getFlexCellFormatter().setStyleName(0, 0, "IconContainer");

            setWidget(0, 1, MsoyUI.createHTML(html, null));
            getFlexCellFormatter().addStyleName(0, 1, "TextContainer");
        }
    }

    protected static class ThumbnailWidget extends SimplePanel
    {
        public ThumbnailWidget (Widget icon, String html)
        {
            this(icon == null ? null : new Widget[] { icon }, html);
        }

        public ThumbnailWidget (Widget[] icons, String html)
        {
            setStyleName("FeedWidget");
            InlinePanel contents = new InlinePanel("ThumbnailWidget");
            add(contents);
            if (icons != null && icons.length > 0) {
                for (Widget icon : icons) {
                    icon.addStyleName("ThumbnailContainer");
                    contents.add(icon);
                }
            }
            contents.add(MsoyUI.createHTML(html, "TextContainer"));
        }
    }

    public static class BasicWidget extends FlowPanel
    {
        public BasicWidget (String html)
        {
            setStyleName("FeedWidget");
            addStyleName("FeedBasic");
            add(MsoyUI.createHTML(html, null));
        }
    }

    /**
     * Display multiple actions by the same person (eg listing new things in the shop).
     */
    protected void addLeftAggregateMessage (List<FeedMessage> list)
    {
        FeedMessage message = list.get(0);
        String friendLink = profileLink(message);
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendAddedFriend(friendLink,
                standardCombine(list))));
            break;

        case FRIEND_UPDATED_ROOM:
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendUpdatedRooms(friendLink,
                standardCombine(list))));
            break;

        case FRIEND_WON_TROPHY:
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendWonTrophies(
                            friendLink, standardCombine(list))));
            break;

        case FRIEND_LISTED_ITEM:
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendListedItem(
                            friendLink, standardCombine(list))));
            break;

        case FRIEND_GAINED_LEVEL:
            // display all levels gained by all friends together
            add(new IconWidget("friend_gained_level",
                        _pmsgs.friendsGainedLevel(friendLinkCombine(list))));
            break;

        case FRIEND_WON_BADGE:
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendWonBadges(
                            friendLink, standardCombine(list))));
            break;

        case FRIEND_WON_MEDAL:
            add(new ThumbnailWidget(buildMediaArray(list), _pmsgs.friendWonMedal(
                            friendLink, standardCombine(list))));
            break;

        default:
            add(new BasicWidget("Unknown left aggregate type: " + message.type));
            break;
        }
    }

    /**
     * Display multiple people performing the same action (eg winning the same trophy).
     */
    protected void addRightAggregateMessage (List<FeedMessage> list)
    {
        FeedMessage message = list.get(0);
        String friendLinks = profileCombine(list);
        switch (message.type) {
        case FRIEND_ADDED_FRIEND:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendAddedFriend(
                friendLinks, buildString(message))));
            break;

        case FRIEND_WON_TROPHY:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonTrophy(
                            friendLinks, buildString(message))));
            break;

        case FRIEND_WON_BADGE:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonBadge(friendLinks,
                buildString(message))));
            break;

        case FRIEND_WON_MEDAL:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.friendWonMedal(friendLinks,
                buildString(message))));
            break;

        case SELF_ROOM_COMMENT:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.selfRoomComment(friendLinks,
                buildString(message))));
            break;

        case SELF_ITEM_COMMENT:
            add(new ThumbnailWidget(buildMedia(message), _pmsgs.selfItemComment(friendLinks,
                buildString(message))));
            break;

        default:
            add(new BasicWidget("Unknown right aggregate type: " + message.type));
            break;
        }
    }

    protected String standardCombine (List<FeedMessage> list)
    {
        return standardCombine(list, new StringBuilder() {
            public String build (FeedMessage message) {
                return buildString(message);
            }
        });
    }

    protected String friendLinkCombine (List<FeedMessage> list)
    {
        return standardCombine(list, new StringBuilder() {
            public String build (FeedMessage message) {
                return _pmsgs.colonCombine(
                    profileLink(message), buildString(message));
            }
        });
    }

    protected String profileCombine (List<FeedMessage> list)
    {
        return standardCombine(list, new StringBuilder() {
            public String build (FeedMessage message) {
                return profileLink(message);
            }
        });
    }

    /**
     * Helper function which combines the core feed message data into a translated, comma
     * separated and ending in 'and' list.
     */
    protected String standardCombine (List<FeedMessage> list, StringBuilder builder)
    {
        String combine = builder.build(list.get(0));
        for (int ii = 1, ll = list.size(); ii < ll; ii++) {
            FeedMessage message = list.get(ii);
            if (ii + 1 == ll) {
                combine = _pmsgs.andCombine(combine, builder.build(message));
            } else {
                combine = _pmsgs.commaCombine(combine, builder.build(message));
            }
        }
        return combine;
    }

    /**
     * Helper function which creates an array of media widgets from feed messages.
     */
    protected Widget[] buildMediaArray (List<FeedMessage> list)
    {
        List<Widget> media = new ArrayList<Widget>();
        for (FeedMessage message : list) {
            Widget w = buildMedia(message);
            if (w != null) {
                media.add(w);
            }

        }
        if (media.isEmpty()) {
            return null;
        }
        return media.toArray(new Widget[media.size()]);
    }

    protected interface StringBuilder
    {
        String build (FeedMessage message);
    }

    protected static final DateTimeFormat _dateFormater = DateTimeFormat.getFormat("MMMM d:");
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);

    /** Whether to say "You earned the trophy" or "Bob earned the trophy" */
    protected boolean _usePronouns;
}
