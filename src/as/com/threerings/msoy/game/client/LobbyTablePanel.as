//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;

import com.whirled.game.data.WhirledGameConfig;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.Text;
import mx.controls.TextInput;
import mx.core.ScrollPolicy;

import com.threerings.util.StringUtil;
import com.threerings.util.Util;

import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Parameter;
import com.threerings.parlor.data.Table;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyTableConfig;
import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.SimpleGrid;

/**
 * Displays information on a table that is currently being matchmade.
 */
public class LobbyTablePanel extends VBox
    implements TableObserver
{
    /** The number of seat columns we display. */
    public static const SEAT_COLS :int = 3;

    public function LobbyTablePanel (gctx :GameContext, ctrl :LobbyController, lobj :LobbyObject)
    {
        _gctx = gctx;
        _ctrl = ctrl;
        _lobj = lobj;
        _table = _ctrl.tableDir.getSeatedTable();

        styleName = "lobbyTablePanel";
        setStyle("verticalGap", 10); // if I set this in the stylesheet, it's ignored, yay!
        percentWidth = 100;
        percentHeight = 100;
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        // nothing doing
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        // if this is our seated table, update mini-table
        if (table.tableId == _table.tableId) {
            update(table, true);
        }
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        // nothing doing
    }

    // from VBox
    override public function parentChanged (parent :DisplayObjectContainer) :void
    {
        super.parentChanged(parent);

        if (parent == null) {
            _ctrl.tableDir.removeTableObserver(this);
        } else {
            _ctrl.tableDir.addTableObserver(this);
        }
    }

    // from VBox
    override protected function createChildren () :void
    {
        super.createChildren();

        // display our table name and configuration
        _title = FlexUtil.createLabel("", "lobbyTitle");
        _info = new Text();
        _info.percentWidth = 100;
        addChild(makeVBox(_title, _info));

        const memId :int = _gctx.getMyId(); // ok if guest

        var shareLink :String = _gctx.getWorldContext().getMsoyController().createSharableLink(
            "world-game_i_" + _lobj.game.gameId + "_" + memId, true);

        // display our invite verbiage
        var ititle :Label = FlexUtil.createLabel(Msgs.GAME.get("t.invite_link"), "lobbyTitle");
        var isubtitle :Label = FlexUtil.createLabel(Msgs.GAME.get("l.invite_link"));
        var ilink :TextInput = new TextInput();
        ilink.text = shareLink;
        addChild(makeVBox(ititle, isubtitle, new CopyableText(ilink)));

        // twitter an invite
        var tweetLbl :Label = FlexUtil.createLabel(Msgs.GAME.get("l.invite_twitter"));
        var tweetBtn :CommandButton = new CommandButton(Msgs.GAME.get("b.invite_twitter"),
            _gctx.getWorldContext().getMsoyController().handleTweetGame,
            [ _lobj.game.gameId, _lobj.game.name, false ]);
        tweetBtn.styleName = "orangeButton";
        addChild(makeHBox(tweetLbl, tweetBtn));

        // create our seats grid
        addChild(_seatsGrid = new SimpleGrid(SEAT_COLS))
        _seatsGrid.styleName = "seatsGrid";
        _seatsGrid.percentWidth = 100;
        _seatsGrid.percentHeight = 100;
        _seatsGrid.verticalScrollPolicy = ScrollPolicy.OFF;
        _seatsGrid.horizontalScrollPolicy = ScrollPolicy.OFF;
        for (var ii :int = 0; ii < _table.players.length; ii++) {
            _seatsGrid.addCell(new SeatPanel());
        }

        // add our control buttons along the bottom
        var bbox :HBox = new HBox();
        bbox.percentWidth = 100;
        bbox.setStyle("horizontalAlign", "right");
        bbox.addChild(new CommandButton(Msgs.GAME.get("b.leave"),
            LobbyController.LEAVE_TABLE, _table.tableId))

        // if we are the creator, add a button for starting the game now
        if (_table.players.length > 0 &&
                _gctx.getMyName().equals(_table.players[0]) &&
                (_table.tconfig.desiredPlayerCount > _table.tconfig.minimumPlayerCount)) {
            _startBtn = new CommandButton(
                Msgs.GAME.get("b.start_now"), LobbyController.START_TABLE, _table.tableId);
            bbox.addChild(_startBtn);
        }
        addChild(bbox);

        update(_table, _ctrl.isSeated());
    }

    protected function makeVBox (... children) :VBox
    {
        var box :VBox = new VBox();
        box.percentWidth = 100;
        box.setStyle("verticalGap", 0);
        children.forEach(Util.adapt(box.addChild));
        return box;
    }

    protected function makeHBox (... children) :HBox
    {
        var box :HBox = new HBox();
        box.percentWidth = 100;
        children.forEach(Util.adapt(box.addChild));
        return box;
    }

    protected function update (table :Table, isSeated :Boolean) :void
    {
        // keep this up to date just for kicks
        _table = table;

        if (_startBtn != null) {
            _startBtn.enabled = table.mayBeStarted();
        }
        for (var ii :int = 0; ii < table.players.length; ii++) {
            (_seatsGrid.getCellAt(ii) as SeatPanel).update(_gctx, table, ii, isSeated);
        }

        // display whether this game is rated
        var info :String = Msgs.GAME.get(table.config.rated ? "l.is_rated" : "l.not_rated");

        // display the non-players in the room (or everyone for party games)
        if (table.watchers != null && table.watchers.length > 0) {
            info = info + ", " + Msgs.GAME.get("l.people") + ": " + table.watchers.join(", ");
        }

        // and display any custom table configuration
        if (table.config is WhirledGameConfig) {
            var gconfig :WhirledGameConfig = (table.config as WhirledGameConfig);
            var params :Array = gconfig.getGameDefinition().params;
            if (params != null) {
                for each (var param :Parameter in params) {
                    var name :String = StringUtil.isBlank(param.name) ? param.ident : param.name;
                    var value :String = String(gconfig.params.get(param.ident));
                    info = info + ", " + name + ": "+ value;
                }
            }
        }

        const tableConfig :MsoyTableConfig = (table.tconfig as MsoyTableConfig);
        _title.text = (tableConfig != null) ? tableConfig.title : "";
        _info.text = info;
    }

    protected var _gctx :GameContext;
    protected var _ctrl :LobbyController;
    protected var _lobj :LobbyObject;
    protected var _table :Table;

    protected var _title :Label;
    protected var _info :Text;
    protected var _seatsGrid :SimpleGrid;
    protected var _startBtn :CommandButton;
}
}

