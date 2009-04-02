//
// $Id$

package com.threerings.msoy.party.client {

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Spacer;
import mx.controls.TextInput;
import mx.events.FlexEvent;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandComboBox;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.PlayerList;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.party.data.PartyCodes;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;

public class PartyPanel extends FloatingPanel
    implements AttributeChangeListener
{
    public function PartyPanel (ctx :WorldContext, partyObj :PartyObject)
    {
        super(ctx, partyObj.name);
        _wctx = ctx;
        showCloseButton = true;

        _partyObj = partyObj;
    }

    override public function close () :void
    {
        _roster.shutdown();
        _partyObj.removeListener(this);

        super.close();
    }

    override protected function didOpen () :void
    {
        super.didOpen();

        _roster.init(_partyObj, PartyObject.PEEPS, PartyObject.LEADER_ID);
        _partyObj.addListener(this);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var isLeader :Boolean = (_partyObj.leaderId == _ctx.getMyId());

        _roster = new PlayerList(
            PeepRenderer.createFactory(_wctx, _partyObj), PartyPeep.createSortByOrder(_partyObj));
        addChild(_roster);

        var sep :VBox = new VBox();
        sep.percentWidth = 100;
        sep.height = 1;
        sep.styleName = "panelBottomSeparator";
        addChild(sep);

        var box :VBox = new VBox();
        box.percentWidth = 100;
        box.styleName = "panelBottom";

        _status = new TextInput();
        _status.styleName = "partyStatus";
        _status.maxChars = PartyCodes.MAX_NAME_LENGTH;
        _status.percentWidth = 100;
        _status.text = Msgs.PARTY.xlate(_partyObj.status);
        _status.enabled = isLeader;
        _status.addEventListener(FlexEvent.ENTER, commitStatus);
        box.addChild(_status);

        var hbox :HBox = new HBox();
        hbox.percentWidth = 100;

        var options :Array = [];
        for (var ii :int = 0; ii < PartyCodes.RECRUITMENT_COUNT; ii++) {
            options.push({ label: Msgs.PARTY.get("l.recruit_" + ii), data: ii });
        }
        _recruit = new CommandComboBox(_wctx.getPartyDirector().updateRecruitment);
        _recruit.dataProvider = options;
        _recruit.selectedData = _partyObj.recruitment;
        _recruit.enabled = isLeader;
        hbox.addChild(_recruit);

        var spacer :Spacer = new Spacer();
        spacer.percentWidth = 100;
        hbox.addChild(spacer);

        hbox.addChild(
            new CommandButton(Msgs.PARTY.get("b.leave"), _wctx.getPartyDirector().clearParty));
        box.addChild(hbox);
        addChild(box);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
            case PartyObject.STATUS:
                _status.text = Msgs.PARTY.xlate(String(event.getValue()));
                break;

            case PartyObject.LEADER_ID:
                var isLeader :Boolean = (event.getValue() == _ctx.getMyId());
                _status.enabled = isLeader;
                _recruit.enabled = isLeader;
                break;

            case PartyObject.RECRUITMENT:
                _recruit.selectedData = int(event.getValue());
                break;
        }
    }

    protected function commitStatus (event :FlexEvent) :void
    {
        _wctx.getPartyDirector().updateStatus(_status.text);
        _wctx.getControlBar().giveChatFocus();
    }

    protected var _wctx :WorldContext;

    protected var _partyObj :PartyObject;

    protected var _roster :PlayerList;
    protected var _status :TextInput;
    protected var _recruit :CommandComboBox;
}
}
