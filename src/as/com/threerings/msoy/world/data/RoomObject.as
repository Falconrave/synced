//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.presents.dobj.DSet;

import com.threerings.whirled.spot.data.SpotSceneObject;

/**
 * Contains the distributed state of a virtual world room.
 */
public class RoomObject extends SpotSceneObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>roomService</code> field. */
    public static const ROOM_SERVICE :String = "roomService";

    /** The field name of the <code>memories</code> field. */
    public static const MEMORIES :String = "memories";

    /** The field name of the <code>controllers</code> field. */
    public static const CONTROLLERS :String = "controllers";

    /** The field name of the <code>effects</code> field. */
    public static const EFFECTS :String = "effects";
    // AUTO-GENERATED: FIELDS END

    /** A message sent by the server to have occupants load, but not play,
     * the specified music.
     * Format: [ url ].  */
    public static const LOAD_MUSIC :String = "loadMusic";

    /** A corresponding message sent by each client when they've got the music
     * completely loaded. No other status is needed.
     * Format: [ url ]. */
    public static const MUSIC_LOADED :String = "musicLoaded";

    /** The message sent by the server to kick-off music playing. The music
     * should be played once and then disposed-of. No action
     * should be taken if the music was not loaded. 
     * Format: [ url ]. */
    public static const PLAY_MUSIC :String = "playMusic";

    /** A message sent by each client to indicate that the music has
     * finished playing.
     * Format: [ url ]. */
    public static const MUSIC_ENDED :String = "musicEnded";

    /** Our room service marshaller. */
    public var roomService :RoomMarshaller;

    /** Contains the memories for all entities in this room. */
    public var memories :DSet = new DSet();

    /** Contains mappings for all controlled entities in this room. */
    public var controllers :DSet = new DSet();

    /** Contains the currently displayed "effects" (temporary furniture..). */
    public var effects :DSet = new DSet();

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        roomService = (ins.readObject() as RoomMarshaller);
        memories = (ins.readObject() as DSet);
        controllers = (ins.readObject() as DSet);
        effects = (ins.readObject() as DSet);
    }
}
}