import mx.containers.Canvas;
import mx.containers.VBox;
import mx.controls.Label;

import com.threerings.parlor.data.Table;

import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.ui.MediaWrapper;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.client.LobbyController;
import com.threerings.msoy.game.client.LobbyPanel;
import com.threerings.msoy.game.client.LobbyTablePanel;
import com.threerings.msoy.item.data.all.Item;

class SeatPanel extends VBox
{
    public function SeatPanel () :void
    {
        styleName = "seatPanel";
    }

    public function update (ctx :GameContext, table :Table, index :int, areSeated :Boolean) :void
    {
        var player :VizMemberName = (table.players[index] as VizMemberName);
        if (player == null) {
            _headShot.setMediaDesc(Item.getDefaultThumbnailMediaFor(Item.AVATAR));
            _headShot.alpha = 0.5;
            _name.text = "waiting..."; // TODO
            FlexUtil.setVisible(_bootBtn, false);

        } else {
            _headShot.setMediaDesc(player.getPhoto());
            _headShot.alpha = 1;
            _name.text = player.toString();
            FlexUtil.setVisible(_bootBtn, ctx.getMyName().equals(table.players[0]));
            _bootBtn.setCommand(LobbyController.BOOT_PLAYER, [ table.tableId, player ]);
        }
    }

    // from VBox
    override protected function createChildren () :void
    {
        super.createChildren();

        var box :Canvas = new Canvas();
        box.addChild(_headShot = MediaWrapper.createView(null, MediaDescSize.THUMBNAIL_SIZE));
        box.addChild(_bootBtn = new CommandButton());
        _bootBtn.styleName = "closeButton";
        _bootBtn.x = MediaDescSize.THUMBNAIL_WIDTH-13;
        _bootBtn.y = 0;
        addChild(box);

        addChild(_name = FlexUtil.createLabel("", "nameLabel"));
        // let the name be a smidgen wider because we have more room than is needed to just
        // display the thumbnails
        _name.width = MediaDescSize.THUMBNAIL_WIDTH +
            SEAT_GRID_WHITESPACE / LobbyTablePanel.SEAT_COLS;
        _name.setStyle("textAlign", "center");
    }

    protected var _headShot :MediaWrapper;
    protected var _name :Label;
    protected var _bootBtn :CommandButton;

    protected static const SEAT_GRID_WHITESPACE :int =
        LobbyPanel.WIDTH - (26+10) /* borders */ -
        MediaDescSize.THUMBNAIL_WIDTH * LobbyTablePanel.SEAT_COLS -
        5 * (LobbyTablePanel.SEAT_COLS-1) /* gaps */;
}
