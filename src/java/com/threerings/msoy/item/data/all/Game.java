//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** Identifies our lobby background table media. */
    public static final String TABLE_MEDIA = "table";

    /** Defines the number of different game types. See GameConfig. */
    public static final int GAME_TYPES = 3;

    /** The XML game configuration. */
    public String config;

    /** The primary game media. */
    public MediaDesc gameMedia;

    // @Override from Item
    public byte getType ()
    {
        return GAME;
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getThumbnailMedia(); // TODO: support logos?
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public boolean isInWorld ()
    {
        // TODO: this will change
        return (0 <= config.indexOf("<toggle ident=\"avrg\" start=\"true\"/>")) ||
            (0 <= config.indexOf("<toggle ident=\"chiyogami\" start=\"true\"/>"));
    }

    // @Override
    public boolean isConsistent ()
    {
        if (!super.isConsistent() || !nonBlank(name)) {
            return false;
        }
        // TODO: Check over the values in the XML to make sure they are sane
        return (gameMedia != null);
    }
}
