//
// $Id$

package client.person;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlinePanel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedItemGenerator;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Builder;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Icon;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Media;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Messages;
import com.threerings.msoy.person.gwt.FeedItemGenerator.Plural;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Display a single news feed item, formatted based on type.
 */
public class FeedMessagePanel extends FocusPanel
{
    /**
     * @param usePronouns If true, say e.g. "You earned the trophy" if current member is the actor.
     */
    public FeedMessagePanel (FeedMessage message, boolean usePronouns)
    {
        Builder builder = new Builder() {
            public Media createMedia (MediaDesc md, final Pages page, final Args args) {
                ClickHandler clicker = new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        Link.go(page, args);
                    }
                };
                int size = MediaDesc.HALF_THUMBNAIL_SIZE;
                if (page == Pages.WORLD && args.get(0, "").startsWith("s")) {
                    // snapshots are unconstrained at a set size; fake a width constraint for
                    // TINY_SIZE.
                    md.constraint = MediaDesc.HORIZONTALLY_CONSTRAINED;
                    size = MediaDesc.SNAPSHOT_TINY_SIZE;
                }
                return new WidgetWrapper(MediaUtil.createMediaView(md, size, clicker));
            }

            public String createLink (String label, Pages page, Args args) {
                return Link.createHtml(label, page, args);
            }

            public void addMedia (Media media, String message) {
                add(new ThumbnailWidget(((WidgetWrapper)media).widget, message));
            }

            public Icon createGainedLevelIcon (String message) {
                return new WidgetWrapper(new IconWidget("friend_gained_level", message));
            }

            public void addIcon (Icon icon) {
                add(((WidgetWrapper)icon).widget);
            }

            public void addMedia (Media[] media, String message) {
                List<Widget> widgets = new ArrayList<Widget>();
                for (Media m : media) {
                    widgets.add(((WidgetWrapper)m).widget);
                }
                Widget[] widgetArray = widgets.toArray(new Widget[widgets.size()]);
                add(new ThumbnailWidget(widgetArray, message));
            }

            public void addText (String text) {
                add(new BasicWidget(text));
            }
        };

        Messages messages = new Messages() {
            public String typeName (String itemType) {
                return _dmsgs.xlate("itemType" + itemType);
            }

            public String you () {
                return "<b>" + _pmsgs.feedProfileMemberYou() + "</b>";
            }

            public String describeItem (String typeName, String itemName) {
                return _pmsgs.descCombine(typeName, itemName);
            }

            public String badgeName (int code, String levelName) {
                String hexCode = Integer.toHexString(code);
                return _dmsgs.get("badge_" + hexCode, levelName);
            }

            public String medal (String medal, String group) {
                return _pmsgs.medal(medal, group);
            }

            public String unknownMember () {
                return _pmsgs.feedProfileMemberUnknown();
            }

            public String action (
                FeedMessageType type, String subject, String object, Plural plural) {
                switch (type) {
                case GLOBAL_ANNOUNCEMENT:
                    return _pmsgs.globalAnnouncement(object);

                case FRIEND_ADDED_FRIEND:
                    return _pmsgs.friendAddedFriend(subject, object);

                case FRIEND_UPDATED_ROOM:
                    switch (plural) {
                    default:
                    case NONE:
                        return _pmsgs.friendUpdatedRoom(subject, object);
                    case SUBJECT:
                        return _pmsgs.friendsUpdatedRoom(subject);
                    case OBJECT:
                        return _pmsgs.friendUpdatedRooms(subject, object);
                    }

                case FRIEND_WON_TROPHY:
                    return plural == Plural.OBJECT ?
                        _pmsgs.friendWonTrophies(subject, object) :
                        _pmsgs.friendWonTrophy(subject, object);

                case FRIEND_LISTED_ITEM:
                    return _pmsgs.friendListedItem(subject, object);

                case FRIEND_GAINED_LEVEL:
                    return plural == Plural.SUBJECT ?
                        _pmsgs.friendsGainedLevel(subject) :
                        _pmsgs.friendGainedLevel(subject, object);

                case FRIEND_WON_BADGE:
                    return plural == Plural.OBJECT ?
                        _pmsgs.friendWonBadges(subject, object) :
                        _pmsgs.friendWonBadge(subject, object);

                case FRIEND_WON_MEDAL:
                    return _pmsgs.friendWonMedal(subject, object);

                case FRIEND_SUBSCRIBED:
                    return plural == Plural.SUBJECT ?
                        _pmsgs.friendsSubscribed(subject) : _pmsgs.friendSubscribed(subject);

                case FRIEND_CREATED_GROUP:
                    return _pmsgs.friendCreatedGroup(subject, object);

                case FRIEND_JOINED_GROUP:
                    switch (plural) {
                    default:
                    case NONE: return _pmsgs.friendJoinedGroup(subject, object);
                    case SUBJECT: return _pmsgs.friendsJoinedGroup(subject, object);
                    case OBJECT: return _pmsgs.friendJoinedGroups(subject, object);
                    }

                case GROUP_ANNOUNCEMENT:
                    return _pmsgs.groupAnnouncement(subject, object);

                case GROUP_UPDATED_ROOM:
                    return _pmsgs.friendUpdatedRoom(subject, object);

                case SELF_ROOM_COMMENT:
                    return _pmsgs.selfRoomComment(subject, object);

                case SELF_ITEM_COMMENT:
                    return _pmsgs.selfItemComment(subject, object);

                case SELF_GAME_COMMENT:
                    return _pmsgs.selfGameComment(subject, object);

                case SELF_FORUM_REPLY:
                    switch (plural) {
                    default:
                    case NONE: return _pmsgs.selfPersonRepliedToForumPost(subject, object);
                    case SUBJECT: return _pmsgs.selfPeopleRepliedToForumPost(subject, object);
                    case OBJECT: return _pmsgs.selfPersonRepliedToForumPosts(subject, object);
                    }

                case FRIEND_PLAYED_GAME:
                    switch (plural) {
                    default:
                    case NONE: return _pmsgs.friendPlayedGame(subject, object);
                    case SUBJECT: return _pmsgs.friendsPlayedGame(subject, object);
                    case OBJECT: return _pmsgs.friendPlayedGames(subject, object);
                    }

                default:
                    return subject + " " + type + " " + object + " (plural: " + plural + ").";
                }
            }

            public String andCombine (String list, String item) {
                return _pmsgs.andCombine(list, item);
            }

            public String briefLevelGain (String subject, String level) {
                return _pmsgs.colonCombine(subject, level);
            }

            public String commaCombine (String list, String item) {
                return _pmsgs.commaCombine(list, item);
            }
        };

        FeedItemGenerator gen = new FeedItemGenerator(
            CShell.getMemberId(), usePronouns, builder, messages);
        gen.addMessage(message);
    }

    protected static class WidgetWrapper
        implements Media, Icon
    {
        public Widget widget;

        public WidgetWrapper (Widget widget)
        {
            this.widget = widget;
        }
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

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final PersonMessages _pmsgs = GWT.create(PersonMessages.class);
}
