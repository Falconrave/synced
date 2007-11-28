//
// $Id$

package com.threerings.msoy.game.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides a mechanism for the client to resolve and obtain the connection info for a game.
 */
public interface MsoyGameService extends InvocationService
{
    /** Reports the server and port on which to connect to a requested game's server. */
    public static interface LocationListener extends InvocationListener
    {
        public void gameLocated (String host, int port);
    }

    /**
     * Locates (potentially resolving in the process) the server on which a game is hosted.
     */
    public void locateGame (Client client, int gameId, LocationListener listener);

    /**
     * Issues a request to the specified friends to invite them to join the requesting player's
     * game. If the game is still being match-made, they'll join the table in question, if it's in
     * play, they'll join the game if possible and watch otherwise.
     */
    public void inviteFriends (Client client, int gameId, int[] friendIds);
}
