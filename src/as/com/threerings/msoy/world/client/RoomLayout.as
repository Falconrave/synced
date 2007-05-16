//
// $Id$

package com.threerings.msoy.world.client {

import flash.geom.Point;
    
import com.threerings.flash.Vector3;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.MsoyLocation;


/**
 * Interface for classes that encapsulate different kinds of layout math for rooms.
 */
public interface RoomLayout {

    /**
     * Updates the room layout object with fresh data.
     */
    function update (data :DecorData) :void;

    /**
     * Get the room metrics used in the layout.
     */
    function get metrics () :RoomMetrics;

    /**
     * Finds screen position (in coordinates of the room view) of the specified room location,
     * as projected onto the screen using current room layout.
     *
     *   @param location     MsoyLocation object that specifies room location
     *
     * @returns A Point with a screen position in stage coordinate system.
     */
    function locationToPoint (location :MsoyLocation) :Point;
    
    /**
     * Finds a room location for avatar movement, based on a screen position.
     *
     * Movement locations can be constrained in particular ways; for example, only allowing
     * movement on the floor or the ceiling.
     *
     *   @param stageX       Mouse x position, in stage coordinate space
     *   @param stageY       Mouse y position, in stage coordinate space
     *   @param anchorPoint  If present, constraints movement to an axis-aligned line passing
     *                       through the anchorPoint. The anchor can either be a Point
     *                       containing screen (stage) coordinates, or an MsoyLocation.
     *   @param anchorAxis   The axis of the constraint line, as one of the following constants:
     *                       RoomMetrics.{N_UP|N_RIGHT|N_AWAY}. This parameter is only
     *                       used in conjunction with anchorPoint.
     *
     * @returns A ClickLocation valid for this room layout, or null if no valid location was found.
     *
     */
    function pointToAvatarLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation;
    
    /**
     * Finds a room location for furni placement, based on a screen position.
     *
     * Furni locations are not usually constrained in the same way as avatars can be,
     * so this function returns a more straightforward placement than the Avatar version.
     *
     *   @param stageX       Mouse x position, in stage coordinate space
     *   @param stageY       Mouse y position, in stage coordinate space
     *   @param anchorPoint  If present, constraints movement to an axis-aligned line passing
     *                       through the anchorPoint. The anchor can either be a Point
     *                       containing screen (stage) coordinates, or an MsoyLocation.
     *   @param anchorAxis   The axis of the constraint line, as one of the following constants:
     *                       RoomMetrics.{N_UP|N_RIGHT|N_AWAY}. This parameter is only
     *                       used in conjunction with anchorPoint.
     *
     * @returns A ClickLocation valid for this room layout, or null if no valid location was found.
     *
     */
    function pointToFurniLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation;
    
    /**
     * Finds the projection of mouse coordinates onto a plane in the room, parallel with the
     * front wall, intersecting the room at specified depth. This type of functionality is useful
     * for converting mouse position into room position at some constant depth.
     *
     *   @param stageX       Mouse x position, in stage coordinate space
     *   @param stageY       Mouse y position, in stage coordinate space
     *   @param depth        Z position of the intersection wall, in room coordinate space.
     *
     * @returns An MsoyLocation for this intersection (with z value equal to depth), or null
     * if no valid location was found.
     *
     */
    function pointToLocationAtDepth (stageX :Number, stageY :Number, depth :Number) :MsoyLocation;
    
    /**
     * Given a position in room space, this function finds its projection in screen space, and
     * updates the DisplayObject's position and scale appropriately. If the display object
     * participates in screen layout (and most of them do, with the notable exception of decor),
     * it will also ask the room view to recalculate the object's z-ordering.
     *
     * @param target object to be updated
     * @param offset optional Point argument that, if not null, will be used to shift
     *        the object left and up by the specified x and y amounts.
     */
    function updateScreenLocation (target :RoomElement, offset :Point = null) :void;

    /**
     * Finds a recommended height of the chat overlay, in room units.
     */
    function recommendedChatHeight () :Number;

}
}
