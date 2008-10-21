//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Landing page for the Deviant Art "Design Your Whirled" contest. Images contain English text at
 * fixed sizes. Production-specific content links are present here and in
 * LandingMessages.properties. This contest will be live for three weeks starting 10/24/2008, then
 * permanently taken down.
 */
public class DAContestPanel extends SimplePanel
{
    public DAContestPanel ()
    {
        setStyleName("daContestPanel");
        AbsolutePanel content = MsoyUI.createAbsolutePanel("Content");
        setWidget(content);

        // design your whirled header
        content.add(new Image("/images/landing/dacontest_header.png"));

        // rooms list floats over on the right
        FlowPanel rooms = MsoyUI.createFlowPanel("Rooms");
        content.add(rooms, 660, 135);
        rooms.add(new Image("/images/landing/dacontest_rooms_top.png"));
        FlowPanel roomsList = MsoyUI.createFlowPanel("RoomsList");
        rooms.add(roomsList);
        for (int ii = 0; ii < COOL_ROOM_IDS.length; ii++) {
            int sceneId = COOL_ROOM_IDS[ii];
            String roomName = COOL_ROOM_NAMES[ii];
            String roomImageURL = "/images/landing/dacontest_room_" + sceneId + ".png";
            ClickListener onClick = Link.createListener(Pages.WORLD, "s" + sceneId);
            Image roomImage = MsoyUI.createActionImage(roomImageURL, roomName, onClick);
            roomImage.addStyleName("RoomImage");
            roomsList.add(roomImage);
            roomsList.add(MsoyUI.createActionLabel(roomName, "RoomName", onClick));
        }
        rooms.add(new Image("/images/landing/dacontest_rooms_bottom.png"));

        // how to enter and step panels are large images that include text
        AbsolutePanel howToEnter = MsoyUI.createAbsolutePanel("HowToEnter");
        content.add(howToEnter);
        howToEnter.add(MsoyUI.createButton(MsoyUI.LONG_THICK, "Join Whirled!",
            Link.createListener(Pages.ACCOUNT, "create")), 455, 345);

        AbsolutePanel step1 = MsoyUI.createAbsolutePanel("Step1");
        content.add(step1);
        step1.add(createInvisiLink(Link.createListener(Pages.SHOP, "3"), 50, 20), 560, 145);
        step1.add(createInvisiLink("http://wiki.whirled.com/", 90, 20), 490, 160);

        AbsolutePanel step2 = MsoyUI.createAbsolutePanel("Step2");
        content.add(step2);

        AbsolutePanel step3 = MsoyUI.createAbsolutePanel("Step3");
        content.add(step3);
        step3.add(createInvisiLink(
            "http://www.deviantart.com/#catpath=projects/contests/2008/whirled", 290, 20), 30,
            480);

        // Rules are in text
        AbsolutePanel rules = MsoyUI.createAbsolutePanel("Rules");
        content.add(rules);
        rules.add(MsoyUI.createHTML(_msgs.dacontestRules(), null), 70, 70);
        rules.add(MsoyUI.createHTML(_msgs.dacontestJudging(), "Judging"), 450, 80);
        rules.add(MsoyUI.createActionImage("/images/landing/dacontest_rules_join.png",
            Link.createListener(Pages.ACCOUNT, "create")), 540, 200);

        // tools icon sits over everything
        content.add(new Image("/images/landing/dacontest_tools.png"), 130, 110);
    }

    /**
     * Helper function for creating an invisible area of a given size with a given offsite link
     * that will be opened in a new window.
     */
    protected Widget createInvisiLink (final String offsiteLinkPath, int width, int height)
    {
        return createInvisiLink(new ClickListener() {
            public void onClick (Widget sender) {
                Window.open(offsiteLinkPath, "_blank", null);
            }
        }, width, height);
    }

    /**
     * Helper function for creating an invisible area of a given size with a given click event,
     * used to create a sort of imagemap.
     */
    protected Widget createInvisiLink (ClickListener listener, int width, int height)
    {
        Image image = MsoyUI.createActionImage("/images/landing/dacontest_blank.png", listener);
        image.setWidth(width + "px");
        image.setHeight(height + "px");
        return image;
    }

    /** Scenes to display under "check out these cool rooms!"; images are indexed by id */
    protected static final int[] COOL_ROOM_IDS = { 1, 2, 3, 1, 2, 3, 1, 2, 3, 1 };
    protected static final String[] COOL_ROOM_NAMES = { "OOO Tentacles", "Club Bella",
        "Corpse Craft", "OOO Tentacles", "Club Bella", "Corpse Craft", "OOO Tentacles",
        "Club Bella", "Corpse Craft", "OOO Tentacles" };

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
