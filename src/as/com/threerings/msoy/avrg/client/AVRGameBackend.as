//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.display.DisplayObject;
import flash.utils.ByteArray;
import flash.events.Event;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyPeep;
import com.threerings.msoy.room.client.MemberSprite;
import com.threerings.msoy.room.client.MobSprite;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.client.RoomController;
import com.threerings.msoy.room.client.RoomMetrics;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesObject;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MethodQueue;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.StringUtil;
import com.threerings.whirled.data.Scene;
import com.whirled.game.data.WhirledPlayerObject;

public class AVRGameBackend extends ControlBackend
{
    public const log :Log = Log.getLog(this);

    /**
     * Magic number for specifying the server agent.
     */
    public static const SERVER_AGENT_ID :int = int.MIN_VALUE;

    public function AVRGameBackend (
        wctx :WorldContext, gctx :GameContext, ctrl :AVRGameController, gameObj :AVRGameObject)
    {
        _wctx = wctx;
        _gctx = gctx;
        _ctrl = ctrl;
        _gameObj = gameObj;

        _playerObj = _gctx.getPlayerObject();

        _wctx.getPartyDirector().addEventListener("partyChanged", handlePartyChanged);
    }

    // from ControlBackend
    override public function shutdown () :void
    {
        if (_gameNetAdapter != null) {
            _gameNetAdapter.release();
        }

        if (_playerNetAdapter != null) {
            _playerNetAdapter.release();
        }

        if (_roomPropsNetAdapter != null) {
            _roomPropsNetAdapter.release();
        }

        _wctx.getPartyDirector().removeEventListener("partyChanged", handlePartyChanged);

        super.shutdown();
    }

    public function isConnected () :Boolean
    {
        return _props != null;
    }

    public function panelResized () :void
    {
        MethodQueue.callLater(callUserCode, [ "panelResized_v1" ]);
    }

    public function requestMobSprite (id :String) :DisplayObject
    {
        return callUserCode("requestMobSprite_v1", id) as DisplayObject;
    }

    public function mobRemoved (id :String) :void
    {
        callUserCode("mobRemoved_v1", id);
    }

    public function mobAppearanceChanged (
        id :String, locArray :Array, orient :Number, moving :Boolean, idle :Boolean) :void
    {
        callUserCode("mobAppearanceChanged_v1", id, locArray, orient, moving, idle);
    }

    public function hitTestPoint (x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        return callUserCode("hitTestPoint_v1", x, y, shapeFlag) as Boolean;
    }

    public function isPlaying () :Boolean
    {
        return _wctx.getGameDirector().getGameId() == _ctrl.getGameId();
    }

    public function playerLeftRoom (roomId :int) :void
    {
        callUserCode("leftRoom_v1", roomId);

        if (_roomPropsNetAdapter != null) {
            _roomPropsNetAdapter.release();
            _roomPropsNetAdapter = null;
        }

        if (_avatarAdapter != null) {
            _avatarAdapter.release();
            _avatarAdapter = null;
        }
    }

    public function playerEnteredRoom (roomId :int, roomProps :RoomPropertiesObject) :void
    {
        if (_props == null) {
            return;
        }

        _roomPropsNetAdapter = new BackendNetAdapter(
            roomProps, RoomPropertiesObject.USER_MESSAGE,
            _props, "room_propertyWasSet_v1", "room_messageReceived_v1");

        _avatarAdapter = new BackendAvatarAdapter(
            _gameObj, _ctrl.getRoom(), _props, "playerMoved_v1", "actorStateSet_v1",
            "actorAppearanceChanged_v1");

        callUserCode("enteredRoom_v1", roomId);
    }

    public function roomOccupantAdded (memberId :int) :void
    {
        callUserCode("playerEntered_v1", memberId);
    }

    public function roomOccupantRemoved (memberId :int) :void
    {
        callUserCode("playerLeft_v1", memberId);
    }

    public function taskCompleted (taskId :String, amount :int) :Boolean
    {
        return callUserCode("taskCompleted_v1", taskId, amount);
    }

    public function playerMoved (memberId :int) :void
    {
        callUserCode("playerMoved_v1", memberId);
    }

    public function gotControl () :void
    {
    }

    public function signalReceived (name :String, value :ByteArray) :void
    {
        callUserCode("signalReceived_v1", name, ObjectMarshaller.decode(value));        
    }

