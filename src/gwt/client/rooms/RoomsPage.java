//
// $Id$

package client.rooms;

import com.google.gwt.core.client.GWT;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.shell.Page;

/**
 * Handles the MetaSOY main page.
 */
public class RoomsPage extends Page
{
    public static final String ROOM_DETAIL = "room";
    public static final String DESIGN_WINNERS = "designwinners";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (ROOM_DETAIL.equals(action)) {
            setContent(new RoomDetailPanel(args.get(1, 0)));

        } else if (action.equals(DESIGN_WINNERS)) {
            setContent(_msgs.titleDesignWinners(), new DesignWinnersPanel());

        } else {
            setContent(new RoomsPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.ROOMS;
    }

    protected static final RoomsMessages _msgs = GWT.create(RoomsMessages.class);
    protected static final WebRoomServiceAsync _worldsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);
}
