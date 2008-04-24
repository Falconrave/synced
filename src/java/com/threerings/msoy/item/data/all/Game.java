//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** A {@link #genre} constant. */
    public static final byte GENRE_OTHER = 0;

    /** A {@link #genre} constant. */
    public static final byte GENRE_WORD = 1;

    /** A {@link #genre} constant. */
    public static final byte GENRE_CARD_BOARD = 2;

    /** A {@link #genre} constant. */
    public static final byte GENRE_PUZZLE = 3;

    /** A {@link #genre} constant. */
    public static final byte GENRE_STRATEGY = 4;

    /** A {@link #genre} constant. */
    public static final byte GENRE_ACTION_ARCADE = 5;

    /** A {@link #genre} constant. */
    public static final byte GENRE_ADVENTURE_RPG = 6;

    /** A {@link #genre} constant. */
    public static final byte GENRE_SPORTS_RACING = 7;

    /** A {@link #genre} constant. */
    public static final byte GENRE_MMO_WHIRLED = 8;

    /** All game genres, in display order. */
    public static final byte[] GENRES = {
        GENRE_WORD, GENRE_STRATEGY, GENRE_ACTION_ARCADE, GENRE_CARD_BOARD, GENRE_PUZZLE,
        GENRE_ADVENTURE_RPG, GENRE_SPORTS_RACING, GENRE_MMO_WHIRLED, GENRE_OTHER
    };

    /** Identifies our lobby background table media. */
    public static final String TABLE_MEDIA = "table";

    /** Defines the number of different game types. See GameConfig. */
    public static final int GAME_TYPES = 3;

    /** We reserve a very unlikely gameId for the tutorial. */
    public static final int TUTORIAL_GAME_ID = Integer.MAX_VALUE;

    /** The width of a game screenshot. */
    public static final int SHOT_WIDTH = 175;

    /** The height of a game screenshot. */
    public static final int SHOT_HEIGHT = 125;

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

    /**
     * Returns true if the specified game is a developer's in-progress original game rather than
     * one listed in the catalog.
     */
    public static boolean isDeveloperVersion (int gameId)
    {
        return (gameId < 0);
    }

    /**
     * Returns true if this is a developer's in-progress original game rather than one listed in
     * the catalog.
     */
    public boolean isDeveloperVersion ()
    {
        return isDeveloperVersion(gameId);
    }

    /**
     * Returns this game's screenshot media. Falls back to its thumbnail media in the absence of a
     * screenshot.
     */
    public MediaDesc getShotMedia ()
    {
        return (shotMedia == null) ? getThumbnailMedia() : shotMedia;
    }

    // @Override from Item
    public byte getType ()
    {
        return GAME;
    }

    // @Override from Item
    public SubItem[] getSubTypes ()
    {
        return (isInWorld() ?
                new SubItem[] {
                    new LevelPack(), new ItemPack(), new TrophySource(), new Prize(), new Prop() } :
                new SubItem[] {
                    new LevelPack(), new ItemPack(), new TrophySource(), new Prize(), });
    }

    // @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        if (furniMedia != null) {
            return furniMedia;
        }
        return getThumbnailMedia();
    }

    // @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return gameMedia;
    }
 
    // @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        gameMedia = desc;
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public boolean isInWorld ()
    {
        return (config != null) && (config.indexOf("<avrg/>") >= 0);
    }

    // @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (gameMedia != null);
    }
}
