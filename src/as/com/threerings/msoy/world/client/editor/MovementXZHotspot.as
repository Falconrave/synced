//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.flash.MathUtil;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Hotspot that moves the target object along the Y axis.
 */
public class MovementXZHotspot extends Hotspot
{
    /** The hotspot won't go beyond this depth, even in advanced mode. */
    public static const MAX_DEPTH :int = 10;
    
    public function MovementXZHotspot (editor :FurniEditor)
    {
        super(editor);
    }

    // @Override from Hotspot
    override public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        super.updateDisplay(targetWidth, targetHeight);
        // show this hotspot on the floor
        this.x = targetWidth / 2;
        this.y = targetHeight + getStemHeight();
    }

    // @Override from Hotspot
    override protected function startAction (event :MouseEvent) :void
    {
        super.startAction(event);
        _originalLocation = _editor.target.getLocation().clone() as MsoyLocation;
        
        // make sure we're showing the proper action icon
        switchDisplay(event.shiftKey ? _displayYMouseOver : _displayMouseOver);
    }

    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);
        updateTargetLocation(event.stageX, event.stageY);
    }

    // @Override from Hotspot
    override protected function endAction (event :MouseEvent) :void
    {
        super.endAction(event);
        // switch back to the non-shifted action icon
        switchDisplay(_displayMouseOver);
    }

    // @Override from Hotspot
    override protected function initializeDisplay () :void
    {
        // do not call super - we're providing different bitmaps
        _displayStandard = new HOTSPOTXZ() as DisplayObject;
        _displayMouseOver = new HOTSPOTXZ_OVER() as DisplayObject;
        _displayYMouseOver = new HOTSPOTY_OVER() as DisplayObject;
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetLocation (sx :Number, sy :Number) :void
    {
        var loc :MsoyLocation = null;
        if (_currentDisplay == _displayYMouseOver) {
            sx -= (_anchor.x - _originalHotspot.x);
            sy -= (_anchor.y - _originalHotspot.y);
            var cloc :ClickLocation = _editor.roomView.layout.pointToFurniLocation(
                sx, sy, _editor.target.getLocation(), RoomMetrics.N_UP, false);
            loc = cloc.loc;

        } else {
            loc = _originalLocation.clone() as MsoyLocation;

            // get x and z location from mouse movement
            loc.x += (sx - _anchor.x) * MOUSE_GAIN.x;
            loc.z += (sy - _anchor.y) * MOUSE_GAIN.y;

            /*
            loc = _editor.roomView.layout.pointToLocationAtHeight(sx, sy, 0);
            if (loc != null) {
                // since click location is now on the floor, don't forget to restore stem height
                loc.y = _editor.target.getLocation().y;
            }
            */
        }

        // see if the user specified an invalid location, or one that's too far away
        if (loc == null || (_advancedMode && loc.z >= MAX_DEPTH)) {
            return; 
        }

        // in default mode, clamp to room boundaries
        if (! _advancedMode) {
            loc.x = MathUtil.clamp(loc.x, 0, 1);
            loc.z = MathUtil.clamp(loc.z, 0, 1);
        }
        
        _editor.updateTargetLocation(loc);
    }

    protected function getStemHeight () :Number
    {
        var oldloc :MsoyLocation = _editor.target.getLocation();
        return _editor.roomView.layout.metrics.roomDistanceToPixelDistance(
            new Point(0, oldloc.y), oldloc.z).y;
    }

    override protected function getToolTip () :String
    {
        return Msgs.EDITING.get("i.moving");
    }

    /** Mouse gain parameter for experimental position adjustment. */
    protected static const MOUSE_GAIN :Point = new Point(0.002, -0.005);
    
    /** Target location at the beginning of a move. */
    protected var _originalLocation :MsoyLocation;
    
    /** Bitmap used for hotspot with mouseover when shift pressed. */
    protected var _displayYMouseOver :DisplayObject;

    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_move_xz.png")]
    public static const HOTSPOTXZ :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_move_xz_over.png")]
    public static const HOTSPOTXZ_OVER :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_move_y_over.png")]
    public static const HOTSPOTY_OVER :Class;
}
}
