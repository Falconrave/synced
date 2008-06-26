//
// $Id$

package client.me;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MyWhirledData;

import client.images.next.NextImages;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;
import client.util.RoundBox;
import client.util.ThumbBox;

/**
 * Displays blurbs about things to do and a player's online friends.
 */
public class WhatsNextPanel extends SmartTable
{
    public WhatsNextPanel (MyWhirledData data)
    {
        super("whatsNext", 0, 0);

        setWidget(0, 0, createPlay(data), 1, "Play");
        setWidget(0, 1, createExplore(data), 1, "Explore");
        if (data.friends == null || data.friends.size() == 0) {
            setWidget(0, 2, createNoFriends(data), 1, "NoFriends");
        } else {
            setWidget(0, 2, createFriends(data), 1, "Friends");
        }
        getFlexCellFormatter().setRowSpan(0, 2, 2);
        setWidget(1, 0, createDecorate(data), 2, "Decorate");
    }

    protected Widget createPlay (MyWhirledData data)
    {
        RoundBox box = new RoundBox(RoundBox.BLUE);
        box.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        Image shot = GAME_SHOTS[Random.nextInt(GAME_SHOTS.length)].createImage();
        ClickListener onClick = Application.createLinkListener(Page.GAMES, "");
        box.add(MsoyUI.makeActionImage(shot, null, onClick));
        box.add(WidgetUtil.makeShim(10, 10));
        box.add(MsoyUI.createButton(MsoyUI.LONG_THIN, CMe.msgs.nextPlay(), onClick));
        box.add(WidgetUtil.makeShim(5, 5));
        box.add(MsoyUI.createLabel(CMe.msgs.nextPlayTip(), "tipLabel"));
        return box;
    }

    protected Widget createExplore (MyWhirledData data)
    {
        RoundBox box = new RoundBox(RoundBox.BLUE);
        box.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        Image shot = WHIRLED_SHOTS[Random.nextInt(WHIRLED_SHOTS.length)].createImage();
        ClickListener onClick = Application.createLinkListener(Page.WHIRLEDS, "");
        box.add(MsoyUI.makeActionImage(shot, null, onClick));
        box.add(WidgetUtil.makeShim(10, 10));
        box.add(MsoyUI.createButton(MsoyUI.LONG_THIN, CMe.msgs.nextExplore(), onClick));
        box.add(WidgetUtil.makeShim(5, 5));
        box.add(MsoyUI.createLabel(CMe.msgs.nextExploreTip(), "tipLabel"));
        return box;
    }

