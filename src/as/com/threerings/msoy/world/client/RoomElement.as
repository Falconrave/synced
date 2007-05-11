package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Interface for all objects that exist in a scene, and have both scene location in room
 * coordinate space, and screen location that needs to be updated appropriately.
 */ 
public interface RoomElement
{
    /**
     * Return the type of layout to do for this element.
     * 
     * @return probably RoomCodes.LAYOUT_NORMAL.
     */
    function getLayoutType () :int

    /**
     * Return the layer upon which this element should be layed out.
     * 
     * @return probably RoomCodes.FURNITURE_LAYER.
     */
    function getRoomLayer () :int;

    /**
     * Set the logical location of the element. The orientation is not updated.
     * @param newLoc may be an MsoyLocation or an Array.
     */
    function setLocation (newLoc :Object) :void

    /**
     * Get the logical location of this object.
     */
    function getLocation () :MsoyLocation;

    /**
     * Set the screen location of the object, based on its location in the scene.
     */
    function setScreenLocation (x :Number, y :Number, scale :Number) :void;
}
}
