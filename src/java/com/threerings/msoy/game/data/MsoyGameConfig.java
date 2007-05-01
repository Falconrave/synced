//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;
import com.threerings.util.MessageBundle;

import com.threerings.crowd.client.PlaceController;

import com.threerings.ezgame.data.GameDefinition;

import com.threerings.toybox.data.ToyBoxGameConfig;

import com.threerings.msoy.item.data.all.Game;

/**
 * A game config for a metasoy game.
 */
public class MsoyGameConfig extends ToyBoxGameConfig
{
    /** The creator provided name of this game. */
    public String name;

    /**
     * Configures this config with information from the supplied {@link Game} item.
     */
    public void init (Game game, GameDefinition gameDef)
    {
        this.name = game.name;
        _gameId = game.getPrototypeId();
        _gameDef = gameDef;
    }

    @Override
    public PlaceController createController ()
    {
        try {
            return (PlaceController) Class.forName(getGameDefinition().controller).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        String manager = getGameDefinition().manager;
        return (manager != null) ? manager : "com.threerings.msoy.game.server.MsoyGameManager";
    }
}
