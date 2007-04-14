package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.Tile;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.controls.Label;
import mx.controls.Text;

import com.threerings.util.Name;

import com.threerings.flex.CommandButton;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.ezgame.data.EZGameConfig;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.data.MsoyTable;

import com.threerings.util.StringUtil;

public class TableRenderer extends HBox
{
    /** The context, initialized by our ClassFactory. */
    public var ctx :WorldContext;

    /** The panel we're rendering to. */
    public var panel :LobbyPanel;

    public function TableRenderer (popup :Boolean = false)
    {
        super();
        _popup = popup
        if (!_popup) {
            // when used in a List, we should not be included in the layout
            includeInLayout = false;
        }
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override public function set data (newData :Object) :void
    {
        super.data = newData;

        recheckTable();
    }

    override public function set width (width :Number) :void
    {
        super.width = width;
        if (_seatsGrid != null) {
            if (_popup) {
                _seatsGrid.width = width;
            } else if (_seatsGrid.numChildren > 2) {
                _seatsGrid.width = width - CONFIG_WIDTH - PADDING_WIDTH - 10; 
            }
        }
    }

    /** 
     * Get the amount of width we could use if we had the room.
     */
    public function get maxUsableWidth () :int
    {
        return _maxUsableWidth;
    }

    override protected function createChildren () :void
    {
        _game = panel.getGame();

        if (_popup) {
            styleName = "floatingTableRenderer";
        } else {
            styleName = "listTableRenderer";
        }

        if (!_popup) {
            _labelsBox = new VBox();
            _labelsBox.width = CONFIG_WIDTH;
            _labelsBox.setStyle("verticalGap", 0);
            _labelsBox.setStyle("paddingLeft", 4);
            _labelsBox.verticalScrollPolicy = ScrollPolicy.OFF;
            _labelsBox.horizontalScrollPolicy = ScrollPolicy.OFF;
            addChild(_labelsBox);
            var padding :VBox = new VBox();
            padding.setStyle("backgroundColor", 0xE0E7EE);
            padding.width = PADDING_WIDTH;
            padding.percentHeight = 100;
            addChild(padding);
        }
        var rightSide :VBox = new VBox();
        rightSide.percentWidth = 100;
        addChild(rightSide);
        rightSide.addChild(_seatsGrid = new Tile());
        _seatsGrid.verticalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.horizontalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.styleName = "seatsGrid";
    }

    protected function removeChildren () :void
    {
        while (numChildren > 0) {
            removeChild(getChildAt(0));
        }
    }

    protected function recheckTable () :void
    {
        var table :MsoyTable = (data as MsoyTable);
        if (table == null) {
            if (_creationPanel == null) {
                _creationPanel = new TableCreationPanel(ctx, panel);
            } 
            removeChildren();
            addChild(_creationPanel);
            panel.createBtn = _creationPanel.getCreateButton();
            return;
        } else if (getChildAt(0) is TableCreationPanel) {
            _creationPanel = null;
            removeChildren();
            createChildren();
        }

        if (!_popup) {
            _watcherCount = table.watcherCount;
        }

        // update the seats
        var length :int = table.occupants == null ? 0 : table.occupants.length;
        if (length != 0) {
            updateSeats(table, length);
        }
        // remove any extra seats/buttons, should there be any
        while (_seatsGrid.numChildren > length) {
            _seatsGrid.removeChildAt(length);
        }
        updateButtons(table);
        _seatsGrid.validateNow();
        _maxUsableWidth = (_seatsGrid.measuredMinWidth + HORZ_GAP) * _seatsGrid.numChildren +
            /* the mystery pixels strike again :/ */ 15;

        if (!_popup) {
            updateConfig(table);
            validateNow();
            if (_seatsGrid.numChildren > 2) {
                _seatsGrid.width = width - CONFIG_WIDTH - PADDING_WIDTH - 10; 
            }
        }
    }

    protected function updateSeats (table :MsoyTable, length :int) :void
    {
        for (var ii :int = 0; ii < length; ii++) {
            var seat :SeatRenderer;
            if (_seatsGrid.numChildren <= ii) {
                seat = new SeatRenderer();
                _seatsGrid.addChild(seat);
            } else if (!(_seatsGrid.getChildAt(ii) is SeatRenderer)) {
                seat = new SeatRenderer();
                _seatsGrid.addChildAt(seat, ii);
            } else {
                seat = (_seatsGrid.getChildAt(ii) as SeatRenderer);
            }
            seat.update(ctx, table, ii, panel.isSeated());
        }
    }

    protected function updateButtons (table :MsoyTable) :void
    {
        var btn :CommandButton;

        // if we are the creator, add a button for starting the game now
        if (table.occupants != null && table.occupants.length > 0 &&
                ctx.getMemberObject().getVisibleName().equals(table.occupants[0]) &&
                (table.tconfig.desiredPlayerCount > table.tconfig.minimumPlayerCount)) {
            var box :HBox = new HBox();
            box.styleName = "seatRenderer";
            box.setStyle("horizontalAlign", "center");
            box.percentWidth = 100;
            box.percentHeight = 100;
            btn = new CommandButton(LobbyController.START_TABLE, table.tableId);
            btn.label = ctx.xlate("game", "b.start_now");
            btn.enabled = table.mayBeStarted();
            box.addChild(btn);
            _seatsGrid.addChild(box);
        }

        // maybe add a button for entering the game
        if (table.gameOid != -1) {
            var key :String = null;
            switch (table.config.getGameType()) {
            case GameConfig.PARTY:
                key = "b.join";
                break;

            default:
                if (!_game.getGameDefinition().unwatchable) {
                    key = "b.watch";
                }
                break;
            }

            if (key != null) {
                box = new HBox();
                box.styleName = "seatRenderer";
                box.setStyle("horizontalAlign", "center");
                box.percentWidth = 100;
                box.percentHeight = 100;
                btn = new CommandButton(MsoyController.GO_LOCATION, table.gameOid);
                btn.label = ctx.xlate("game", key);
                box.addChild(btn);
                _seatsGrid.addChild(box);
            }
        }
    }

    /**
     * Update the displayed custom configuration options.
     */
    protected function updateConfig (table :MsoyTable) :void
    {
        while (_labelsBox.numChildren > 0) {
            _labelsBox.removeChild(_labelsBox.getChildAt(0));
        }
        _labelsBox.addChild(getConfigRow(ctx.xlate("game", "l.watchers"), String(_watcherCount)));

        var configXML :XML = panel.getGame().getGameDefinition().config;
        var customConfig :Object = null;
        if (table.config is EZGameConfig) {
            customConfig = (table.config as EZGameConfig).customConfig;
        }

        if (customConfig != null) {
            for each (var param :XML in configXML..params.children()) {
                if (StringUtil.isBlank(param.@ident)) {
                    continue;
                }
                var ident :String = String(param.@ident);
                var name :String = String(param.@name);
                var tip :String = String(param.@tip);
                if (StringUtil.isBlank(name)) {
                    name = ident;
                }

                var value :String = String(customConfig[name]);
                _labelsBox.addChild(getConfigRow(name, value, tip == "" ? name : tip, 
                    value.length > 5 ? value : ""));
            }
        }
    }

    protected function getConfigRow (name :String, value :String, nameTip :String = "", 
        valueTip :String = "") :UIComponent
    {
        var row :HBox = new HBox();
        row.setStyle("horizontalGap", 2);
        row.percentWidth = 100;
        row.height = 12;
        var lbl :Label = new Label();
        lbl.text = name + ":";
        lbl.styleName = "lobbyLabel";
        if (nameTip != "") {
            lbl.toolTip = nameTip;
        }
        lbl.width = 70;
        row.addChild(lbl);
        lbl = new Label();
        lbl.text = value;
        lbl.styleName = "lobbyLabel";
        if (valueTip != "") {
            lbl.toolTip = valueTip;
        }
        lbl.percentWidth = 100;
        row.addChild(lbl);
        return row;
    }

    protected static const CONFIG_WIDTH :int = 105;
    protected static const PADDING_WIDTH :int = 2;
    protected static const HORZ_GAP :int = 4;

    protected var _watcherCount :int;
    protected var _labelsBox :VBox;

    protected var _seatsGrid :Tile;

    protected var _game :Game;

    protected var _popup :Boolean; 

    protected var _maxUsableWidth :int;

    protected var _creationPanel :TableCreationPanel;
}
}

