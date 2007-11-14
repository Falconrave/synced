//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants used by the lobby services.
 */
public interface LobbyCodes extends InvocationCodes
{
    /** A mode constant for {@link LobbyService#playNow}. */
    public static final int PLAY_NOW_SINGLE = 0;

    /** A mode constant for {@link LobbyService#playNow}. */
    public static final int PLAY_NOW_FRIENDS = 1;

    /** A mode constant for {@link LobbyService#playNow}. */
    public static final int PLAY_NOW_ANYONE = 2;
}
