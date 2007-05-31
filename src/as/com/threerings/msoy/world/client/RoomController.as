//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.ContextMenuItem;
import flash.ui.Keyboard;
import flash.utils.ByteArray;

import mx.core.Application;
import mx.core.IChildList;
import mx.core.IToolTip;
import mx.core.UIComponent;
import mx.managers.ToolTipManager;
import mx.managers.ISystemManager;

import com.threerings.util.ClassUtil;
import com.threerings.util.Integer;
import com.threerings.util.NetUtil;
import com.threerings.util.ValueEvent;

import com.threerings.io.TypedArray;

import com.threerings.flash.MenuUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandMenu;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.presents.dobj.ChangeListener;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.ezgame.util.EZObjectMarshaller;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldClient;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.updates.FurniUpdateAction;
import com.threerings.msoy.world.client.updates.UpdateAction;
import com.threerings.msoy.world.client.updates.UpdateStack;
import com.threerings.msoy.world.client.editor.DoorTargetEditController;
import com.threerings.msoy.world.client.editor.RoomEditPanel;
import com.threerings.msoy.world.client.editor.EditorController;
import com.threerings.msoy.world.client.editor.FurniUsedDialog;

import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.SceneAttrsUpdate;
import com.threerings.msoy.world.data.WorldMemberInfo;
import com.threerings.msoy.world.data.WorldOccupantInfo;
import com.threerings.msoy.world.data.WorldPetInfo;

import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.chat.client.ReportingListener;

/**
 * Manages the various interactions that take place in a room scene.
 */
public class RoomController extends SceneController
{
    private const log :Log = Log.getLog(RoomController);

    public static const EDIT_DOOR :String = "EditDoor";
    public static const EDIT_CLICKED :String = "EditClicked";
    public static const FURNI_CLICKED :String = "FurniClicked";
    public static const AVATAR_CLICKED :String = "AvatarClicked";
    public static const PET_CLICKED :String = "PetClicked";

    public static const ORDER_PET :String = "OrderPet";

    /**
     * Get the instanceId of all the entity instances in the room.
     * This is used so that two instances of a pet can negotiate which
     * client will control it, for example.
     */
    public function getEntityInstanceId () :int
    {
        // every sprite uses our own OID as the instanceid.
        return _mctx.getMemberObject().getOid();
    }

    /**
     * Returns true if we are in edit mode, false if not.
     */
    public function isEditMode () :Boolean
    {
        // currently holding shift down puts us in edit mode, soon this will be based on whether or
        // not the hammer has been clicked
        return _shiftDown || isRoomEditing();
    }

    /**
     * Requests that this client be given control of the specified item.
     */
    public function requestControl (ident :ItemIdent) :void
    {
        if (_roomObj == null) {
            log.warning("Cannot request entity control, no room object [ident=" + ident + "].");
            return;
        }

        var result :Object = hasEntityControl(ident);
        // side-effect of calling hasEntityControl: the sprite
        // will be notified (possibly again) that it has control if it does
        if (result == null) {
            // only if nobody currently has control do we issue the request
            _roomObj.roomService.requestControl(_mctx.getClient(), ident);
        }
    }

    /**
     * Handles a request by an item in our room to send an "action" (requires control) or a
     * "message" (doesn't require control).
     */
    public function sendSpriteMessage (
        ident :ItemIdent, name :String, arg :Object, isAction :Boolean) :void
    {
        if (isAction && !checkCanRequest(ident, "triggerAction")) {
            log.info("Dropping message for lack of control [ident=" + ident +
                     ", name=" + name + "].");
            return;
        }

        // send the request off to the server
        log.info("Sending sprite message [ident=" + ident + ", name=" + name + "].");
        var data :ByteArray = (EZObjectMarshaller.encode(arg, false) as ByteArray);
        _roomObj.roomService.sendSpriteMessage(_mctx.getClient(), ident, name, data, isAction);
    }

    /**
     * Handles a request by an actor item to change its persistent state.  Requires control.
     */
    public function setActorState (ident :ItemIdent, actorOid :int, state :String) :void
    {
        if (!checkCanRequest(ident, "setState")) {
            log.info("Dropping state change for lack of control [ident=" + ident +
                     ", state=" + state + "].");
            return;
        }

        log.info("Changing actor state [ident=" + ident + ", state=" + state + "].");
        _roomObj.roomService.setActorState(_mctx.getClient(), ident, actorOid, state);
    }

    /**
     * Handles a request by an entity item to send a chat message.
     */
    public function sendPetChatMessage (msg :String, info :ActorInfo) :void
    {
        var svc :PetService = (_mctx.getClient().requireService(PetService) as PetService);
        if (checkCanRequest(info.getItemIdent(), "PetService")) {
            svc.sendChat(
                _mctx.getClient(), info.bodyOid, _scene.getId(), msg,
                new ReportingListener(_mctx));
        }
    }
    
