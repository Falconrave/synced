//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.utils.ByteArray;

import com.threerings.util.HashMap;
import com.threerings.util.Iterator;
import com.threerings.util.Name;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.flash.MenuUtil;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.msoy.avrg.client.AVRGameBackend;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.PlaceLoadingDisplay;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.UberClient;

import com.threerings.msoy.chat.client.ChatInfoProvider;
import com.threerings.msoy.chat.client.ComicOverlay;

import com.threerings.msoy.chat.data.ChatChannel;

import com.threerings.msoy.world.client.editor.DoorTargetEditController;

import com.threerings.msoy.world.data.ActorInfo;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.ControllableAVRGame;
import com.threerings.msoy.world.data.ControllableEntity;
import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.FurniUpdate_Remove;
import com.threerings.msoy.world.data.MemberInfo;
import com.threerings.msoy.world.data.MobInfo;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomCodes;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

/**
 * Extends the base roomview with the ability to view a RoomObject, view chat, and edit.
 */
public class RoomObjectView extends RoomView
    implements ContextMenuProvider, SetListener, MessageListener,
               ChatDisplay, ChatInfoProvider
{
    /**
     * Create a roomview.
     */
    public function RoomObjectView (ctx :WorldContext, ctrl :RoomObjectController)
    {
        super(ctx, ctrl);
        _octrl = ctrl;
    }

    public function getRoomObjectController () :RoomObjectController
    {
        return _octrl;
    }

    // from MsoyPlaceView, via RoomView
    override public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        super.setPlaceSize(unscaledWidth, unscaledHeight);
        updateEditingOverlay();
    }

    /**
     * (Re)set our scene to the one the scene director knows about.
     */ 
    public function rereadScene () :void
    {       
        setScene(_ctx.getSceneDirector().getScene() as MsoyScene);
    }   

    /**
     * Called by the editor to have direct access to our sprite list..
     */
    public function getFurniSprites () :HashMap
    {
        return _furni;
    }

    override public function setScene (scene :MsoyScene) :void
    {
        super.setScene(scene);
        updateEditingOverlay();
    }

    override public function moveFinished (sprite :OccupantSprite) :void
    {
        if (sprite.getOid() == _ctx.getMemberObject().getOid()) {
            _ctx.getGameDirector().tutorialEvent("playerMoved");
        }

        super.moveFinished(sprite);
    }

    /**
     * Update the 'my' user's specified avatar's scale, non-permanently.  This is called via the
     * avatar viewer, so that scale changes they make are instantly viewable in the world.
     */
    public function updateAvatarScale (avatarId :int, newScale :Number) :void
    {
        var avatar :MemberSprite = getMyAvatar();
        if (avatar != null) {
            var occInfo :MemberInfo = (avatar.getOccupantInfo() as MemberInfo);
            if (occInfo.getItemIdent().equals(new ItemIdent(Item.AVATAR, avatarId))) {
                occInfo.setScale(newScale);
                avatar.setOccupantInfo(occInfo);
            }
        }
    }

    /**
     * Enable or disable editing.
     */
    public function setEditing (editing :Boolean) :void
    {
        _editing = editing;

        // update all sprites
        _furni.forEach(function (key :*, sprite :MsoySprite) :void {
                sprite.setEditing(_editing);
            });
        if (_bg != null) {
            _bg.setEditing(_editing);
        }

        if (_editing) {

            // see if we need to recreate the overlay
            if (_backdropOverlay == null) {
                _backdropOverlay = new BackdropOverlay();
                addChild(_backdropOverlay);
                updateEditingOverlay();
            }

        } else {
            // destroy the overlay if it's still around
            if (_backdropOverlay != null) {
                removeChild(_backdropOverlay);
                _backdropOverlay = null;
            }

            // definitely update the furni
            updateAllFurni();
        }

        // if we haven't yet started loading sprites other than the background, start now
        if (!_loadAllMedia) {
            _loadAllMedia = true;
            updateAllFurni();
            updateAllEffects();
        }
    }

    /**
     * Refreshes the overlay used to draw the room edges in editing mode.
     */
    protected function updateEditingOverlay () :void
    {
        // if the overlay exists, then we should update it
        if (_backdropOverlay != null) {
            _backdrop.drawRoom(
                _backdropOverlay.graphics, _actualWidth, _actualHeight, true, false, 0.4);
            _layout.updateScreenLocation(_backdropOverlay);
        }
    }

    /**
     * Called by our controller when a scene update is received.
     */
    public function processUpdate (update :SceneUpdate) :void
    {
        if (update is FurniUpdate_Remove) {
            removeFurni((update as FurniUpdate_Remove).data);

        } else if (update is SceneAttrsUpdate) {
            rereadScene(); // re-read our scene
            updateBackground();
            updateBackgroundAudio();
        }

        // this will take care of anything added
        updateAllFurni();
        updateAllEffects();
    }

    /**
     * A convenience function to get our personal avatar.
     */
    override public function getMyAvatar () :MemberSprite
    {
        return (getOccupant(_ctx.getClient().getClientOid()) as MemberSprite);
    }

    /**
     * A convenience function to get the specified occupant sprite, even if it's on the way out the
     * door.
     */
    public function getOccupant (bodyOid :int) :OccupantSprite
    {
        var sprite :OccupantSprite = (_occupants.get(bodyOid) as OccupantSprite);
        if (sprite == null) {
            sprite = (_pendingRemovals.get(bodyOid) as OccupantSprite);
        }
        return sprite;
    }

    /**
     * A convenience function to get the specified occupant sprite, even if it's on the way out the
     * door.
     */
    public function getOccupantByName (name :Name) :OccupantSprite
    {
        if (_roomObj == null) {
            return null;
        }

        var occInfo :OccupantInfo = _roomObj.getOccupantInfo(name);
        return (occInfo == null) ? null : getOccupant(occInfo.bodyOid);
    }

    /**
     * A convenience function to get an array of all sprites for all pets in the room.
     */
    public function getPets () :Array /* of PetSprite */
    {
        return _occupants.values().filter(function (o :*, i :int, a :Array) :Boolean {
            return (o is PetSprite);
        });
    }
    
    // from ContextMenuProvider
    public function populateContextMenu (ctx :MsoyContext, menuItems :Array) :void
    {
        var hit :* = _ctrl.getHitSprite(stage.mouseX, stage.mouseY, true);
        if (hit === undefined) {
            return;
        }
        var sprite :MsoySprite = (hit as MsoySprite);
        if (sprite == null) {
            if (_bg == null) {
                return;
            } else {
                sprite = _bg;
            }
        }

        var ident :ItemIdent = sprite.getItemIdent();
        if (ident != null) {
            var kind :String = Msgs.GENERAL.get(sprite.getDesc());
            if (ident.type > Item.NOT_A_TYPE) { // -1 is used for the default avatar, etc.
                menuItems.push(MenuUtil.createControllerMenuItem(
                                   Msgs.GENERAL.get("b.view_item", kind),
                                   MsoyController.VIEW_ITEM, ident));
            }

            if (sprite.isBlockable()) {
                var isBlocked :Boolean = sprite.isBlocked();
                menuItems.push(MenuUtil.createControllerMenuItem(
                    Msgs.GENERAL.get((isBlocked ? "b.unbleep_item" : "b.bleep_item"), kind),
                    sprite.toggleBlocked, ctx));
            }

            if ((sprite is FurniSprite) && _octrl.canManageRoom() &&
                    (null != (sprite as FurniSprite).getCustomConfigPanel())) {
                menuItems.push(MenuUtil.createControllerMenuItem(
                    Msgs.GENERAL.get("b.config_item", kind), _ctrl.showFurniConfigPopup, sprite));
            }
        }
    }

    // from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            addBody(event.getEntry() as OccupantInfo);

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            var sceneLoc :SceneLocation = (event.getEntry() as SceneLocation);
            portalTraversed(sceneLoc.loc, true);

        } else if (RoomObject.MEMORIES == name) {
            var mem :EntityMemoryEntry = event.getEntry() as EntityMemoryEntry;
            dispatchMemoryChanged(mem.item, mem.key, mem.value);

        } else if (RoomObject.ROOM_PROPERTIES == name) {
            var prop :RoomPropertyEntry = event.getEntry() as RoomPropertyEntry;
            dispatchRoomPropertyChanged(prop.key, prop.value);

        } else if (RoomObject.CONTROLLERS == name) {
            var ctrl :EntityControl = (event.getEntry() as EntityControl);
            if (ctrl.controllerOid == _ctx.getMemberObject().getOid()) {
                if (ctrl.controlled is ControllableEntity) {
                    dispatchEntityGotControl(
                        (ctrl.controlled as ControllableEntity).getItemIdent());

                } else {
                    dispatchAVRGameGotControl(
                        (ctrl.controlled as ControllableAVRGame).getGameId());
                }
            }

        } else if (RoomObject.EFFECTS == name) {
            addEffect(event.getEntry() as EffectData);
        }
    }

    // from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            updateBody(event.getEntry() as OccupantInfo, event.getOldEntry() as OccupantInfo);

        } else if (SpotSceneObject.OCCUPANT_LOCS == name) {
            moveBody((event.getEntry() as SceneLocation).bodyOid);

        } else if (RoomObject.MEMORIES == name) {
            var mem :EntityMemoryEntry = event.getEntry() as EntityMemoryEntry;
            dispatchMemoryChanged(mem.item, mem.key, mem.value);

        } else if (RoomObject.ROOM_PROPERTIES == name) {
            var prop :RoomPropertyEntry = event.getEntry() as RoomPropertyEntry;
            dispatchRoomPropertyChanged(prop.key, prop.value);

        } else if (RoomObject.EFFECTS == name) {
            updateEffect(event.getEntry() as EffectData);
        }
    }

    // from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            removeBody((event.getOldEntry() as OccupantInfo).getBodyOid());

        } else if (RoomObject.EFFECTS == name) {
            removeEffect(event.getKey() as int);

        } else if (RoomObject.ROOM_PROPERTIES == name) {
            dispatchRoomPropertyChanged(event.getKey() as String, null);
        }
    }

    // fro interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomCodes.SPRITE_MESSAGE:
            dispatchSpriteMessage((args[0] as ItemIdent), (args[1] as String),
                                  (args[2] as ByteArray), (args[3] as Boolean));
            break;
        case RoomCodes.SPRITE_SIGNAL:
            dispatchSpriteSignal((args[0] as String), (args[1] as ByteArray));
            break;
        }
    }

    // from ChatInfoProvider
    public function getBubblePosition (speaker :Name) :Point
    {
        var sprite :OccupantSprite = getOccupantByName(speaker);
        return (sprite == null) ? null : sprite.getBubblePosition();
    }

    // from ChatDisplay
    public function clear () :void
    {
        // nada
    }

    // from ChatDisplay
    public function displayMessage (msg :ChatMessage, alreadyDisplayed :Boolean) :Boolean
    {
        if (msg is UserMessage && ChatChannel.typeIsForRoom(msg.localtype, _scene.getId())) {
            var umsg :UserMessage = (msg as UserMessage);
            if (umsg.speaker.equals(_ctx.getMemberObject().memberName)) {
                _ctx.getGameDirector().tutorialEvent("playerSpoke");
            }
            var avatar :MemberSprite =
                (getOccupantByName(umsg.getSpeakerDisplayName()) as MemberSprite);
            if (avatar != null) {
                avatar.performAvatarSpoke();
            }

            // send it to pets as well
            var petSprites :Array = getPets();
            for each (var pet :PetSprite in petSprites) {
                pet.processChatMessage(umsg);
            }
        }

        return false; // we never display the messages ourselves
    }

    // from RoomView
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        // set load-all to false, as we're going to just load the decor item first.
        _loadAllMedia = false;
        FurniSprite.setLoadingWatcher(
            new PlaceLoadingDisplay(_ctx.getTopPanel().getPlaceContainer()));

        // save our scene object
        _roomObj = (plobj as RoomObject);

        rereadScene();
        updateAllFurni();

        // listen for client minimization events
        _ctx.getClient().addEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        _roomObj.addListener(this);

        addAllOccupants();

        // we add ourselves as a chat display so that we can trigger speak actions on avatars
        _ctx.getChatDirector().addChatDisplay(this);

        // let the chat overlay know about us so we can be queried for chatter locations
        var comicOverlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
        if (comicOverlay != null) {
            comicOverlay.willEnterPlace(this);
        }

        // and animate ourselves entering the room (everyone already in the (room will also have
        // seen it)
        portalTraversed(getMyCurrentLocation(), true);

        // load the background image first
        setBackground(_scene.getDecor());
        // load the decor data we have, even if it's just default values.
        _bg.setLoadedCallback(backgroundFinishedLoading);

        // start playing background audio
        _octrl.setBackgroundMusic(_scene.getAudioData());
    }

    // from RoomView
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomObj.removeListener(this);

        // stop listening for avatar speak action triggers
        _ctx.getChatDirector().removeChatDisplay(this);

        // tell the comic overlay to forget about us
        var comicOverlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
        if (comicOverlay != null) {
            comicOverlay.didLeavePlace(this);
        }

        // stop listening for client minimization events
        _ctx.getClient().removeEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        removeAll(_effects);
        removeAllOccupants();

        super.didLeavePlace(plobj);

        _roomObj = null;

        FurniSprite.setLoadingWatcher(null);

        // in case we were auto-scrolling, remove the event listener..
        removeEventListener(Event.ENTER_FRAME, tick);
    }

    // from RoomView
    override public function set scrollRect (r :Rectangle) :void
    {
        super.scrollRect = r;
        var overlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
        if (overlay != null) {
            overlay.setScrollRect(r);
        }
    }

    // documentation inherited
    override public function dispatchRoomPropertyChanged (key :String, data :ByteArray) :void
    {
        super.dispatchRoomPropertyChanged(key, data);
        // TODO: Fuck me, I wish we could not decode unless there is a backend..
        callAVRGCode("roomPropertyChanged_v1", key, ObjectMarshaller.decode(data));
    }

    /** Return an array of the MOB sprites associated with the identified game. */
    public function getMobs (gameId :int) :Array
    {
        var result :Array = new Array();
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MobInfo && MobInfo(occInfo).getGameId() == gameId) {
                var sprite :MobSprite = (_occupants.get(occInfo.getBodyOid()) as MobSprite);
                if (sprite) {
                    result.push(sprite);
                }
            }
        }
        return result;
    }

    /** Return a uniquely identified MOB associated with the given game, or null. */
    public function getMob (gameId :int, mobId :String) :MobSprite
    {
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MobInfo && MobInfo(occInfo).getGameId() == gameId &&
                MobInfo(occInfo).getIdent() == mobId) {
                var sprite :MobSprite = (_occupants.get(occInfo.getBodyOid()) as MobSprite);
                if (sprite) {
                    return sprite;
                }
            }
        }
        return null;
    }

    /** Signals that an AVRG has started (gameId != 0) or ended (gameId == 0). */
    public function avrGameAvailable (gameId :int) :void
    {
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MobInfo) {
                if (gameId == 0) {
                    removeBody(occInfo.getBodyOid());

                } else if (MobInfo(occInfo).getGameId() == gameId) {
                    addBody(occInfo);
                }
            }
        }
    }

    /** Executes a usercode method through the AVRG backend. */
    public function callAVRGCode (... args) :*
    {
        var backend :AVRGameBackend = _ctx.getGameDirector().getAVRGameBackend();
        if (backend != null) {
            return backend.callUserCode.apply(backend, args);
        }
    }

    /**
     * Called when control of an AVRG is assigned to us.
     */
    protected function dispatchAVRGameGotControl (gameId :int) :void
    {
        if (gameId != _ctx.getGameDirector().getGameId()) {
            log.warning("Got control over an AVRG we're not playing [gameId=" + gameId + "]");
            return;
        }
        log.debug("AVRG got control [gameId=" + gameId + "]");
        callAVRGCode("gotControl_v1");
    }

    override protected function backgroundFinishedLoading () :void
    {
        super.backgroundFinishedLoading();

        updateAllEffects();
        
        // TODO: HOWSABOUT WE ONLY USE THE DOOR THINGY WHEN WE'RE MAKING DOORS!
        // inform the "floating" door editor
        DoorTargetEditController.updateLocation();
    }

    /**
     * Called when the client is minimized or unminimized.
     */
    protected function miniWillChange (event :ValueEvent) :void
    {
        relayout();
    }

    override protected function relayout () :void
    {
        super.relayout();

        relayoutSprites(_effects.values())

        if (UberClient.isFeaturedPlaceView()) {
            var sceneWidth :int = Math.round(_scene.getWidth() * scaleX) as int;
            if (sceneWidth < _actualWidth) {
                // center a scene that is narrower than our view area.
                x = (_actualWidth - sceneWidth) / 2;
            } else {
                // center a scene that is wider than our view area.
                var rect :Rectangle = scrollRect;
                if (rect != null) {
                    var newX :Number = (_scene.getWidth() - _actualWidth / scaleX) / 2;
                    newX = Math.min(_scene.getWidth() - rect.width, Math.max(0, newX));
                    rect.x = newX;
                    scrollRect = rect;
                }
            }
        }
    }

    /**
     * Re-layout any effects.
     */
    protected function updateAllEffects () :void
    {
        if (shouldLoadAll()) {
            for each (var effect :EffectData in _roomObj.effects.toArray()) {
                updateEffect(effect);
            }
        }
    }

    /**
     * Restart playing the background audio.
     */
    protected function updateBackgroundAudio () :void
    {
        var audiodata :AudioData = _scene.getAudioData();
        if (audiodata != null) {
            _octrl.setBackgroundMusic(audiodata);
        }
    }

    protected function addBody (occInfo :OccupantInfo) :void
    {
        if (!shouldLoadAll()) {
            return;
        }

        // TODO: handle viewonly occupants

        var bodyOid :int = occInfo.getBodyOid();
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        // see if the occupant was already created, pending removal
        var occupant :OccupantSprite = (_pendingRemovals.remove(bodyOid) as OccupantSprite);

        if (occupant == null) {
            occupant = _ctx.getMediaDirector().getSprite(occInfo);
            if (occupant == null) {
                return; // we have no visualization for this kind of occupant, no problem
            }

            var overlay :ComicOverlay = _ctx.getTopPanel().getPlaceChatOverlay();
            if (overlay != null) {
                occupant.setChatOverlay(overlay as ComicOverlay);
            }
            _occupants.put(bodyOid, occupant);
            addSprite(occupant);
            occupant.setEntering(loc);
            occupant.roomScaleUpdated();

            // if we ever add ourselves, we follow it
            if (bodyOid == _ctx.getClient().getClientOid()) {
                setFastCentering(true);
                setCenterSprite(occupant);
            }

        } else {
            // update the sprite
            spriteWillUpdate(occupant);
            occupant.setOccupantInfo(occInfo);
            spriteDidUpdate(occupant);

            // place the sprite back into the set of active sprites
            _occupants.put(bodyOid, occupant);
            overlay = _ctx.getTopPanel().getPlaceChatOverlay();
            if (overlay != null) {
                occupant.setChatOverlay(overlay);
            }
            occupant.moveTo(loc, _scene);
        }

        // if this occupant is a pet, notify GWT that we've got a new pet in the room.
        if (occupant is PetSprite) {
            var ident :ItemIdent = occupant.getItemIdent();
            (_ctx.getClient() as WorldClient).itemUsageChangedToGWT(
                Item.PET, ident.itemId, Item.USED_AS_PET, _scene.getId());
        }
    }

    protected function removeBody (bodyOid :int) :void
    {
        var sprite :OccupantSprite = (_occupants.remove(bodyOid) as OccupantSprite);
        if (sprite == null) {
            return;
        }

        if (sprite.isMoving()) {
            _pendingRemovals.put(bodyOid, sprite);
        } else {
            removeSprite(sprite);
        }

        // if this occupant is a pet, notify GWT that we've removed a pet from the room.
        if (sprite is PetSprite) {
            (_ctx.getClient() as WorldClient).itemUsageChangedToGWT(
                Item.PET, sprite.getItemIdent().itemId, Item.UNUSED, 0);
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var sprite :OccupantSprite = (_occupants.get(bodyOid) as OccupantSprite);
        if (sprite == null) {
            // It's possible to get an occupant update while we're still loading the room
            // and haven't yet set up the occupant's sprite. Ignore.
            return;
        }
        var sloc :SceneLocation = (_roomObj.occupantLocs.get(bodyOid) as SceneLocation);
        sprite.moveTo(sloc.loc as MsoyLocation, _scene);
    }

    protected function updateBody (newInfo :OccupantInfo, oldInfo :OccupantInfo) :void
    {
        var sprite :OccupantSprite = (_occupants.get(newInfo.getBodyOid()) as OccupantSprite);
        if (sprite == null) {
            // It's possible to get an occupant update while we're still loading the room
            // and haven't yet set up the occupant's sprite. Ignore.
            return;
        }
        spriteWillUpdate(sprite);
        sprite.setOccupantInfo(newInfo);
        spriteDidUpdate(sprite);
    }

    protected function addEffect (effect :EffectData) :FurniSprite
    {
        var sprite :EffectSprite = new EffectSprite(_ctx, _octrl.adjustEffectData(effect));
        addSprite(sprite);
        sprite.setLocation(effect.loc);
        sprite.roomScaleUpdated();
        _effects.put(effect.id, sprite);
        return sprite;
    }

    protected function updateEffect (effect :EffectData) :void
    {
        var sprite :FurniSprite = (_effects.get(effect.id) as FurniSprite);
        if (sprite != null) {
            spriteWillUpdate(sprite);
            sprite.update(_octrl.adjustEffectData(effect));
            spriteDidUpdate(sprite);
        } else {
            addEffect(effect);
        }
    }

    protected function removeEffect (effectId :int) :void
    {
        var sprite :EffectSprite = (_effects.remove(effectId) as EffectSprite);
        if (sprite != null) {
            removeSprite(sprite);
        }
    }

    override protected function addAllOccupants () :void
    {
        if (!shouldLoadAll()) {
            return;
        }

        // add all currently present occupants
        for each (var occInfo :OccupantInfo in _roomObj.occupantInfo.toArray()) {
            if (!_occupants.containsKey(occInfo.getBodyOid())) {
                addBody(occInfo);
            }
        }
    }

    override protected function removeSprite (sprite :MsoySprite) :void
    {
        super.removeSprite(sprite);

        if (sprite is MobSprite) {
            MobSprite(sprite).removed();
        }
    }

    /** _ctrl, casted as a RoomObjectController. */
    protected var _octrl :RoomObjectController;

    /** The transitory properties of the current scene. */
    protected var _roomObj :RoomObject;

    /** Transparent bitmap on which we can draw the room backdrop.*/
    protected var _backdropOverlay :BackdropOverlay;

    /** Maps effect id -> EffectData for effects. */
    protected var _effects :HashMap = new HashMap();

}
}

import flash.display.Shape;

import com.threerings.msoy.world.client.RoomElement;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;

/**
 * This shape is used to draw room edges on top of everything during editing.
 */
internal class BackdropOverlay extends Shape
    implements RoomElement
{
    // documentation inherited from interface RoomElement
    public function getLayoutType () :int
    {
        return RoomCodes.LAYOUT_NORMAL;
    }

    // documentation inherited from interface RoomElement
    public function getRoomLayer () :int
    {
        return RoomCodes.FOREGROUND_EFFECT_LAYER;
    }

    // documentation inherited from interface RoomElement
    public function getLocation () :MsoyLocation
    {
        return DEFAULT_LOCATION;
    }

    // documentation inherited from interface RoomElement
    public function isImportant () :Boolean
    {
        return false;
    }

    // documentation inherited from interface RoomElement
    public function setLocation (newLoc :Object) :void
    {
        // no op - this object is not placed inside the room
    }

    // documentation inherited from interface RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        // no op - this object cannot be moved, it's always displayed directly on top of the room
    }

    protected static const DEFAULT_LOCATION :MsoyLocation = new MsoyLocation(0, 0, 0, 0);
}