    public function processChatMessage (entityIdent :String, msg :String) :void
    {
        callUserCode("receivedChat_v2", entityIdent, msg);
    }

    public function processMusicStartStop (started :Boolean) :void
    {
        callUserCode("musicStartStop_v1", started);
    }

    public function processMusicId3 (metadata :Object) :void
    {
        callUserCode("musicId3_v1", metadata);
    }

    override protected function handleUserCodeConnect (evt :Object) :void
    {
        super.handleUserCodeConnect(evt);

        MethodQueue.callLater(_ctrl.backendConnected);
    }

    override public function callUserCode (name :String, ... args) :*
    {
        if (_props == null) {
            log.warning("Calling user code " + name + " before connection.");

        } else if (_props[name] == null) {
            log.warning("User code " + name + " not found.");
        }

        args.unshift(name);
        return super.callUserCode.apply(null, args);
    }

    override protected function setUserProperties (o :Object) :void
    {
        super.setUserProperties(o);

        _gameNetAdapter = new BackendNetAdapter(
            _gameObj, AVRGameObject.USER_MESSAGE, o, "game_propertyWasSet_v1",
            "game_messageReceived_v1");

        _playerNetAdapter = new BackendNetAdapter(
            _playerObj, WhirledPlayerObject.getMessageName(_gameObj.getOid()), o,
            "player_propertyWasSet_v1", "player_messageReceived_v1");
    }

    // from ControlBackend
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        // GameSubControl
        o["game_getGameData_v1"] = game_getGameData_v1;
        o["game_getPlayerIds_v1"] = game_getPlayerIds_v1;
        o["game_getOccupantName_v1"] = game_getOccupantName_v1;
        o["getLevelPacks_v2"] = getLevelPacks_v2;
        o["getItemPacks_v1"] = getItemPacks_v1;

        // RoomSubControl
        o["room_getRoomId_v1"] = room_getRoomId_v1;
        o["room_getRoomName_v1"] = room_getRoomName_v1;
        o["room_getPlayerIds_v1"] = room_getPlayerIds_v1;
        o["room_getOccupantIds_v1"] = room_getOccupantIds_v1;
        o["room_getOccupantName_v1"] = room_getOccupantName_v1;
        o["room_canEditRoom_v1"] = room_canEditRoom_v1;
        o["room_getGameData_v1"] = room_getGameData_v1;
        o["isPlayerHere_v1"] = isPlayerHere_v1;
        o["getAvatarInfo_v2"] = getAvatarInfo_v2;
        o["getEntityIds_v1"] = getEntityIds_v1;
        o["getEntityProperty_v1"] = getEntityProperty_v1;
        o["getMusicId3_v1"] = getMusicId3_v1;
        // .getRoom() backwards compat
        o["getAvatarInfo_v1"] = getAvatarInfo_v1;

        // PlayerSubControl
        o["player_getGameData_v1"] = player_getGameData_v1;
        o["player_setProperty_v1"] = player_setProperty_v1;
        o["player_getRoomId_v1"] = player_getRoomId_v1;
        o["player_moveToRoom_v1"] = player_moveToRoom_v1;
        o["getAvatarMasterItemId_v1"] = getAvatarMasterItemId_v1;
        o["getPlayerId_v1"] = getPlayerId_v1;
        o["getPlayerName_v1"] = getPlayerName_v1;
        o["holdsTrophy_v1"] = holdsTrophy_v1;
        o["getPlayerItemPacks_v1"] = getPlayerItemPacks_v1;
        o["getPlayerLevelPacks_v1"] = getPlayerLevelPacks_v1;
        o["requestConsumeItemPack_v1"] = requestConsumeItemPack_v1;
        o["deactivateGame_v1"] = deactivateGame_v1;
        o["completeTask_v1"] = completeTask_v1;
        o["playAvatarAction_v1"] = playAvatarAction_v1;
        o["setAvatarState_v1"] = setAvatarState_v1;
        o["setAvatarMoveSpeed_v1"] = setAvatarMoveSpeed_v1;
        o["setAvatarLocation_v1"] = setAvatarLocation_v1;
        o["setAvatarOrientation_v1"] = setAvatarOrientation_v1;