    /**
     * Handles a request by an item in our room to update its memory.
     */
    public function updateMemory (ident :ItemIdent, key :String, value: Object) :Boolean
    {
        if (!checkCanRequest(ident, "updateMemory")) {
            return false;
        }

        // serialize datum (TODO: move this to somewhere more general purpose)
        var data :ByteArray = (EZObjectMarshaller.encode(value, false) as ByteArray);

        // TODO: total up item's used memory, ensure it doesn't exceed the allowed limit

        // ship the update request off to the server
        _roomObj.roomService.updateMemory(_mctx.getClient(), new MemoryEntry(ident, key, data));
        return true;
    }

    /**
     * Handles a request by an actor to change its location. Returns true if the request was
     * dispatched, false if funny business prevented it.
     */
    public function requestMove (ident :ItemIdent, newloc :MsoyLocation) :Boolean
    {
        if (!checkCanRequest(ident, "requestMove")) {
            return false;
        }
        _roomObj.roomService.changeLocation(_mctx.getClient(), ident, newloc);
        return true;
    }

    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _mctx = (ctx as WorldContext);
        // watch for when we're un-minimized and the display list is valid, so that we can
        // open the editor, and place things correctly when necessary
        _mctx.getTopPanel().getControlBar().addEventListener(ControlBar.DISPLAY_LIST_VALID, 
            function (evt :ValueEvent) :void {
                if (_openEditor) {
                    beginRoomEditing(_mctx.getTopPanel().getControlBar().roomEditBtn);
                }
                _openEditor = false;
            }
        );
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _roomView = new RoomView(ctx as WorldContext, this);
        return _roomView;
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);
        _roomListener = new MessageAdapter(msgReceivedOnRoomObj);
        _roomObj.addListener(_roomListener);

        // get a copy of the scene
        _scene = (_mctx.getSceneDirector().getScene() as MsoyScene);

        _walkTarget.visible = false;
        _flyTarget.visible = false;
        _roomView.addChildAt(_flyTarget, _roomView.numChildren);
        _roomView.addChildAt(_walkTarget, _roomView.numChildren);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _updates.reset();
        if (isRoomEditing()) {
            cancelRoomEditing();
        }

        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.removeChild(_walkTarget);
        _roomView.removeChild(_flyTarget);
        setHoverSprite(null);

        _roomObj.removeListener(_roomListener);

        _scene = null;
        _roomObj = null;

        closeAllMusic(false);

        super.didLeavePlace(plobj);
    }

    /**
     * Called to enact the avatar action globally: all users will see it.
     */
    public function doAvatarAction (action :String) :void
    {
        sendSpriteMessage(_roomView.getMyAvatar().getItemIdent(),
            action, null, true);
    }

    /**
     * Called to enact an avatar state change.
     */
    public function doAvatarState (state :String) :void
    {
        var avatar :AvatarSprite = _roomView.getMyAvatar();
        setActorState(avatar.getItemIdent(), avatar.getOid(), state);
    }

    /**
     * Close and reset all music.
     */
    protected function closeAllMusic (resumeBackground :Boolean) :void
    {
        if (_music != null && !(resumeBackground && _musicIsBackground)) {
            _music.close();
            _music = null;
            _musicIsBackground = true;
        }
        if (_loadingMusic != null) {
            _loadingMusic.close();
            _loadingMusic = null;
        }

        if (resumeBackground && _music == null) {
            setBackgroundMusic(_scene.getAudioData());
        }
    }

    /**
     * Handles EDIT_DOOR.
     */
    public function handleEditDoor (furniData :FurniData) :void
    {
        if (isRoomEditing()) {
            cancelRoomEditing();
        }

        _roomObj.roomService.editRoom(
            _mctx.getClient(), new ResultWrapper(
                function (cause :String) :void {
                    _mctx.displayFeedback("general", cause);
                },
                function (result :Object) :void {
                    DoorTargetEditController.start(furniData, _mctx);
                }));
    }

    /**
     * Handle the ROOM_EDIT command.
     */
    public function handleRoomEdit (button :CommandButton) :void
    {
        _roomObj.roomService.editRoom(
            _mctx.getClient(), new ResultWrapper(
                function (cause :String) :void {
                    _mctx.displayFeedback("general", cause);
                },
                function (result :Object) :void {
                    // if we're editing, let's finish, otherwise let's start!
                    if (isRoomEditing()) {
                        cancelRoomEditing();
                    } else {
                        beginRoomEditing(button);
                    }
                }));
    }

    /**
     * Returns true if we are in edit mode, false if not.
     */
    public function isRoomEditing () :Boolean
    {
        return _roomEditPanel != null && _roomEditPanel.isOpen;
    }

    /**
     * Handles EDIT_CLICKED.
     */
    public function handleEditClicked (sprite :MsoySprite) :void
    {
        var ident :ItemIdent = sprite.getItemIdent();
        if (ident == null || !(sprite is FurniSprite)) {
            return; // only furni sprites can be edited
        }

        if (isRoomEditing()) {
            return; // don't let editor v. 1 interfere with editor v. 2
        }

        var furni :FurniSprite = sprite as FurniSprite;
        var menuItems :Array = [];

        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            var menu :CommandMenu = CommandMenu.createMenu(menuItems);
            menu.setDispatcher(_roomView);
            menu.show();
        }
    }

    /**
     * Handles FURNI_CLICKED.
     */
    public function handleFurniClicked (furni :FurniData) :void
    {
        switch (furni.actionType) {
        case FurniData.ACTION_URL:
            NetUtil.navigateToURL(furni.actionData);
            return;

        case FurniData.ACTION_LOBBY_GAME:
        case FurniData.ACTION_WORLD_GAME:
            var actionData :Array = furni.splitActionData();
            var gameId :int = int(actionData[0]);
            postAction(furni.actionType == FurniData.ACTION_LOBBY_GAME ?
                MsoyController.JOIN_GAME_LOBBY : MsoyController.JOIN_WORLD_GAME, gameId);
            return;
            
        case FurniData.ACTION_PORTAL:
            _mctx.getSpotSceneDirector().traversePortal(furni.id);
            return;

        case FurniData.ACTION_HELP_PAGE:
            actionData = furni.splitActionData();
            var tabName :String = String(actionData[0]);
            var url :String = String(actionData[1]);
            (_mctx.getChatDirector() as MsoyChatDirector).displayPage(tabName, url);
            return;
            
        default:
            log.warning("Clicked on unhandled furni action type " +
                "[actionType=" + furni.actionType +
                ", actionData=" + furni.actionData + "].");
            return;
        }
    }

    /**
     * Handles AVATAR_CLICKED.
     */
    public function handleAvatarClicked (avatar :AvatarSprite) :void
    {
        var occInfo :MemberInfo = (avatar.getActorInfo() as MemberInfo);
        if (occInfo == null) {
            log.info("Clicked on non-MemberInfo sprite " +
                "[info=" + avatar.getActorInfo() + "].");
            return;
        }

        var us :MemberObject = _mctx.getMemberObject();
        var menuItems :Array = [];
        if (occInfo.bodyOid == us.getOid()) {
            if (_mctx.worldProps.userControlsAvatar) {
                // create a menu for clicking on ourselves
                var actions :Array = avatar.getAvatarActions();
                if (actions.length > 0) {
                    var worldActions :Array = [];
                    for each (var act :String in actions) {
                        worldActions.push({ label: act,
                            callback: doAvatarAction, arg: act });
                    }

                    menuItems.push({ label: Msgs.GENERAL.get("l.avAction"),
                        children: worldActions });
                }

                var states :Array = avatar.getAvatarStates();
                if (states.length > 0) {
                    var worldStates :Array = [];
                    for each (var state :String in states) {
                        worldStates.push({ label: state,
                            callback: doAvatarState, arg :state });
                    }

                    menuItems.push({ label: Msgs.GENERAL.get("l.avStates"),
                        children: worldStates });
                }
            }

        } else {
            // create a menu for clicking on someone else
            var memId :int = occInfo.getMemberId();
            var isGuest :Boolean = (memId == MemberName.GUEST_ID);
            var isFriend :Boolean = us.friends.containsKey(memId);
//            menuItems.push({ label: Msgs.GENERAL.get("b.tell"),
//                command: MsoyController.TELL, arg: memId });

            if (!isGuest) {
                menuItems.push(
                    { label: Msgs.GENERAL.get("b.visit_home"),
                      command: MsoyController.GO_MEMBER_HOME,
                      arg: memId },
                    { label: Msgs.GENERAL.get("b.view_member"),
                      command: MsoyController.VIEW_MEMBER,
                      arg: memId },
                    { label: Msgs.GENERAL.get(isFriend ? "b.removeAsFriend"
                                                       : "b.addAsFriend"),
                      command: MsoyController.ALTER_FRIEND,
                      arg: [memId, !isFriend] });
            }
        }

        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            var menu :CommandMenu = CommandMenu.createMenu(menuItems);
            menu.setDispatcher(_roomView);
            menu.show();
        }
    }

    /**
     * Handles PET_CLICKED.
     */
    public function handlePetClicked (pet :ActorSprite) :void
    {
        var occInfo :WorldPetInfo = (pet.getActorInfo() as WorldPetInfo);
        if (occInfo == null) {
            log.warning("Pet has unexpected ActorInfo [info=" + pet.getActorInfo() + "].");
            return;
        }

        var petId :int = occInfo.getItemIdent().itemId;
        var menuItems :Array = [];

        // TODO: check for pet ownership, etc.
        menuItems.push(
//         { label: Msgs.GENERAL.get("b.order_pet_stay"),
//           command: ORDER_PET, arg: [ petId, Pet.ORDER_STAY ] },
//         { label: Msgs.GENERAL.get("b.order_pet_follow"),
//           command: ORDER_PET, arg: [ petId, Pet.ORDER_FOLLOW ] },
//         { label: Msgs.GENERAL.get("b.order_pet_go_home"),
//           command: ORDER_PET, arg: [ petId, Pet.ORDER_GO_HOME ] },
        { label: Msgs.GENERAL.get("b.order_pet_sleep"),
          command: ORDER_PET, arg: [ petId, Pet.ORDER_SLEEP ] }
        );

        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            var menu :CommandMenu = CommandMenu.createMenu(menuItems);
            menu.setDispatcher(_roomView);
            menu.show();
        }
    }

    /**
     * Handles ORDER_PET.
     */
    public function handleOrderPet (petId :int, command :int) :void
    {
        var svc :PetService = (_mctx.getClient().requireService(PetService) as PetService);
        svc.orderPet(_mctx.getClient(), petId, command,
                     new ReportingListener(_mctx, "general", null, "m.pet_ordered" + command));
    }

    /**
     * Get the top-most sprite mouse-capturing sprite with a non-transparent
     * pixel at the specified location.
     *
     * @return undefined if the mouse isn't in our bounds, or null, or an MsoySprite.
     */
    public function getHitSprite (
        stageX :Number, stageY :Number, all :Boolean = false) :*
    {
        var smgr :ISystemManager = Application.application.systemManager as ISystemManager;
        var ii :int;
        var disp :DisplayObject;
        for (ii = smgr.numChildren - 1; ii >= 0; ii--) {
            disp = smgr.getChildAt(ii)
            if (disp is Application) {
                continue;
            }
            if (disp.hitTestPoint(stageX, stageY)) {
                return undefined;
            }
        }
//        var popups :IChildList = smgr.popUpChildren;
//        for (ii = popups.numChildren - 1; ii >= 0; ii--) {
//            disp = popups.getChildAt(ii);
//            trace("Checkingz: " + disp + " : " + ClassUtil.getClassName(disp));
//            if (disp.hitTestPoint(stageX, stageY)) {
//                return undefined;
//            }
//        }

        // we search from last-drawn to first drawn to get the topmost...
        for (var dex :int = _roomView.numChildren - 1; dex >= 0; dex--) {
            var spr :MsoySprite = (_roomView.getChildAt(dex) as MsoySprite);
            if ((spr != null) && (all || (spr.isActive() && spr.capturesMouse())) &&
                spr.hitTestPoint(stageX, stageY, true))
            {
                return spr;
            }
        }

        return null;
    }

    /**
     * This is called from javascript to add a piece of furni to the room from the inventory
     * browsing interface.
     */
    public function addFurni (itemId :int, itemType :int) :void
    {
        var scene :MsoyScene = _mctx.getSceneDirector().getScene() as MsoyScene;
        if (scene == null || !scene.canEdit(_mctx.getMemberObject())) {
            _mctx.displayInfo("editing", "e.no_permission");
        } else {
            _openEditor = true;
            (new InventoryAction(itemType, _mctx)).trigger(
                // closure to add the new furni once this item type has been loaded on this
                // user's MemberObject
                function (furniId :int, furniType :int) :Function {
                    return function () :void {
                        var item :Item = null;
                        for each (var checkItem :Item in 
                            _mctx.getMemberObject().getItems(furniType)) {
                            if (checkItem.itemId == furniId) {
                                item = checkItem;
                                break;
                            }
                        }
                        if (item == null) {
                            // didn't find the item
                            return;
                        }
                
                        var addToRoom :Function = function () :void {
                            // create a generic furniture descriptor
                            var furni :FurniData = new FurniData();
                            furni.id = _scene.getNextFurniId(0);
                            furni.itemType = item.getType();
                            furni.itemId = item.itemId;
                            furni.media = item.getFurniMedia();
                            // create it at the front of the scene, centered on the floor
                            furni.loc = new MsoyLocation(0.5, 0, 0);
                            if (item is Game) {
                                var game :Game = (item as Game);
                                furni.actionType = game.isInWorld() ?
                                    FurniData.ACTION_WORLD_GAME : FurniData.ACTION_LOBBY_GAME;
                                furni.actionData = String(game.getPrototypeId()) + ":" + game.name;
                            }
                            applyUpdate(new FurniUpdateAction(_mctx, null, furni));
                        };

                        if (item.isUsed()) {
                            (new FurniUsedDialog(_mctx, function () :void {
                                var confWrap :ConfirmAdapter = new ConfirmAdapter(
                                    // failure function
                                    function (cause :String) :void {
                                        Log.getLog(this).debug(
                                            "Failed to remove item from its current location [id=" +
                                            item.itemId + ", type=" + item.getType() + 
                                            ", cause=" + cause);
                                        _mctx.displayInfo("editing", "e.failed_to_remove");
                                    },
                                    // success function
                                    function () :void {
                                        addToRoom();
                                    });
                                (_mctx.getClient().requireService(ItemService) as ItemService).
                                    reclaimItem(_mctx.getClient(), new ItemIdent(item.getType(), 
                                        item.itemId), confWrap);
                            })).open(true);
                        } else {
                            addToRoom();
                        }
                    }
                }(itemId, itemType));
        }
    }

    /**
     * Begin editing the room.
     */
    protected function beginRoomEditing (button :CommandButton) :void
    {
        _walkTarget.visible = false;
        _flyTarget.visible = false;
        setHoverSprite(null);

        button.selected = true;

        // this function will be called when the edit panel is closing
        var wrapupFn :Function = function () :void {
            _roomEditPanel = null;
            // re-start any music
            if (_music != null) {
                _music.play();
            }
            button.selected = false;
        }

        if (_music != null && ! _musicIsBackground) {
            _music.close();
            _music = null;
            _musicIsBackground = true;
        }
        
        _roomEditPanel = new RoomEditPanel(_mctx, button, _roomView, wrapupFn);
        _roomEditPanel.open(false, null, button);
        _roomEditPanel.updateUndoButton(_updates.length != 0);
    }

    /**
     * End editing the room.
     */
    public function cancelRoomEditing () :void
    {
        _roomEditPanel.close();
    }

    /**
     * Applies a specified room update object to the current room.
     */
    public function applyUpdate (update :UpdateAction) :void
    {
        _updates.push(update);
    }

    /**
     * Undo the effects of the most recent update. Returns true if the update stack contains more
     * actions, false if it's become empty.
     */
    public function undoLastUpdate () :Boolean
    {
        _updates.pop();
        return _updates.length != 0;
    }

    /**
     * Sends the entire array of room edits to the server.
     *  @param updates a TypedArray containing instances of SceneUpdate object.
     */
    protected function updateRoom (updates :TypedArray /* of SceneUpdate */) :void
    {
        _roomObj.roomService.updateRoom(_mctx.getClient(), updates, new ReportingListener(_mctx));
    }

    /**
     * Handle ENTER_FRAME and see if the mouse is now over anything.
     * Normally the flash player will dispatch mouseOver/mouseLeft
     * for an object even if the mouse isn't moving: the sprite could move.
     * Since we're hacking in our own mouseOver handling, we emulate that.
     * Gah.
     */
    protected function checkMouse (event :Event) :void
    {
        var sx :Number = _roomView.stage.mouseX;
        var sy :Number = _roomView.stage.mouseY;
        var showWalkTarget :Boolean = false;
        var showFlyTarget :Boolean = false;
        var hoverTarget :MsoySprite = null;
        
        if (isRoomEditing()) {
            _roomEditPanel.controller.mouseMove(sx, sy);
        }
        
        // if shift is being held down, we're looking for locations only, so
        // skip looking for hitSprites.
        var hit :* = (_shiftDownSpot == null) ? getHitSprite(sx, sy, isRoomEditing()) : null;
        var hitter :MsoySprite = (hit as MsoySprite);
        // ensure we hit no pop-ups
        if (hit !== undefined) {
            hoverTarget = hitter;
            if (hitter == null) {
                var cloc :ClickLocation = _roomView.layout.pointToAvatarLocation(
                    sx, sy, _shiftDownSpot, RoomMetrics.N_UP);
                
                if (cloc != null && _mctx.worldProps.userControlsAvatar) {
                    addAvatarYOffset(cloc);
                    if (cloc.loc.y != 0) {
                        _flyTarget.setLocation(cloc.loc);
                        _roomView.layout.updateScreenLocation(_flyTarget);
                        showFlyTarget = true;

                        // Set the Y to 0 and use it for the walkTarget
                        cloc.loc.y = 0;
                        _walkTarget.alpha = .5;

                    } else {
                        _walkTarget.alpha = 1;
                    }

                    // don't show the walk target if we're "in front" of the room view
                    showWalkTarget = (cloc.loc.z >= 0);
                    _walkTarget.setLocation(cloc.loc);
                    _roomView.layout.updateScreenLocation(_walkTarget);
                }

            } else if (!hoverTarget.hasAction()) {
                // it may have captured the mouse, but it doesn't actually
                // have any action, so we don't hover it.
                hoverTarget = null;
            }
            
            // if we're editing the room, don't highlight any furni at all,
            if (isRoomEditing()) {
                hoverTarget = null;
                
                // let the editor override our decision to display walk targets
                showWalkTarget = (showWalkTarget && _roomEditPanel.isMovementEnabled);
                showFlyTarget = (showFlyTarget && _roomEditPanel.isMovementEnabled);
                
                // and tell the editor which sprite was being hovered (whether highlighted or not)
                _roomEditPanel.controller.mouseOverSprite(hitter); 
            }
        }

        _walkTarget.visible = showWalkTarget;
        _flyTarget.visible = showFlyTarget;

        setHoverSprite(hoverTarget, sx, sy);
    }

    /**
     * Set the sprite that the mouse is hovering over.
     */
    protected function setHoverSprite (
        sprite :MsoySprite, stageX :Number = 0, stageY :Number = 0) :void
    {
        // if the same sprite is glowing, we don't have to change as much..
        if (_hoverSprite == sprite) {
            updateHovered(stageX, stageY);
            if (_hoverSprite != null) {
                // but we do want to tell it about it, in case it wants
                // to glow differently depending on the location...
                _hoverSprite.setHovered(true, stageX, stageY);
            }
            return;
        }

        // otherwise, unglow the old sprite (and remove any tooltip)
        if (_hoverSprite != null) {
            _hoverSprite.setHovered(false);
            if (_hoverTip != null) {
                ToolTipManager.destroyToolTip(_hoverTip);
                _hoverTip = null;
            }
        }

        // assign the new hoversprite
        _hoverSprite = sprite;

        // and glow the new hoversprite
        updateHovered(stageX, stageY);
    }

    /**
     * Update the hovered status of the current _hoverSprite.
     */
    protected function updateHovered (stageX :Number, stageY :Number) :void
    {
        if (_hoverSprite == null) {
            return;
        }

        var text :String = _hoverSprite.setHovered(true, stageX, stageY);
        if (_hoverTip != null && _hoverTip.text != text) {
            ToolTipManager.destroyToolTip(_hoverTip);
            _hoverTip = null;
        }
        if (_hoverTip == null && text != null) {
            _hoverTip = ToolTipManager.createToolTip(text,
                stageX, stageY);
            var tipComp :UIComponent = UIComponent(_hoverTip);
            tipComp.styleName = "roomToolTip";
            var hoverColor :uint = _hoverSprite.getHoverColor();
            tipComp.setStyle("color", hoverColor);
            if (hoverColor == 0) {
                tipComp.setStyle("backgroundColor", 0xFFFFFF);
            }
        }
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        // if shift is being held down, we're looking for locations only, so
        // skip looking for hitSprites.
        var hit :* = (_shiftDownSpot == null) ?
            getHitSprite(event.stageX, event.stageY, isRoomEditing()) : null;
        
        if (hit === undefined) {
            return;
        }

        var hitter :MsoySprite = (hit as MsoySprite);
        
        // deal with the target
        if (hitter != null) {
            // let the sprite decide what to do with it
            hitter.mouseClick(event);
        } else if (_mctx.worldProps.userControlsAvatar) {
            var curLoc :MsoyLocation = _roomView.getMyCurrentLocation();
            if (curLoc == null) {
                return; // we've already left, ignore the click
            }

            // calculate where the location is
            var cloc :ClickLocation = _roomView.layout.pointToAvatarLocation(
                event.stageX, event.stageY, _shiftDownSpot, RoomMetrics.N_UP);
            
            if (cloc != null &&
                cloc.loc.z >= 0) { // disallow clicking in "front" of the scene when minimized
                // orient the location as appropriate
                addAvatarYOffset(cloc);
                var newLoc :MsoyLocation = cloc.loc;
                var degrees :Number = 180 / Math.PI *
                    Math.atan2(newLoc.z - curLoc.z, newLoc.x - curLoc.x);
                // we rotate so that 0 faces forward
                newLoc.orient = (degrees + 90 + 360) % 360;
                _mctx.getSpotSceneDirector().changeLocation(newLoc, null);
            }
        }

        // and in any case, tell the editor
        if (isRoomEditing()) {
            _roomEditPanel.controller.mouseClick(hitter, event);
        }

    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        try {
            var keyDown :Boolean = event.type == KeyboardEvent.KEY_DOWN;
            switch (event.keyCode) {
            case Keyboard.F4:
                _roomView.dimAvatars(keyDown);
                return;

            case Keyboard.F5:
                _roomView.dimFurni(keyDown);
                return;

            case Keyboard.F6:
                _roomView.chatOverlay.setClickableGlyphs(keyDown);
                return;

            case Keyboard.SHIFT:
                _shiftDown = keyDown;
                if (keyDown) {
                    if (_walkTarget.visible && (_shiftDownSpot == null)) {
                        // record the y position at this
                        _shiftDownSpot = new Point(_roomView.stage.mouseX, _roomView.stage.mouseY);
                    }

                } else {
                    _shiftDownSpot = null;
                }
            }

            if (keyDown) {
                switch (event.charCode) {
                case 91: // '['
                    _roomView.scrollViewBy(-ROOM_SCROLL_INCREMENT);
                    break;

                case 93: // ']'
                    _roomView.scrollViewBy(ROOM_SCROLL_INCREMENT);
                    break;
                }
            }

        } finally {
            event.updateAfterEvent();
        }
    }

    /**
     * Return the avatar's preferred y offset for normal mouse positioning,
     * unless shift is being held down, in which case use 0 so that the user
     * can select their height precisely.
     */
    protected function addAvatarYOffset (cloc :ClickLocation) :void
    {
        if (_shiftDownSpot != null) {
            return;
        }
        
        var av :AvatarSprite = _roomView.getMyAvatar();
        if (av != null) {
            var prefY :Number = av.getPreferredY() / _roomView.layout.metrics.sceneHeight;
            // but clamp their preferred Y into the normal range
            cloc.loc.y = Math.min(1, Math.max(0, prefY));
        }
    }

    /**
     * Called when a message is received on the room object.
     */
    protected function msgReceivedOnRoomObj (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomObject.LOAD_MUSIC:
            if (_loadingMusic != null) {
                _loadingMusic.close();
            }
            _loadingMusic = new SoundPlayer(String(args[0]));
            // TODO: dispatched MUSIC_LOADED back...
            break;

        case RoomObject.PLAY_MUSIC:
            if (args == null || args.length == 0) {
                closeAllMusic(true);
                break;
            }
            var url :String = (args[0] as String);
            if (_loadingMusic != null) {
                if (_loadingMusic.getURL() == url) {
                    // awesome
                    if (_music != null) {
                        _music.close();
                    }
                    _music = _loadingMusic;
                    _loadingMusic = null;
                    _musicIsBackground = false;
                    _music.addEventListener(Event.COMPLETE, musicFinishedPlaying);
                    _music.play();

                } else {
                    log.warning("Asked to play music different from loaded? " +
                        "[loaded=" + _loadingMusic.getURL() +
                        ", toPlay=" + url + "].");
                }
            }
            break;

        case RoomObject.ADD_EFFECT:
            addTransientEffect(args[0] as int /*bodyOid*/, args[1] as EffectData);
            break;
        }
    }

    public function setBackgroundMusic (data :AudioData) :void
    {
        if (!_musicIsBackground) {
            if (_music.isPlaying()) {
                // don't disrupt the other music..
                return;

            } else {
                // oh, this other music is done. Sure, let's go for
                // the background music again
                _music.close();
                _music = null;
                _musicIsBackground = true;
            }
        }

        var isPathValid :Boolean = data.isInitialized() && data.media != null;
        var path :String = isPathValid ? data.media.getMediaPath() : null;
        
        // maybe shutdown old music
        // if _music is playing the right thing, let it keep on playing
        if (_music != null && _music.getURL() != path) {
            _music.close();
            _music = null;
        }
        // set up new music, if needed
        if (_music == null && isPathValid) {
            _music = new SoundPlayer(path);
            _music.addEventListener(Event.COMPLETE, musicFinishedPlaying);
            // TODO: we probably need to wait for COMPLETE
            _music.loop();
        }
        // set the volume, even if we're just re-setting it on
        // already-playing music
        if (_music != null) {
            _music.setVolume(data.volume);
        }
    }

    /**
     * Do any needed clientside adjustments to the effect data.
     */
    public function adjustEffectData (effect :EffectData) :EffectData
    {
        switch (effect.actionType) {
        default:
            log.warning("Unhandled EffectData parameter mode: " + effect.actionType);
            Log.dumpStack();
            // fall through to MODE_NONE...

        case EffectData.MODE_NONE:
            return effect;

        case EffectData.MODE_XLATE:
            effect.actionData = Msgs.GENERAL.xlate(effect.actionData);
            break;
        }

        // set the mode to MODE_NONE to indicate that we've adjusted
        effect.actionType = EffectData.MODE_NONE;
        return effect;
    }

    /**
     * Callback when the music finishes.
     */
    protected function musicFinishedPlaying (... ignored) :void
    {
        _roomObj.manager.invoke(RoomObject.MUSIC_ENDED, _music.getURL());
    }

    /**
     * Add a transient effect to an actor sprite.
     */
    protected function addTransientEffect (bodyOid :int, effect :EffectData) :void
    {
        var actor :ActorSprite = _roomView.getActor(bodyOid);
        if (actor != null) {
            actor.addTransientEffect(adjustEffectData(effect));

        } else {
            log.info("Unable to find actor for transient effect [bodyOid=" + bodyOid + "].");
        }
    }

    /**
     * Ensures that we can issue a request to update the distributed state of the specified item,
     * returning true if so, false if we don't yet have a room object or are not in control of that
     * item.
     */
    protected function checkCanRequest (ident :ItemIdent, from :String) :Boolean
    {
        if (_roomObj == null) {
            log.warning("Cannot issue request for lack of room object [from=" + from +
                        ", ident=" + ident + "].");
            return false;
        }

        // make sure we are in control of this entity (or that no one has control)
        var result :Object = hasEntityControl(ident);
        if (result == null || result == true) {
            // it's ok if nobody has control
            return true;
        }

        log.info("Dropping request as we are not controller [from=" + from +
                 ", item=" + ident + "].");
        return false;
    }

    /**
     * Does this client have control over the specified entity?
     *
     * Side-effect: The gotControl() will always be re-dispatched to the entity if it does.
     * The newest EntityControl will suppress repeats.
     *
     * @returns true, false, or null if nobody currently has control.
     */
    protected function hasEntityControl (ident :ItemIdent) :Object
    {
        var ourOid :int = _mctx.getMemberObject().getOid();

        // first, let's check all the WorldMemberInfos
        for each (var occInfo :Object in _roomObj.occupantInfo.toArray()) {
            if (occInfo is WorldMemberInfo) {
                var winfo :WorldMemberInfo = (occInfo as WorldMemberInfo);
                if (ident.equals(winfo.getItemIdent())) {
                    if (winfo.bodyOid == ourOid) {
                        // dispatch got-control to the avatar, it should
                        // supress repeats
                        _roomView.dispatchGotControl(ident);
                        return true;

                    } else {
                        return false; // we can't control another's avatar!
                    }
                }
            }
        }
        // ok, the ident does not belong to a member's avatar..

        var ctrl :EntityControl = (_roomObj.controllers.get(ident) as EntityControl);
        if (ctrl == null) {
            return null;

        } else if (ctrl.controllerOid == ourOid) {
            // redispatch that we have control, just in case the media
            // started up after the last dispatch...
            _roomView.dispatchGotControl(ident);
            return true;

        } else {
            return false;
        }
    }

    override protected function sceneUpdated (update :SceneUpdate) :void
    {
        super.sceneUpdated(update);
        _roomView.processUpdate(update);
    }
    
    /** The number of pixels we scroll the room on a keypress. */
    protected static const ROOM_SCROLL_INCREMENT :int = 20;

    /** The life-force of the client. */
    protected var _mctx :WorldContext;

    /** The room view that we're controlling. */
    protected var _roomView :RoomView;

    protected var _roomObj :RoomObject;

    /** Our general-purpose room listener. */
    protected var _roomListener :ChangeListener;

    protected var _hoverSprite :MsoySprite;

    protected var _hoverTip :IToolTip;

    /** True if the shift key is currently being held down, false if not. */
    protected var _shiftDown :Boolean;

    /** If shift is being held down, the coordinates at which it was pressed. */
    protected var _shiftDownSpot :Point;

    /** The music currently playing in the scene, which may or may not be
     * background music. */
    protected var _music :SoundPlayer;

    /** True if _music is the room's background music. Otherwise
     * The music playing is from some other source. */
    protected var _musicIsBackground :Boolean = true;

    /** Holds loading alternate music. Once triggered to play,
     * it's shifted to _music. */
    protected var _loadingMusic :SoundPlayer;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** The "cursor" used to display that a location is walkable. */
    protected var _walkTarget :WalkTarget = new WalkTarget();

    protected var _flyTarget :WalkTarget = new WalkTarget(true);

    /** Panel for in-room furni editing. */
    protected var _roomEditPanel :RoomEditPanel; 

    /** Stack that stores the sequence of room updates. */
    protected var _updates :UpdateStack = new UpdateStack(updateRoom);

    /** A flag to indicate that the room editor should be opened when the view is un-minimized */
    protected var _openEditor :Boolean = false;
}
}

