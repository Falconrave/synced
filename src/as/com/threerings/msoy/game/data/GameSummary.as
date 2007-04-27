//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.util.Cloneable;
import com.threerings.util.Equalable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

/**
 * Provides information on the game that a player is currently matchmaking for.
 */
public class GameSummary extends SimpleStreamableObject
    implements Cloneable
{
    /** The game item id */
    public var gameId :int;

    /** The name of the game - used as a tooltip */
    public var name :String;

    /** The mime type of this game's client media (SWF or JAR). */
    public var gameMediaType :int;

    /** The thumbnail of the game - used as a game icon */
    public var thumbMedia :MediaDesc;

    public function GameSummary (game :Game = null)
    {
        // only used for unserialization
    }

    /**
     * Returns the thumbnail media for the game we're summarizing.
     */
    public function getThumbMedia () :MediaDesc
    {
        return thumbMedia != null ? thumbMedia : Item.getDefaultThumbnailMediaFor(Item.GAME);
    }

    // documentation from Cloneable
    public function clone () :Object
    {
        var data :GameSummary = new GameSummary();
        data.gameId = this.gameId;
        data.name = this.name;
        data.thumbMedia = this.thumbMedia;
        return data;
    }

    // documentation from Equalable
    public function equals (other :Object) :Boolean
    {
        if (other is GameSummary) {
            var data :GameSummary = other as GameSummary;
            return data.gameId == this.gameId;
        }
        return false;
    }

    // documentation from Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        gameId = ins.readInt();
        name = (ins.readField(String) as String);
        gameMediaType = ins.readByte();
        thumbMedia = (ins.readObject() as MediaDesc);
    }

    // documntation from Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeInt(gameId);
        out.writeField(name);
        out.writeByte(gameMediaType);
        out.writeObject(thumbMedia);
    }
}
}
