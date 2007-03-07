//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Stage;

import mx.core.Application;

import mx.managers.ISystemManager;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.Client;

import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.client.ParlorDirector;
import com.threerings.parlor.util.ParlorContext;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.spot.client.SpotSceneDirector;
import com.threerings.whirled.util.WhirledContext;

import com.threerings.msoy.client.persist.SharedObjectSceneRepository;

import com.threerings.msoy.chat.client.ChatOverlay;

import com.threerings.msoy.game.client.GameDirector;
import com.threerings.msoy.item.client.ItemDirector;
import com.threerings.msoy.world.client.WorldDirector;

/**
 * Defines services for the main virtual world and game clients. TODO: make GameContext?
 */
public class WorldContext extends BaseContext
    implements WhirledContext, ParlorContext
{
    /** Contains non-persistent properties that are set in various places
     * and can be bound to to be notified when they change.
     */
    public var worldProps :WorldProperties = new WorldProperties();

    public function WorldContext (client :Client)
    {
        super(client);

        _sceneRepo = new SharedObjectSceneRepository()
        _sceneDir = new SceneDirector(this, _locDir, _sceneRepo,
            new MsoySceneFactory());
        _spotDir = new SpotSceneDirector(this, _locDir, _sceneDir);
        _parlorDir = new ParlorDirector(this);
        _mediaDir = new MediaDirector(this);
        _gameDir = new GameDirector(this);
        _itemDir = new ItemDirector(this);
        _worldDir = new WorldDirector(this);

        // set up the top panel
        _topPanel = new TopPanel(this);
        _controller = new MsoyController(this, _topPanel);

        // ensure that the chat history is on..
        ChatOverlay.ensureHistoryListening(this);
    }

    // from WhirledContext
    public function getSceneDirector () :SceneDirector
    {
        return _sceneDir;
    }

    // from ParlorContext
    public function getParlorDirector () :ParlorDirector
    {
        return _parlorDir;
    }

    /**
     * Get the media director.
     */
    public function getMediaDirector () :MediaDirector
    {
        return _mediaDir;
    }

    /**
     * Get the GameDirector.
     */
    public function getGameDirector () :GameDirector
    {
        return _gameDir;
    }

    /**
     * Get the WorldDirector.
     */
    public function getWorldDirector () :WorldDirector
    {
        return _worldDir;
    }

    /**
     * Get the ItemDirector.
     */
    public function getItemDirector () :ItemDirector
    {
        return _itemDir;
    }

    /**
     * Get the SpotSceneDirector.
     */
    public function getSpotSceneDirector () :SpotSceneDirector
    {
        return _spotDir;
    }

    /**
     * Get the top-level msoy controller.
     */
    public function getMsoyController () :MsoyController
    {
        return _controller;
    }

    // from BaseContext
    override public function setPlaceView (view :PlaceView) :void
    {
        _topPanel.setPlaceView(view);
    }

    // from BaseContext
    override public function clearPlaceView (view :PlaceView) :void
    {
        _topPanel.clearPlaceView(view);
    }

    public function getTopPanel () :TopPanel
    {
        return _topPanel;
    }

    public function TEMPClearSceneCache () :void
    {
        _sceneRepo.TEMPClearSceneCache();
    }

    protected var _topPanel :TopPanel;
    protected var _controller :MsoyController;

    protected var _sceneDir :SceneDirector;
    protected var _spotDir :SpotSceneDirector;
    protected var _parlorDir :ParlorDirector;
    protected var _gameDir :GameDirector;
    protected var _mediaDir :MediaDirector;
    protected var _worldDir :WorldDirector;
    protected var _itemDir :ItemDirector;

    protected var _sceneRepo :SharedObjectSceneRepository;
}
}