        // LocalSubControl
        o["localChat_v1"] = localChat_v1;
        o["getStageSize_v1"] = getPaintableArea_v1; // backwards compat.
        o["getPaintableArea_v1"] = getPaintableArea_v1;
        o["getRoomBounds_v1"] = getRoomBounds_v1;
        o["stageToRoom_v1"] = stageToRoom_v1;
        o["roomToStage_v1"] = roomToStage_v1;
        o["locationToRoom_v1"] = locationToRoom_v1;
        o["stageToLocationAtDepth_v1"] = stageToLocationAtDepth_v1;
        o["stageToLocationAtHeight_v1"] = stageToLocationAtHeight_v1;
        o["roomToLocationAtDepth_v1"] = stageToLocationAtDepth_v1; // backwards compat.
        o["roomToLocationAtHeight_v1"] = stageToLocationAtHeight_v1; // backwards compat.
        o["showInvitePage_v1"] = showInvitePage_v1;
        o["getInviteToken_v1"] = getInviteToken_v1;
        o["getInviterMemberId_v1"] = getInviterMemberId_v1;
        o["getRoomBounds_vRay"] = getRoomBounds_vRay; // Added: Ray Jan 23, 2009.

        // AgentSubControl
        o["agent_sendMessage_v1"] = agent_sendMessage_v1;

        // TODO: MobControl helpers
        o["setMobDecoration_v1"] = setMobDecoration_v1;
        o["setMobHotSpot_v1"] = setMobHotSpot_v1;