import mx.containers.HBox;

import mx.controls.Label;

import mx.core.UIComponent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.client.LobbyController;
import com.threerings.msoy.game.data.MsoyTable;

import com.threerings.msoy.web.data.MemberName;

class SeatRenderer extends HBox
{
    public function SeatRenderer () :void
    {
        styleName = "seatRenderer";
        percentWidth = 100;
        percentHeight = 100;
    }

    public function update (ctx :WorldContext, table :MsoyTable, index :int, 
        weAreSeated :Boolean) :void
    {
        _ctx = ctx;
        _table = table;
        _index = index;
        var occupant :MemberName = _table.occupants[_index] as MemberName;

        if (occupant != null) {
            prepareOccupant();
            _headShot.setMedia((_table.headShots[_index] as MediaDesc).getMediaPath());
            _name.text = occupant.toString();
            if (occupant.equals(_ctx.getMemberObject().memberName)) {
                _leaveBtn.setCommand(LobbyController.LEAVE_TABLE, _table.tableId);
                _leaveBtn.visible = _leaveBtn.includeInLayout = true;
            } else {
                _leaveBtn.visible = _leaveBtn.includeInLayout = false;
            }
            // TODO: add support for booting players from tables to the TableService, make it 
            // optional on TableManager creation, and support it here in the form of the closebox
        } else {
            prepareJoinButton();
            _joinBtn.enabled = !weAreSeated;
        }
    }

    protected function prepareOccupant () :void
    {
        if (_name == null || _name.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }
            addChild(new MediaWrapper(_headShot = new ScalingMediaContainer(40, 40), 40, 40));
            addChild(_name = new Label());
            _name.styleName = "nameLabel";
            addChild(_leaveBtn = new CommandButton());
            _leaveBtn.styleName = "closeButton";
        } 
        setStyle("horizontalAlign", "left");
    }

    protected function prepareJoinButton () :void
    {
        if (_joinBtn == null || _joinBtn.parent != this) {
            while (numChildren > 0) {
                removeChild(getChildAt(0));
            }
            if (_joinBtn == null) {
                _joinBtn = new CommandButton(LobbyController.JOIN_TABLE, 
                    [ _table.tableId, _index ]);
                _joinBtn.label = _ctx.xlate("game", "b.join");
            }
            addChild(_joinBtn);
        }
        setStyle("horizontalAlign", "center");
    }

    protected var _ctx :WorldContext;
    protected var _table :MsoyTable;
    protected var _index :int;

    protected var _joinBtn :CommandButton;
    protected var _headShot :ScalingMediaContainer;
    protected var _name :Label;
    protected var _leaveBtn :CommandButton;
}
