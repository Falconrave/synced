//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;


/**
 * Tracks the furni sprite that's currently being edited, and decorates it with draggable hotspots.
 */
public class FurniEditor extends FurniHighlight
{
    public function FurniEditor (controller :RoomEditorController)
    {
        super(controller);
    }

    // @Override from FurniHighlight
    override public function start () :void
    {
        super.start();

        _hotspots = new Array();
        _hotspots.push(new MovementWallHotspot(this));
        _hotspots.push(new ScalingHotspot(this));
        _hotspots.push(new MovementYHotspot(this));
        _hotspots.push(new MovementXZHotspot(this));

        for each (var hotspot :Hotspot in _hotspots) {
            _border.addChild(hotspot);
            hotspot.init();
        }

    }

    // @Override from FurniHighlight
    override public function end () :void
    {
        for each (var hotspot :Hotspot in _hotspots) {
            hotspot.deinit();
            _border.removeChild(hotspot);
        }            

        super.end();
    }

    // @Override from FurniHighlight
    override public function set target (sprite :FurniSprite) :void
    {
        super.target = sprite;
        _controller.updateDeleteStatus(_target != null);
    }

    /** Accessor to the room view. */
    public function get roomView () :RoomView
    {
        return _controller.roomView;
    }

    /** Returns true if no hotspot is active (i.e., currently being dragged). */
    public function isIdle () :Boolean
    {
        return _activeHotspot == null;
    }

    /** Called by hotspots, stores a reference to the currently active hotspot. */
    public function setActive (hotspot :Hotspot) :void
    {
        if (hotspot != null) {
            // we just started a new action - make a copy of the target's data
            _originalTargetData = target.getFurniData().clone() as FurniData;
        } else {
            // the action just finished - wrap up.
            if (_originalTargetData != null) {
                _controller.updateFurni(_originalTargetData, target.getFurniData());
                _originalTargetData = null;
            }
        }
        
        _activeHotspot = hotspot;
    }

    /** Called by hotspots, changes the target's display scale to the specified values. */
    public function updateTargetScale (x :Number, y :Number) :void
    {
        // update furni
        target.setMediaScaleX(x);
        target.setMediaScaleY(y);
        
        _controller.targetSpriteUpdated();
    }

    /** Called by hotspots, changes the target's display position. */
    public function updateTargetLocation (loc :MsoyLocation) :void
    {
        target.setLocation(loc);          // change position on screen...
        target.getFurniData().loc = loc;  // ...and in the data parameters
        
        _controller.targetSpriteUpdated();
    }

    // @Override from FurniHighlight
    override protected function clearBorder () :void
    {
        super.clearBorder();
        for each (var hotspot :Hotspot in _hotspots) {
            hotspot.visible = false;
        }
    }

    // @Override from FurniHighlight
    override protected function repaintBorder () :void
    {
        // note: do not call super - this is a complete replacement.
        // it paints the border and adjusts all hotspots.
        
        var g :Graphics = _border.graphics;
        var w :Number = target.getActualWidth();
        var h :Number = target.getActualHeight();
        var view :RoomView = _controller.roomView;

        g.clear();
        
        // compute location info for the stem from the current location to the floor

        // get target location in room and stage coordinates
        var roomLocation :MsoyLocation = target.getLocation();
        var stageLocation :Point = view.localToGlobal(view.layout.locationToPoint(roomLocation));
        var targetLocation :Point = target.globalToLocal(stageLocation);
        
        // get stem root location by dropping the target y value, and converting back to screen
        var roomRoot :MsoyLocation = new MsoyLocation(roomLocation.x, 0, roomLocation.z, 0);
        var stageRoot :Point = view.localToGlobal(view.layout.locationToPoint(roomRoot));
        var targetRoot :Point = target.globalToLocal(stageRoot); 

        // draw outer and inner outlines
        g.lineStyle(0, 0x000000, 0.5, true);
        g.drawRect(0, 0, w, h);
        g.drawRect(-2, -2, w + 4, h + 4);
        g.drawRect(targetRoot.x - 1, targetRoot.y, 2, (targetLocation.y - targetRoot.y) + 2);

        // draw center lines
        g.lineStyle(0, 0xffffff, 1, true);
        g.drawRect(-1, -1, w + 2, h + 2);
        g.moveTo(targetRoot.x, targetRoot.y);
        g.lineTo(targetLocation.x, targetLocation.y + 1);

        // reset position, so that subsequent fills don't hallucinate that a curve was
        // left open, and needs to be filled in. (you'd think that curves defined *before*
        // a call to beginFill would get ignored, but you'd be wrong.)
        g.moveTo(0, 0);

        // now update hotspot positions
        for each (var hotspot :Hotspot in _hotspots) {
            hotspot.visible = (_target != null);
            hotspot.updateDisplay(w, h);
        }
    }
    
    /** Copy of the target's original furni data, created when the user activates a hotspot. */
    protected var _originalTargetData :FurniData;
    
    /** Reference to the currently active hotspot. */
    protected var _activeHotspot :Hotspot;

    /** Array of all Hotspot instances (initialized in the constructor). */
    protected var _hotspots :Array; // of Hotspot references
}
}