        // TEMP
        o["getPartyInfo_temp"] = getPartyInfo_temp;
    }

    // GameSubControl
    protected function game_getGameData_v1 (targetId :int) :Object
    {
        return _gameObj.getUserProps();
    }

    // GameSubControl
    protected function game_getPlayerIds_v1 () :Array
    {
        if (!isPlaying()) {
            return null;
        }

        return BackendUtils.getPlayerIds(_gameObj, null, 0);
    }

    // GameSubControl
    protected function game_getOccupantName_v1 (playerId :int) :String
    {
        return BackendUtils.getOccupantName(_gameObj, playerId);
    }

    // GameSubControl
    protected function getLevelPacks_v2 (filter :Function = null) :Array
    {
        return BackendUtils.getLevelPacks(_gameObj.gameData, filter);
    }

    // GameSubControl
    protected function getItemPacks_v1 (filter :Function = null) :Array
    {
        return BackendUtils.getItemPacks(_gameObj.gameData, filter);
    }

    // RoomSubControl
    protected function room_getRoomId_v1 (targetId :int /* ignored */) :int
    {
        validateRoomTargetId(targetId);
        // TODO: this should guarantee to only return a non-zero value after the room entry event
        // has been sent
        return getRoomId();
    }

    // RoomSubControl
    protected function room_getRoomName_v1 (targetId :int /* ignored */) :String
    {
        validateRoomTargetId(targetId);
        var robj :RoomObject = (_wctx.getLocationDirector().getPlaceObject() as RoomObject);
        return (robj == null) ? null : robj.name;
    }

    // RoomSubControl
    protected function room_getPlayerIds_v1 (targetId :int /* ignored */) :Array
    {
        validateRoomTargetId(targetId);
        if (!isPlaying() || _ctrl.getRoom() == null) {
            return null;
        }
        return BackendUtils.getPlayerIds(_gameObj, _ctrl.getRoom(), getRoomId());
    }

    // RoomSubControl
    protected function room_getOccupantIds_v1 (targetId :int /* ignored */) :Array
    {
        validateRoomTargetId(targetId);
        if (!isPlaying() || _ctrl.getRoom() == null) {
            return null;
        }
        return BackendUtils.getOccupantIds(_ctrl.getRoom());
    }

    // RoomSubControl
    protected function room_getOccupantName_v1 (targetId :int /* ignored */, playerId :int) :String
    {
        validateRoomTargetId(targetId);
        if (!isPlaying() || _ctrl.getRoom() == null) {
            return null;
        }
        return BackendUtils.getOccupantName(_ctrl.getRoom(), playerId);
    }

    // RoomSubControl
    protected function room_canEditRoom_v1 (targetId :int /* ignored */, memberId :int) :Boolean
    {
        validateRoomTargetId(targetId);
        var ctrl :RoomController = getRoomController("room_canEditRoom(" + memberId + ")");
        return (ctrl != null) && ctrl.canManageRoom(memberId);
    }

    // RoomSubControl
    protected function room_getGameData_v1 (targetId :int) :Object
    {
        validateRoomTargetId(targetId);
        var roomProps :RoomPropertiesObject = _ctrl.getRoomProperties();
        if (roomProps == null) {
            log.warning("Room properties not found [roomObj=" + _ctrl.getRoom() + "]");
            return { };
        }
        return roomProps.getUserProps();
    }

    // RoomSubControl
    protected function isPlayerHere_v1 (targetId :int /* ignored */, playerId :int) :Boolean
    {
        validateRoomTargetId(targetId);
        if (isPlaying() && _ctrl.getRoom() != null) {
            var fakeName :MemberName = new MemberName("", playerId);
            return (_gameObj.getOccupantInfo(fakeName) != null &&
                    _ctrl.getRoom().getOccupantInfo(fakeName) != null);
        }
        return false;
    }

    // RoomSubControl
    protected function getAvatarInfo_v1 (targetId :int /* ignored */, playerId :int) :Array
    {
        var obj :Object = getAvatarInfo_v2(targetId, playerId);
        return (obj == null) ? null :
            [ obj["name"], obj["state"], obj["x"], obj["y"], obj["z"], obj["orient"],
              obj["moveSpeed"], obj["isMoving"], obj["isIdle"], obj["bounds"] ];
    }

    // RoomSubControl
    protected function getAvatarInfo_v2 (targetId :int /* ignored */, playerId :int) :Object
    {
        validateRoomTargetId(targetId);
        var sprite :MemberSprite = getAvatarSprite(playerId);
        if (sprite != null) {
            var data :Object = new Object();
            data["entityId"] = sprite.getActorInfo().getItemIdent().toString();
            data["state"] = sprite.getState();
            data["x"] = sprite.getLocation().x;
            data["y"] = sprite.getLocation().y;
            data["z"] = sprite.getLocation().z;
            data["orient"] = sprite.getLocation().orient;
            data["moveSpeed"] = sprite.getMoveSpeed(1); // TODO: this may be inaccurate. remove?
            data["isMoving"] = sprite.isMoving();
            data["isIdle"] = sprite.isIdle();
            data["bounds"] = sprite.getBounds(sprite.stage);
            data["name"] = sprite.getActorInfo().username.toString(); // deprecated, only for _v1
            return data;
        }
        return null;
    }

    // RoomSubControl
    protected function getEntityIds_v1 (targetId :int, type :String) :Array
    {
        validateRoomTargetId(targetId);
        var ctrl :RoomController = getRoomController("getEntityIds(" + type + ")");
        return ctrl != null ? ctrl.getEntityIds(type) : null;
    }

    // RoomSubControl
    protected function getEntityProperty_v1 (targetId :int, entityId :String, key :String) :Object
    {
        validateRoomTargetId(targetId);
        var ctrl :RoomController = getRoomController(
            "getEntityProperty(" + entityId + ", " + key + ")");
        return ctrl != null ? ctrl.getEntityProperty(ItemIdent.fromString(entityId), key) : null;
    }

    // RoomSubControlClient
    protected function getMusicId3_v1 (targetId :int) :Object
    {
        validateRoomTargetId(targetId);
        var view :RoomView = getRoomView("getMusicId3()");
        return (view == null) ? null : view.getMusicId3();
    }

    // PlayerSubControl
    protected function player_getGameData_v1 (targetId :int) :Object
    {
        validatePlayerTargetId(targetId);
        return _playerObj.getUserProps();
    }

    // PlayerSubControl
    protected function player_setProperty_v1 (
        targetId :int, propName :String, value :Object, key :Object, isArray :Boolean,
        immediate :Boolean) :void
    {
        validatePlayerTargetId(targetId);
        if (!isPlaying()) {
            return;
        }

        BackendUtils.encodeAndSet(
            _gctx.getClient(), _playerObj, propName, value, key, isArray, immediate);
    }

    // PlayerSubControl
    protected function getPlayerId_v1 (targetId :int /* ignored */) :int
    {
        validatePlayerTargetId(targetId);
        return _wctx.getMyId();
    }

    // PlayerSubControl
    protected function getPlayerName_v1 (targetId :int /* ignored */) :String
    {
        return _wctx.getMyName().toString();
    }

    // PlayerSubControl
    protected function deactivateGame_v1 (targetId :int /* ignored */) :void
    {
        validatePlayerTargetId(targetId);
        if (!isPlaying()) {
            return;
        }

        _ctrl.deactivateGame();
    }

    // PlayerSubControl
    protected function player_getRoomId_v1 (targetId :int /* ignored */) :int
    {
        validatePlayerTargetId(targetId);
        // TODO: this should guarantee to only return a non-zero value after the room entry event
        // has been sent
        return getRoomId();
    }

    // PlayerSubControl
    protected function player_moveToRoom_v1 (
        targetId :int /* ignored */, roomId :int, exitCoords :Array) :void
    {
        validatePlayerTargetId(targetId);
        // TODO: how to handle exit coords?
        _wctx.getWorldController().handleGoScene(roomId);
    }

    // PlayerSubControl
    protected function getAvatarMasterItemId_v1 (targetId :int) :int
    {
        validatePlayerTargetId(targetId);

        if (_wctx.getMemberObject().avatar == null) {
            return 0;
        }
        return _wctx.getMemberObject().avatar.getMasterId();
    }

    // PlayerSubControl
    protected function holdsTrophy_v1 (targetId :int /* ignored */, ident :String) :Boolean
    {
        validatePlayerTargetId(targetId);
        return BackendUtils.holdsTrophy(targetId, ident, playerOwnsData);
    }

    // PlayerSubControl
    protected function getPlayerItemPacks_v1 (targetId :int /* ignored */) :Array
    {
        validatePlayerTargetId(targetId);
        return BackendUtils.getPlayerItemPacks(_gameObj.gameData, targetId, countPlayerData);
    }

    // PlayerSubControl
    protected function getPlayerLevelPacks_v1 (targetId :int /* ignored */) :Array
    {
        validatePlayerTargetId(targetId);
        return BackendUtils.getPlayerLevelPacks(_gameObj.gameData, targetId, playerOwnsData);
    }

    // PlayerSubControl
    protected function requestConsumeItemPack_v1 (
        targetId :int /* ignored */, ident :String, msg :String) :Boolean
    {
        return false; // TODO
    }

    // PlayerSubControl
    protected function completeTask_v1 (
        targetId :int /* ignored */, taskId :String, payout :Number) :void
    {
        validatePlayerTargetId(targetId);
        if (StringUtil.isBlank(taskId) || !isPlaying()) {
            return;
        }

        _gameObj.avrgService.completeTask(
            _gctx.getClient(), 0, taskId, Math.max(0, Math.min(1, payout)),
            BackendUtils.loggingConfirmListener("completeTask"));
    }

    // PlayerSubControl
    protected function playAvatarAction_v1 (targetId :int /* ignored */, action :String) :void
    {
        validatePlayerTargetId(targetId);
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.sendMessage(action, null, true);
        } else {
            BackendUtils.playAvatarAction(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), _wctx.getMyId(), action);
        }
    }

    // PlayerSubControl
    protected function setAvatarState_v1 (targetId :int /* ignored */, state :String) :void
    {
        validatePlayerTargetId(targetId);
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setState(state);
        } else {
            BackendUtils.setAvatarState(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), _wctx.getMyId(), state);
        }
    }

    // PlayerSubControl
    protected function setAvatarMoveSpeed_v1 (
        targetId :int /* ignored */, pixelsPerSecond :Number) :void
    {
        validatePlayerTargetId(targetId);

        // todo make the below dispatch to all clients
        return;

        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setMoveSpeedFromUser(pixelsPerSecond);
        } else {
            BackendUtils.setAvatarMoveSpeed(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), _wctx.getMyId(), pixelsPerSecond);
        }
    }

    // PlayerSubControl
    protected function setAvatarLocation_v1 (
        targetId :int /* ignored */, x :Number, y :Number, z: Number, orient :Number) :void
    {
        validatePlayerTargetId(targetId);
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setLocationFromUser(x, y, z, orient);
        } else {
            BackendUtils.setAvatarLocation(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), _wctx.getMyId(), x, y, z, orient);
        }
    }

    // PlayerSubControl
    protected function setAvatarOrientation_v1 (
        targetId :int /* ignored */, orient :Number) :void
    {
        validatePlayerTargetId(targetId);

        // todo make the below dispatch to all clients
        return;

        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setOrientationFromUser(orient);
        } else {
            BackendUtils.setAvatarOrientation(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), _wctx.getMyId(), orient);
        }
    }

    // LocalSubControl
    protected function localChat_v1 (msg :String) :void
    {
        validateChat(msg);
        // The sendChat() messages will end up being routed through this method on each client.
        displayInfo(null, MessageBundle.taint(msg), ChatCodes.PLACE_CHAT_TYPE);
    }

    // LocalSubControl
    protected function getPaintableArea_v1 (full :Boolean) :Rectangle
    {
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        if (view != null) {
            if (full) {
                var bounds :Rectangle = _wctx.getPlaceViewBounds();
                return new Rectangle(0, 0, bounds.width, bounds.height);
            }
            return view.getScrollSize();
        }
        return null;
    }

    // RoomSubControl
    protected function getRoomBounds_v1 (targetId :int /* ignored */) :Rectangle
    {
        validateRoomTargetId(targetId);
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        if (view != null) {
            var metrics :RoomMetrics = view.layout.metrics;
            return new Rectangle(0, 0, metrics.sceneWidth, metrics.sceneHeight);
        }
        return null;
    }

    // LocalSubControl
    protected function stageToRoom_v1 (p :Point) :Point
    {
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        if (view != null) {
            p = _wctx.getTopPanel().getPlaceContainer().localToGlobal(p);
            p = view.globalToLocal(p);
            return p;
        }
        return null;
    }

    // LocalSubControl
    protected function roomToStage_v1 (p :Point) :Point
    {
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        if (view != null) {
            return _wctx.getTopPanel().getPlaceContainer().globalToLocal(view.localToGlobal(p));
        }
        return null;
    }

    // LocalSubControl
    protected function locationToRoom_v1 (x :Number, y :Number, z :Number)  :Point
    {
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        if (view != null) {
            return view.layout.locationToPoint(new MsoyLocation(x, y, z));
        }

        return null;
    }

    // LocalSubControl
    protected function stageToLocationAtDepth_v1 (p :Point, depth :Number) :Array
    {
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        if (view != null) {
            var msoyLoc :MsoyLocation = view.layout.pointToLocationAtDepth(p.x, p.y, depth);
            if (msoyLoc != null) {
                return [ msoyLoc.x, msoyLoc.y, msoyLoc.z ];
            }
        }

        return null;
    }

    // LocalSubControl
    protected function stageToLocationAtHeight_v1 (p :Point, height :Number) :Array
    {
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        if (view != null) {
            var msoyLoc :MsoyLocation = view.layout.pointToLocationAtHeight(p.x, p.y, height);
            if (msoyLoc != null) {
                return [ msoyLoc.x, msoyLoc.y, msoyLoc.z ];
            }
        }

        return null;
    }
    
    // LocalSubControl
    protected function showInvitePage_v1 (defmsg :String, token :String = "") :void
    {
        _gctx.showInvitePage(defmsg, token, getRoomId());
    }
    
    // LocalSubControl
    protected function getInviteToken_v1 () :String
    {
        return _gctx.getInviteToken();
    }
    
    // LocalSubControl
    protected function getInviterMemberId_v1 () :int
    {
        return _gctx.getInviterMemberId();
    }

    // LocalSubControl
    protected function getRoomBounds_vRay () :Array
    {
        var view :RoomView = _wctx.getPlaceView() as RoomView;
        return (view != null) ? view.getRoomBounds() : null;
    }

    // AgentSubControl
    protected function agent_sendMessage_v1 (name :String, value :Object) :void
    {
        BackendUtils.sendPrivateMessage(
            _gameObj.messageService, _gctx.getClient(), SERVER_AGENT_ID, name, value, "game");
    }

    // MobControl
    protected function setMobDecoration_v1 (
        mobId :String, decoration :DisplayObject, add :Boolean) :void
    {
        if (isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite != null) {
                if (add) {
                    sprite.addDecoration(decoration);
                } else {
                    sprite.removeDecoration(decoration);
                }
            }
        }
    }

    // MobControl
    protected function setMobHotSpot_v1 (
        mobId :String, x :Number, y :Number, height :Number = NaN) :void
    {
        if (isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite != null) {
                sprite.setHotSpot(x, y, height);
            }
        }
    }