import flash.display.DisplayObject;
import flash.display.Sprite;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.world.client.RoomElement;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;

class WalkTarget extends Sprite
    implements RoomElement
{
    public function WalkTarget (fly :Boolean = false)
    {
        var targ :DisplayObject = (fly ? new FLYTARGET() : new WALKTARGET() as DisplayObject);
        targ.x = -targ.width/2;
        targ.y = -targ.height/2;
        addChild(targ);
    }

    // from RoomElement
    public function setLocation (newLoc :Object) :void
    {
        _loc.set(newLoc);
    }

    // from RoomElement
    public function getLocation () :MsoyLocation
    {
        return _loc;
    }

    // from RoomElement
    public function getLayoutType () :int
    {
        return RoomCodes.LAYOUT_NORMAL;
    }

    // from RoomElement
    public function getRoomLayer () :int
    {
        return RoomCodes.FURNITURE_LAYER;
    }

    // from RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        this.x = x
        this.y = y

        // don't let the target shrink too much - 0.25 of original size at most
        var clampedScale :Number = Math.max(0.25, scale);
        this.scaleX = clampedScale;
        this.scaleY = clampedScale;
    }

    /** Our logical location. */
    protected const _loc :MsoyLocation = new MsoyLocation();

    [Embed(source="../../../../../../../rsrc/media/walkable.swf")]
    protected static const WALKTARGET :Class;

    [Embed(source="../../../../../../../rsrc/media/flyable.swf")]
    protected static const FLYTARGET :Class;
}

class InventoryAction 
    implements AttributeChangeListener
{
    public function InventoryAction (itemType :int, mctx :WorldContext) 
    {
        _itemType = itemType;
        _mctx = mctx;
    }

    public function trigger (callback :Function) :void
    {
        _callback = callback;
        var member :MemberObject = _mctx.getMemberObject();
        if (member.isInventoryLoaded(_itemType)) {
            _callback();
        } else {
            member.addListener(this);
            _mctx.getItemDirector().loadInventory(_itemType);
        }
    }

    // from AttributeChangeListener
    public function attributeChanged (evt :AttributeChangedEvent) :void
    {
        var member :MemberObject = _mctx.getMemberObject();
        if (evt.getName() == MemberObject.LOADED_INVENTORY && member.isInventoryLoaded(_itemType)) {
            member.removeListener(this);
            _callback();
        }
    }

    protected var _mctx :WorldContext;
    protected var _itemType :int;
    protected var _callback :Function;
}
