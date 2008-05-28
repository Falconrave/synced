//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.web.data.ArcadeData;
import com.threerings.msoy.web.data.FeaturedGameInfo;
import com.threerings.msoy.web.data.GameDetail;
import com.threerings.msoy.web.data.GameGenreData;
import com.threerings.msoy.web.data.GameMetrics;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.PlayerRating;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TrophyCase;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Provides game related services.
 */
public interface GameService extends RemoteService
{
    /** Return result for {@link #compareTrophies}. */
    public static class CompareResult implements IsSerializable
    {
        /** The name of the game for which we're comparing trophies. */
        public String gameName;

        /** The thumbnail icon for the game in question. */
        public MediaDesc gameThumb;

        /** The trophies for the game in question, in display order. */
        public Trophy[] trophies;

        /** The members being compared. */
        public MemberCard[] members;

        /** When the members earned the trophies. Array elements may be null if the a member has
         * not earned a particular trophy. */
        public Long[][] whenEarneds;
    }

    /**
     * Loads the details for the specified game.
     */
    public GameDetail loadGameDetail (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Loads the metrics for the specified game. Caller must be an admin or the owner of the game.
     */
    public GameMetrics loadGameMetrics (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Updates the instructions for the specified game. The caller must be the owner of the game's
     * source item.
     */
    public void updateGameInstructions (WebIdent ident, int gameId, String instructions)
        throws ServiceException;

    /**
     * Requests to reset the percentiler scores for the game in question. Caller must be an admin
     * or the owner of the game.
     *
     * @param single if true the single player scores will be reset, if false the multiplayer
     * scores will be reset.
     */
    public void resetGameScores (WebIdent ident, int gameId, boolean single)
        throws ServiceException;

    /**
     * Loads and returns the trophies awarded by the specified game. Filling in when they were
     * earned by the caller if possible.
     *
     * @gwt.typeArgs <com.threerings.msoy.game.data.all.Trophy>
     */
    public List loadGameTrophies (WebIdent ident, int gameId)
        throws ServiceException;

    /**
     * Compares the trophy earnings for the specified set of members in the specified game.
     */
    public CompareResult compareTrophies (WebIdent ident, int gameId, int[] memberIds)
        throws ServiceException;

    /**
     * Loads all trophies owned by the specified member.
     */
    public TrophyCase loadTrophyCase (WebIdent ident, int memberId)
        throws ServiceException;

    /**
     * Returns the top-ranked players for the specified game.
     *
     * @param onlyMyFriends if true, only the player and their friends will be included in the
     * rankings.
     *
     * @return two arrays, the first the single-player rankings for this game, the second its
     * multiplayer rankings.
     */
    public PlayerRating[][] loadTopRanked (WebIdent ident, int gameId, boolean onlyMyFriends)
        throws ServiceException;

    /**
     * Loads up information for the arcade.
     */
    public ArcadeData loadArcadeData (WebIdent ident)
        throws ServiceException;

    /**
     * Loads up information on the specified game genre.
     */
    public GameGenreData loadGameGenre (WebIdent ident, byte genre)
        throws ServiceException;
    
    /**
     * Loads up information for the landing page top games.
     */
    public FeaturedGameInfo[] loadTopGamesData (WebIdent ident)
        throws ServiceException;
}
