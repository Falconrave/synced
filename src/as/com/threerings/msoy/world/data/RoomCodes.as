//
// $Id$

package com.threerings.msoy.world.data {

/**
 * Codes and constants relating to the Room services.
 */
public class RoomCodes
{
    /** A message event type dispatched on the room object. */
    public static const SPRITE_MESSAGE :String = "sprMsg";

    /** An error (sort of) reported when an entity requests control but is already being controlled
     * by another client. */
    public static const E_ALREADY_CONTROLLED :String = "e.already_controlled";

    /** Error reported when the entity is denied entrance to a scene. */
    public static const E_ENTRANCE_DENIED :String = "e.entrance_denied";
    
    /** A room layer that is in front of normal furniture and such. */
    public static const FOREGROUND_EFFECT_LAYER :int = 0;

    /** The normal room layer where most things are placed. */
    public static const FURNITURE_LAYER :int = 1;

    /** A room layer that is behind of normal furniture, but in front of decor. */
    public static const BACKGROUND_EFFECT_LAYER :int = 2;

    /** The backmost layer, should only be occupied by decor objects. */
    public static const DECOR_LAYER :int = 3;

    /** Layout constant: normal layout. */
    public static const LAYOUT_NORMAL :int = 0;

    /** Layout constant: fill the visible room area. */
    public static const LAYOUT_FILL :int = 1;
}
}
