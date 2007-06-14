//
// $Id$

package com.threerings.msoy.world.client.layout {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.flash.Vector3;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.world.client.AbstractRoomView;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.RoomElement;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomCodes;


/**
 * This class factors out room layout math that converts between 3D room coordinate space
 * and screen coordinates.
 */
public class RoomLayoutStandard implements RoomLayout
{
    /** Chat history should be at least 50px tall, even in rooms with low horizon. */
    public static const MIN_CHAT_HEIGHT :Number = 50;

    /** Constructor. */
    public function RoomLayoutStandard (view :AbstractRoomView)
    {
        _parentView = view;
        _metrics = new RoomMetrics();
    }

    // from interface RoomLayout
    public function update (data :Decor) :void
    {
        _metrics.update(data);
    }
    
    // from interface RoomLayout
    public function get metrics () :RoomMetrics
    {
        return _metrics;
    }
    
    // from interface RoomLayout
    public function pointToAvatarLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation
    {
        var loc :ClickLocation = pointToLocation(stageX, stageY, anchorPoint, anchorAxis);
        return (loc.click == ClickLocation.FLOOR) ? loc : null;
    }
    
    // from interface RoomLayout
    public function pointToFurniLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation
    {
        return pointToLocation(stageX, stageY, anchorPoint, anchorAxis);
    }

    // from interface RoomLayout
    public function pointToLocationAtDepth (
        stageX :Number, stageY :Number, depth :Number) :MsoyLocation
    {
        // get click location, in screen coords
        var p :Point = new Point(stageX, stageY);
        p = _parentView.globalToLocal(p);

        // let's make an anchor point
        var anchor :Vector3 = new Vector3(0, 0, depth);

        // find the intersection of the line of sight with the plane
        var cloc :ClickLocation = _metrics.screenToPlaneProjection(p.x, p.y, anchor);
        
        return (cloc != null) ? cloc.loc : null;
    }
    
    /**
     * Turn the screen coordinate into an MsoyLocation, with the orient field set to 0.
     */
    protected function pointToLocation (
        globalX :Number, globalY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation
    {
        // get click location, in screen coords
        var p :Point = new Point(globalX, globalY);
        p = _parentView.globalToLocal(p);

        var cloc :ClickLocation;
        if (anchorPoint == null) {

            // find the intersection of the line of sight with the first available wall.
            cloc = _metrics.screenToInnerWallsProjection(p.x, p.y);
            
        } else {
            var anchorLocation :Vector3;

            if (anchorPoint is Point) {
                // the constraint is a point on screen - convert it
                var pp :Point = _parentView.globalToLocal(anchorPoint as Point);
                var constLocation :ClickLocation =
                    _metrics.screenToInnerWallsProjection(pp.x, pp.y);
                
                anchorLocation = _metrics.toVector3(constLocation.loc);

            } else if (anchorPoint is MsoyLocation) {
                // the constraint is a room location - we're ready to go!
                anchorLocation = _metrics.toVector3(anchorPoint as MsoyLocation);

            } else {
                throw new ArgumentError("Invalid constraint argument type");
            }

            // which constraint operation are we using?
            var fn :Function = null;
            switch (anchorAxis) {
            case RoomMetrics.N_RIGHT: fn = _metrics.screenToXLineProjection; break;
            case RoomMetrics.N_UP:    fn = _metrics.screenToYLineProjection; break;
            case RoomMetrics.N_AWAY:  fn = _metrics.screenToZLineProjection; break;
            default:
                throw new ArgumentError("Unsupported anchorAxis type: " + anchorAxis);
            }
            
            // now find a point on the constraint line pointed to by the mouse
            var yLocation :Vector3 = fn(p.x, p.y, anchorLocation).clampToUnitBox();

            // we're done - make a fake "floor" location
            cloc = new ClickLocation(ClickLocation.FLOOR, _metrics.toMsoyLocation(yLocation));
        }

        clampClickLocation(cloc);
        
        return cloc;
    }

    /** Clamps a click location to be inside the unit box. */
    protected function clampClickLocation (cloc :ClickLocation) :void
    {
        cloc.loc.x = Math.min(Math.max(cloc.loc.x, 0), 1);
        cloc.loc.y = Math.min(Math.max(cloc.loc.y, 0), 1);
        cloc.loc.z = Math.min(Math.max(cloc.loc.z, 0), 1);
    }

    
    // Perspectivization
    // Disabled for now, until we settle on new room layout logic
    // getPerspInfo comes from FurniSprite, checkPerspective() and updatePerspective()
    
    /**
     * Calculate the info needed to perspectivize a piece of furni.
     */

    /*
    public function getPerspInfo (
        sprite :MsoySprite, contentWidth :int, contentHeight :int,
        loc :MsoyLocation) :PerspInfo
    {
        var hotSpot :Point = sprite.getMediaHotSpot();
        var mediaScaleX :Number = Math.abs(sprite.getMediaScaleX());
        var mediaScaleY :Number = Math.abs(sprite.getMediaScaleY());

        // below, 0 refers to the right side of the source sprite
        // N refers to the left side, and H refers to the location
        // of the hotspot

        // the scale of the object is determined by the z coordinate
        var distH :Number = _metrics.focal + (_metrics.sceneDepth * loc.z);
        var dist0 :Number = (hotSpot.x * mediaScaleX);
        var distN :Number = (contentWidth - hotSpot.x) * mediaScaleX;
        if (loc.x < .5) {
            dist0 *= -1;
        } else {
            distN *= -1;
        }

        var scale0 :Number = _metrics.focal / (distH + dist0);
        var scaleH :Number = _metrics.focal / distH;
        var scaleN :Number = _metrics.focal / (distH + distN);

        var logicalY :Number = loc.y + ((contentHeight * mediaScaleY) / _metrics.sceneHeight);

        var p0 :Point = projectedLocation(scale0, loc.x, logicalY);
        var pH :Point = projectedLocation(scaleH, loc.x, loc.y);
        var pN :Point = projectedLocation(scaleN, loc.x, logicalY);

        var height0 :Number = contentHeight * scale0 * mediaScaleY;
        var heightN :Number = contentHeight * scaleN * mediaScaleY;

        // min/max don't account for the hotspot location
        var minX :Number = Math.min(p0.x, pN.x);
        var minY :Number = Math.min(p0.y, pN.y);
        p0.offset(-minX, -minY);
        pN.offset(-minX, -minY);
        pH.offset(-minX, -minY);

        return new PerspInfo(p0, height0, pN, heightN, pH);
    }
    */


    /**
     * Determine the location of the projected coordinate.
     *
     * @param x the logical x coordinate (0 - 1)
     * @param y the logical y coordinate (0 - 1)
     */
    // TODO: deprecate, fix perspectivization, use the _metrics version
    // of these methods
    /*
    protected function projectedLocation (
        scale :Number, x :Number, y :Number) :Point
    {
        // x position depends on logical x and the scale
        var floorWidth :Number = (_metrics.sceneWidth * scale);
        var floorInset :Number = (_metrics.sceneWidth - floorWidth) / 2;

        var horizonY :Number = _metrics.sceneHeight * (1 - _metrics.sceneHorizon);
        return new Point(floorInset + (x * floorWidth),
                         horizonY + ((_metrics.sceneHeight - horizonY) -
                                     (y * _metrics.sceneHeight)) * scale);
    }
    */

    // from interface RoomLayout
    public function locationToPoint (location :MsoyLocation) :Point
    {
        return _metrics.roomToScreen(location.x, location.y, location.z);
    }

    // from interface RoomLayout
    public function recommendedChatHeight () :Number
    {
        return Math.min(MIN_CHAT_HEIGHT,
                        _metrics.roomToScreen(0, RoomMetrics.LEFT_BOTTOM_FAR.y, 0).y);
    }

    // from interface RoomLayout
    public function updateScreenLocation (target :RoomElement, offset :Point = null) :void
    {
        if (!(target is DisplayObject)) {
            throw new ArgumentError("Invalid target passed to updateScreenLocation: " + target);
        }

        switch (target.getLayoutType()) {
        default:
            Log.getLog(this).warning("Unknown layout type: " + target.getLayoutType() +
                ", falling back to LAYOUT_NORMAL.");
            // fall through to LAYOUT_NORMAL

        case RoomCodes.LAYOUT_NORMAL:
            var loc :MsoyLocation = target.getLocation();
            var screen :Point = _metrics.roomToScreen(loc.x, loc.y, loc.z);
            var scale :Number = _metrics.scaleAtDepth(loc.z);
            offset = (offset != null ? offset : NO_OFFSET);
            target.setScreenLocation(screen.x - offset.x, screen.y - offset.y, scale);
            break;

        case RoomCodes.LAYOUT_FILL:
            var disp :DisplayObject = (target as DisplayObject);
            disp.x = 0;
            disp.y = 0;
            var r :Rectangle = _parentView.getScrollBounds();
            disp.width = r.width;
            disp.height = r.height;
            break;
        }

        adjustZOrder(target as DisplayObject);
    }


    /**
     * Adjust the z order of the specified sprite so that it is drawn according to its logical Z
     * coordinate relative to other sprites.
     */
    protected function adjustZOrder (sprite :DisplayObject) :void
    {
        var dex :int;
        try {
            dex = _parentView.getChildIndex(sprite);
        } catch (er :Error) {
            // this can happen if we're resized during editing as we
            // try to reposition a sprite that's still in our data structures
            // but that has been removed as a child.
            return;
        }

        var newdex :int = dex;
        var ourZ :Number = getZOfChildAt(dex);
        var z :Number;
        while (newdex > 0) {
            z = getZOfChildAt(newdex - 1);
            if (z >= ourZ) {
                break;
            }
            newdex--;
        }

        if (newdex == dex) {
            while (newdex < _parentView.numChildren - 1) {
                z = getZOfChildAt(newdex + 1);
                if (z <= ourZ) {
                    break;
                }
                newdex++;
            }
        }

        if (newdex != dex) {
            _parentView.setChildIndex(sprite, newdex);
        }
    }

    /**
     * Convenience method to get the logical z coordinate of the child at the specified index.
     * Returns the z ordering, or NaN if the object is not part of the scene layout.
     */
    protected function getZOfChildAt (index :int) :Number
    {
        var re :RoomElement = _parentView.getChildAt(index) as RoomElement;

        if (re != null) {
            // we multiply the layer constant by 1000 to spread out the z values that
            // normally lie in the 0 -> 1 range.
            return re.getLocation().z + (1000 * re.getRoomLayer());
        } else {
            return NaN;
        }
    }

    /** Room metrics storage. */
    protected var _metrics :RoomMetrics;

    /** AbstractRoomView object that contains this instance. */
    protected var _parentView :AbstractRoomView;

    /** Point (0, 0) expressed as a constant. */
    protected static const NO_OFFSET :Point = new Point(0, 0);
}
}
    
