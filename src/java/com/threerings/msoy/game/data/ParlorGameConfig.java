//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;
import com.threerings.toybox.data.ToyBoxGameConfig;
import com.whirled.game.data.GameDefinition;
import com.threerings.msoy.item.data.all.Game;

/**
 * A game config for a metasoy game.
 */
public class ParlorGameConfig extends ToyBoxGameConfig
{
    /** The game item. */
    public Game game;

    /** The game's groupId, or 0 for none. */
    public int groupId;

    /**
     * Configures this config with information from the supplied {@link Game} item.
     */
    public void init (Game game, GameDefinition gameDef, int groupId)
    {
        this.game = game;
        this.groupId = groupId;
        _gameId = game.gameId;
        _gameDef = gameDef;
    }

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        String manager = getGameDefinition().manager;
        return (manager != null) ? manager : "com.threerings.msoy.game.server.ParlorGameManager";
    }
}
