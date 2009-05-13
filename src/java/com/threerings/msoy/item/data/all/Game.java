//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** Identifies our lobby background table media. */
    public static final String TABLE_MEDIA = "table";

    /** Identifies the game splash media. */
    public static final String SPLASH_MEDIA = "splash";

    /** Identifies our server code media. */
    public static final String SERVER_CODE_MEDIA = "scode";

    /** Defines the number of different game types. See GameConfig. */
    public static final int GAME_TYPES = 3;

    /** Value of groupId when there is no associated group */
    public static final int NO_GROUP = 0;

    /** This game's genre. */
    public byte genre;

    /** The XML game configuration. */
    public String config;

    /** The primary game media. */
    public MediaDesc gameMedia;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public int gameId;

    /** The game screenshot media. */
    public MediaDesc shotMedia;

    /** The game splash screen media for the loader. */
    public MediaDesc splashMedia;

    /**
     *  The server code media. Games may provide server code (in the form of a compiled action
     *  script library) to be run in a bureau whenever the game launches.
     *  @see com.threerings.bureau.server.BureauRegistry
     */
    public MediaDesc serverMedia;

    /** Optional group associated with this game; 0 means no group */
    public int groupId;

    /** The tag used to identify items in this game's shop. */
    public String shopTag;

    /**
     * Returns the id of the listed item for the given game id. Note this does not check to see
     * if the listing exists.
     */
    public static int getListedId (int gameId)
    {
        return gameId < 0 ? -gameId : gameId;
    }

    /**
     * Returns the id of the developement version of the given game id.
     */
    public static int getDevelopmentId (int gameId)
    {
        return -getListedId(gameId);
    }

    /**
     * For the given  XML game configuration string, checks if the game takes place in the world.
     */
    public static boolean detectIsInWorld (String config)
    {
        return (config != null) && (config.indexOf("<avrg/>") >= 0);
    }

    /**
     * Returns this game's screenshot media. Falls back to its thumbnail media in the absence of a
     * screenshot.
     */
    public MediaDesc getShotMedia ()
    {
        return (shotMedia == null) ? getThumbnailMedia() : shotMedia;
    }

    @Override // from Item
    public byte getType ()
    {
        return GAME;
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        if (_furniMedia != null) {
            return _furniMedia;
        }
        return getThumbnailMedia();
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return gameMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        gameMedia = desc;
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public boolean isInWorld ()
    {
        return detectIsInWorld(config);
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (gameMedia != null) &&
            !(isInWorld() && serverMedia == null);
    }
}
