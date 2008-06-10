//
// $Id: GameLogs.java 8844 2008-04-15 17:05:43Z nathan $

package com.threerings.msoy.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Information displayed to the creator of a game.
 */
public class GameLogs
    implements IsSerializable
{
    /** The id of the game for which we're reporting logs. */
    public int gameId;

    /** The identifiers of the logs we've got stored for this game. */
    public int[] logIds;

    /** The timestamps when these logs were recorded. */
    public Date[] logTimes;
}
