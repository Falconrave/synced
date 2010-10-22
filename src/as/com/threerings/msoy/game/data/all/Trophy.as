//
// $Id$

package com.threerings.msoy.game.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.Long;

import com.threerings.orth.data.MediaDesc;

/**
 * Contains information on a trophy held by a player.
 */
public class Trophy extends SimpleStreamableObject
{
    /** The game for which this trophy was awarded. */
    public var gameId :int;

    /** The name of the trophy. */
    public var name :String;

    /** The identifier of the trophy. */
    public var ident :String;

    /** The description of how to earn this trophy (not always available). */
    public var description :String;

    /** The media for the trophy image. */
    public var trophyMedia :MediaDesc;

    /** When this trophy was earned. */
    public var whenEarned :Long;

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        gameId = ins.readInt();
        name = (ins.readField(String) as String);
        ident = (ins.readField(String) as String);
        description = (ins.readField(String) as String);
        trophyMedia = MediaDesc(ins.readObject());
        whenEarned = (ins.readField(Long) as Long);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(gameId);
        out.writeField(name);
        out.writeField(ident);
        out.writeField(description);
        out.writeObject(trophyMedia);
        out.writeField(whenEarned);
    }
}
}
