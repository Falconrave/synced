//
// $Id$

package client.groups;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;

import com.threerings.msoy.room.gwt.RoomInfo;
import com.threerings.msoy.room.gwt.WebRoomService;
import com.threerings.msoy.room.gwt.WebRoomServiceAsync;

import client.room.RoomWidget;
import client.ui.TongueBox;
import client.util.InfoCallback;
import client.util.ServiceUtil;

/**
 * Displays the rooms of a particular Group.
 */
public class GroupRoomsPanel extends VerticalPanel
{
    public GroupRoomsPanel (GroupDetail detail)
    {
        _detail = detail;

        _roomsvc.loadGroupRooms(
            _detail.group.groupId, new InfoCallback<WebRoomService.RoomsResult>() {
            public void onSuccess (WebRoomService.RoomsResult result) {
                init(result);
            }
        });
    }

    protected void init (WebRoomService.RoomsResult rooms)
    {
        _myRooms = rooms.callerRooms;
        add(new TongueBox(null, _msgs.detailRoomsDetail(_detail.group.name), false));
        _roomsGrid = new SmartTable(0, 0);
        for (int ii = 0; ii < rooms.groupRooms.size(); ii++) {
            int row = ii / ROOM_COLUMNS, col = ii % ROOM_COLUMNS;
            _roomsGrid.setWidget(row, col, new RoomWidget(rooms.groupRooms.get(ii)));
        }
        add(new TongueBox(_msgs.detailRoomsTitle(_detail.group.name), _roomsGrid));

        VerticalPanel transferPanel = new VerticalPanel();
        transferPanel.add(new Label(_msgs.detailTransferRoomInfo()));
        HorizontalPanel transferForm = new HorizontalPanel();
        transferPanel.add(transferForm);
        transferForm.setSpacing(10);
        transferForm.add(_roomsListBox = new ListBox());
        for (RoomInfo callerRoom : _myRooms) {
            _roomsListBox.addItem(callerRoom.name);
        }
        Button transferButton = new Button(_msgs.detailTransferRoom(_detail.group.name),
            new ClickHandler() {
                public void onClick (ClickEvent event) {
                    transferCurrentRoom();
                }
            });
        transferForm.add(transferButton);
        add(new TongueBox(_msgs.detailCallersRoomsTitle(), transferPanel));
    }

    protected void transferCurrentRoom ()
    {
        final int index = _roomsListBox.getSelectedIndex();
        if (index < 0) {
            return;
        }
        RoomInfo room = _myRooms.get(index);
        _groupsvc.transferRoom(_detail.group.groupId, room.sceneId, new InfoCallback<Void>() {
            public void onSuccess (Void result) {
                moveSceneToGrid(index);
            }
        });
    }

    protected void moveSceneToGrid (int index)
    {
        // TODO if we leave this tab and come back to it, this data should be refreshed from the
        // server
        RoomInfo room = _myRooms.remove(index);
        _roomsListBox.removeItem(index);
        int row = _roomsGrid.getRowCount() - 1;
        int col = _roomsGrid.getCellCount(row);
        if (col >= ROOM_COLUMNS) {
            row++;
            col = 0;
        } else {
            col++;
        }
        _roomsGrid.setWidget(row, col, new RoomWidget(room));
    }

    protected GroupDetail _detail;
    protected List<RoomInfo> _myRooms;
    protected ListBox _roomsListBox;
    protected SmartTable _roomsGrid;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)
        ServiceUtil.bind(GWT.create(GroupService.class), GroupService.ENTRY_POINT);
    protected static final WebRoomServiceAsync _roomsvc = (WebRoomServiceAsync)
        ServiceUtil.bind(GWT.create(WebRoomService.class), WebRoomService.ENTRY_POINT);

    protected static final int ROOM_COLUMNS = 2;
}
