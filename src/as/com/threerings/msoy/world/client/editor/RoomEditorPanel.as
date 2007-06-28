//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.IEventDispatcher;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.containers.HBox;
import mx.containers.TabNavigator;
import mx.containers.VBox;
import mx.controls.Button;
import mx.controls.HRule;
import mx.controls.Label;
import mx.controls.Spacer;

import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.ui.FloatingPanel;


/**
 * A separate room editing panel, which lets the player edit furniture inside the room.
 */
public class RoomEditorPanel extends FloatingPanel
{
    public function RoomEditorPanel (ctx :WorldContext, controller :RoomEditorController)
    {
        super(ctx, Msgs.EDITING.get("t.editor_title"));
        _controller = controller;

        styleName = "roomEditPanel";
        showCloseButton = true;
    }

    // @Override from FloatingPanel
    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, avoid :DisplayObject = null) :void
    {
        super.open(modal, parent, avoid);

        this.x = TopPanel.DECORATIVE_MARGIN_HEIGHT;
        this.y = HeaderBar.HEIGHT + TopPanel.DECORATIVE_MARGIN_HEIGHT;
    }

    // @Override from FloatingPanel
    override public function close () :void
    {
        super.close();
        _controller.actionEditorClosed();
    }

    /** Updates object data displayed on the editing panel. */
    public function updateDisplay (data :FurniData) :void
    {
        _details.updateDisplay(data);
        _action.updateDisplay(data);
        _room.updateDisplay(data);
    }

    /** Updates object name for display in the window title bar. */
    public function updateName (name :String) :void
    {
        if (_namelabel != null) {
            _namelabel.text = (name != null) ? name : "";
        }
    }
    
    /** Updates the enabled status of the undo button (based on the size of the undo stack). */
    public function updateUndoStatus (enabled :Boolean) :void
    {
        _undoButton.enabled = enabled;
    }

    /** Updates the enabled status of the delete button (based on current selection). */
    public function updateDeleteStatus (enabled :Boolean) :void
    {
        if (_deleteButton != null) { // just in case this gets called during initialization...
            _deleteButton.enabled = enabled;
        }
    }
    
    // from superclasses
    override protected function createChildren () :void
    {
        super.createChildren();

        var makeListener :Function = function (thunk :Function) :Function {
            return function (event :Event) :void { thunk(); };
        };
        
        // container for room name
        var namebar :VBox = new VBox();
        namebar.styleName = "roomEditNameBar";
        namebar.percentWidth = 100;
        namebar.addChild(_room = new RoomPanel(_controller));
        addChild(namebar);

        // container for everything else
        var contents :VBox = new VBox();
        contents.styleName = "roomEditContents";
        contents.percentWidth = 100;
        addChild(contents);
        
        // sub-container for object name and buttons
        var box :HBox = new HBox();
        box.styleName = "roomEditButtonBar";
        box.percentWidth = 100;
        contents.addChild(box);

        _namelabel = new Label();
        _namelabel.styleName = "roomEditNameLabel";
        _namelabel.percentWidth = 100;
        box.addChild(_namelabel);
        
        _deleteButton = new Button();
        _deleteButton.styleName = "roomEditButtonTrash3";
        _deleteButton.toolTip = Msgs.EDITING.get("i.delete_button");
        _deleteButton.enabled = false;
        _deleteButton.addEventListener(MouseEvent.CLICK, makeListener(_controller.actionDelete));
        box.addChild(_deleteButton);
        
        _undoButton = new Button();
        _undoButton.styleName = "roomEditButtonUndo3";
        _undoButton.toolTip = Msgs.EDITING.get("i.undo_button");
        _undoButton.enabled = false;
        _undoButton.addEventListener(MouseEvent.CLICK, makeListener(_controller.actionUndo));
        box.addChild(_undoButton);

        hr = new HRule();
        hr.percentWidth = 100;
        contents.addChild(hr);

        // now create collapsing sections
        var c :CollapsingContainer = new CollapsingContainer(Msgs.EDITING.get("t.item_prefs"));
        c.setContents(_details = new DetailsPanel(_controller));
        contents.addChild(c);

        var hr :HRule = new HRule();
        hr.percentWidth = 100;
        contents.addChild(hr);

        c = new CollapsingContainer(Msgs.EDITING.get("t.item_action"));
        c.setContents(_action = new ActionPanel(_controller)); 
        contents.addChild(c);
    }

    protected var _deleteButton :Button;
    protected var _undoButton :Button;
    protected var _details :DetailsPanel;
    protected var _action :ActionPanel;
    protected var _room :RoomPanel;
    protected var _namelabel :Label;
    protected var _controller :RoomEditorController;
}
}