    protected Widget createDecorate (MyWhirledData data)
    {
        RoundBox box = new RoundBox(RoundBox.BLUE);
        SmartTable contents = new SmartTable(0, 0);
        ClickListener onClick = Application.createLinkListener(Page.WORLD,
            "m" + CMe.getMemberId());
        contents.setWidget(0, 0, _images.home_shot().createImage(), 1, "Screen");
        contents.getFlexCellFormatter().setRowSpan(0, 0, 2);
        contents.setWidget(0, 1, MsoyUI.createButton(
                               MsoyUI.MEDIUM_THIN, CMe.msgs.nextDecorate(), onClick));
        contents.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_CENTER);
        contents.setText(1, 0, CMe.msgs.nextDecorateTip(), 1, "tipLabel");
        box.add(contents);
        return box;
    }

    protected Widget createNoFriends (MyWhirledData data)
    {
        SmartTable friends = new SmartTable(0, 0);
        friends.setHeight("100%");
        friends.setText(0, 0, CMe.msgs.nextFriends(), 1, "Title");
        friends.setText(1, 0, CMe.msgs.nextNoFriends(), 1, "NoFriends");
        friends.setWidget(2, 0, Application.createImageLink(
                              "/images/me/invite_friends.png",
                              CMe.msgs.nextInviteTip(), Page.PEOPLE, "invites"));
        friends.setText(3, 0, CMe.msgs.nextOr(), 1, "Or");
        friends.setText(4, 0, CMe.msgs.nextFind(), 1, "Title");

        HorizontalPanel sctrls = new HorizontalPanel();
        sctrls.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
        final TextBox search = MsoyUI.createTextBox("", -1, -1);
        search.setWidth("150px");
        sctrls.add(search);
        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                String query = search.getText().trim();
                if (query.length() > 0) {
                    Application.go(Page.PEOPLE, Args.compose("search", "0", query));
                }
            }
        };
        search.addKeyboardListener(new EnterClickAdapter(onClick));
        sctrls.add(WidgetUtil.makeShim(5, 5));
        sctrls.add(new Button("Search", onClick));

        friends.setWidget(5, 0, sctrls);
        friends.setText(6, 0, CMe.msgs.nextFindTip(), 1, "FindTip");
        return friends;
    }

    protected Widget createFriends (MyWhirledData data)
    {
        FlowPanel friends = new FlowPanel();
        friends.add(MsoyUI.createLabel(CMe.msgs.nextFriends(), "Title"));
        friends.add(MsoyUI.createLabel(CMe.msgs.nextFriendClick(), "ClickTip"));

        // if we have few friends, show larger photo images
        int size = (data.friends.size() > 6) ?
            MediaDesc.QUARTER_THUMBNAIL_SIZE : MediaDesc.HALF_THUMBNAIL_SIZE;

        // group our friends by location (in rooms or games)
        Map games = new HashMap();
        Map rooms = new HashMap();
        for (MemberCard card : data.friends) {
            if (card.status instanceof MemberCard.InScene) {
                int sceneId = ((MemberCard.InScene)card.status).sceneId;
                FlowPanel room = getPlacePanel(
                    rooms, sceneId, ((MemberCard.InScene)card.status).sceneName);
                ClickListener onClick = Application.createLinkListener(Page.WORLD, "s"+sceneId);
                Widget member = makeMemberWidget(card, size, onClick);
                member.addStyleName("MemberRoom");
                room.add(member);

            } else if (card.status instanceof MemberCard.InGame) {
                int gameId = ((MemberCard.InGame)card.status).gameId;
                FlowPanel game = getPlacePanel(
                    games, gameId, ((MemberCard.InGame)card.status).gameName);
                ClickListener onClick = Application.createLinkListener(
                    Page.WORLD, Args.compose("game", "l", ""+gameId));
                Widget member = makeMemberWidget(card, size, onClick);
                member.addStyleName("MemberGame");
                game.add(member);
            }
        }

        // now add the rooms and games to our scrolling contents (rooms first)
        for (Iterator iter = rooms.values().iterator(); iter.hasNext(); ) {
            friends.add((FlowPanel)iter.next());
        }
        for (Iterator iter = games.values().iterator(); iter.hasNext(); ) {
            friends.add((FlowPanel)iter.next());
        }

        ScrollPanel scroller = new ScrollPanel(friends);
        scroller.addStyleName("FriendsScroller");
        return scroller;
    }

    protected FlowPanel getPlacePanel (Map places, int placeId, String placeName)
    {
        Integer key = new Integer(placeId);
        FlowPanel place = (FlowPanel)places.get(key);
        if (place == null) {
            places.put(key, place = new FlowPanel());
            place.add(MsoyUI.createLabel(placeName, "PlaceName"));
        }
        return place;
    }

    protected Widget makeMemberWidget (MemberCard card, int size, ClickListener onClick)
    {
        SmartTable member = new SmartTable("Member", 0, 0);
        member.setWidget(0, 0, new ThumbBox(card.photo, size, onClick), 1, "Photo");
        member.setWidget(0, 1, MsoyUI.createActionLabel(card.name.toString(), null, onClick),
                         1, "Name");
        return member;
    }

    /** Our screenshot images. */
    protected static NextImages _images = (NextImages)GWT.create(NextImages.class);

    protected static final AbstractImagePrototype[] GAME_SHOTS = {
        _images.astro_shot(), _images.brawler_shot(), _images.dict_shot(),
        _images.drift_shot(), _images.lol_shot()
    };

    protected static final AbstractImagePrototype[] WHIRLED_SHOTS = {
        _images.brave_shot(), _images.kawaii_shot(), _images.nap_shot(),
        _images.pirate_shot(), _images.rave_shot()
    };
}
