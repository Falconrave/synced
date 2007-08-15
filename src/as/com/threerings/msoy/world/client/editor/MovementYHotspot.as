//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Hotspot that moves the target object along the Y axis.
 */
public class MovementYHotspot extends Hotspot
{
    public function MovementYHotspot (editor :FurniEditor)
    {
        super(editor);
    }

    // @Override from Hotspot
    override public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        super.updateDisplay(targetWidth, targetHeight);
        
        this.x = targetWidth / 2;
        this.y = targetHeight - _displayStandard.height;
    }
    
    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);

        updateTargetLocation(event.stageX, event.stageY);
    }

    // @Override from Hotspot
    override protected function initializeDisplay () :void
    {
        // do not call super - we're providing different bitmaps
        _displayStandard = new HOTSPOT() as DisplayObject;
        _displayMouseOver = new HOTSPOT_OVER() as DisplayObject;
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetLocation (sx :Number, sy :Number) :void
    {
        sx -= (_anchor.x - _originalHotspot.x);
        sy -= (_anchor.y - _originalHotspot.y);

        var cloc :ClickLocation = _editor.roomView.layout.pointToFurniLocation(
            sx, sy, _editor.target.getLocation(), RoomMetrics.N_UP, false);

        _editor.updateTargetLocation(cloc.loc);
    }

    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_move_y.png")]
    public static const HOTSPOT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_move_y_over.png")]
    public static const HOTSPOT_OVER :Class;
}
}