//    // TODO: not used?
//    protected function getPlayerName_v1 (playerId :int) :String
//    {
//        var memInfo :MemberInfo = _ctrl.getRoom().getMemberInfo(playerId);
//        return (memInfo == null) ? null : memInfo.username.toString();
//    }

    /**
     * Displays an info message to the player. Default does nothing, so subclasses should
     * override.
     * TODO: candidate for code sharing with BaseGameBackend?
     */
    protected function displayInfo (bundle :String, msg :String, localType :String = null) :void
    {
        _wctx.displayInfo(bundle, msg, localType);
    }

    // internal utility method
    protected function getRoomId () :int
    {
        var scene :Scene = _wctx.getSceneDirector().getScene();
        return scene != null ? scene.getId() : 0;
    }

    // internal utility method
    protected function getMySprite () :MemberSprite
    {
        if (isPlaying()) {
            var view :RoomView = _wctx.getPlaceView() as RoomView;
            if (view != null) {
                return view.getMyAvatar();
            }
        }
        return null;
    }

    // internal utility method
    protected function getMobSprite (mobId :String) :MobSprite
    {
        var view :RoomObjectView = _wctx.getPlaceView() as RoomObjectView;
        return (view == null) ? null : view.getMob(_ctrl.getGameId(), mobId);
    }

    // internal utility method
    protected function getRoomView (func :String) :RoomObjectView
    {
        if (!isPlaying()) {
            log.debug(func + " while !isPlaying()");
            return null;
        }
        var view :RoomObjectView = _wctx.getPlaceView() as RoomObjectView;
        if (view == null) {
            log.debug(func + " without RoomView");
            return null;
        }
        return view;
    }

    // internal utility method
    protected function getRoomController (func :String) :RoomController
    {
        var view :RoomObjectView = getRoomView(func);
        if (view == null) {
            // reason already logged
            return null;
        }
        var ctrl :RoomController = view.getRoomController();
        if (ctrl == null) {
            log.debug(func + " without RoomController");
            return null;
        }
        return ctrl;
    }

    // internal utility method
    protected function getAvatarSprite (playerId :int) :MemberSprite
    {
        var func :String = "getAvatarSprite(" + playerId + ")";
        var view :RoomObjectView = getRoomView(func);
        if (view == null) {
            return null;
        }
        var roomObj :RoomObject = _ctrl.getRoom();
        if (roomObj == null) {
            log.debug(func + " without RoomObject");
            return null;
        }
        var occInfo :OccupantInfo = _gameObj.getOccupantInfo(new MemberName("", playerId));
        if (occInfo == null) {
            log.debug(func + ": player not in game");
            return null;
        }
        occInfo = roomObj.getOccupantInfo(occInfo.username);
        if (occInfo == null) {
            log.debug(func + ": player not in room");
            return null;
        }
        var sprite :OccupantSprite = view.getOccupant(occInfo.bodyOid);
        if (sprite == null) {
            log.debug(func + ": sprite not found");
            return null;
        }
        return sprite as MemberSprite;
    }

    /**
     * Verify that the supplied chat message is valid.
     */
    protected function validateChat (msg :String) :void
    {
        if (StringUtil.isBlank(msg)) {
            throw new ArgumentError("Empty chat may not be displayed.");
        }
    }

    protected function validatePlayerTargetId (targetId :int) :void
    {
        if (targetId != 0) {
            throw new Error("Unexpected player target [id=" + targetId + "]");
        }
    }

    protected function validateRoomTargetId (targetId :int) :void
    {
        if (targetId != 0) {
            throw new Error("Unexpected room target [id=" + targetId + "]");
        }
    }

    /**
     * Determine whether or not this player has access to some item or level pack.
     */
    protected function playerOwnsData (type :int, ident :String, playerId :int) :Boolean
    {
        return countPlayerData(type, ident, playerId) > 0;
    }

    /**
     * Returns the number of copies of the specified level or item pack owned by the player.
     */
    protected function countPlayerData (type :int, ident :String, playerId :int) :int
    {
        return _playerObj.countGameContent(_ctrl.getGameId(), type, ident);
    }

    protected function getPartyInfo_temp () :Object
    {
        var pobj :PartyObject = _wctx.getPartyDirector().getPartyObject();
        if (pobj == null) {
            return null;
        }

        var peeps :Array = pobj.peeps.toArray().map(function (peep :PartyPeep, ... rest) :int {
            return peep.name.getMemberId();
        });

        return { id: pobj.id, name: pobj.name, leaderId: pobj.leaderId, players: peeps };
    }

    // TEMP
    protected function handlePartyChanged (event :Event) :void
    {
        callUserCode("partyChanged_temp");
    }

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;

    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;

    protected var _gameNetAdapter :BackendNetAdapter;
    protected var _playerNetAdapter :BackendNetAdapter;
    protected var _roomPropsNetAdapter :BackendNetAdapter;
    protected var _avatarAdapter :BackendAvatarAdapter;
}
}
