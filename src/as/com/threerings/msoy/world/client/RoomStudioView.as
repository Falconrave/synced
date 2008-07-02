//
// $Id$

package com.threerings.msoy.world.client {

import flash.external.ExternalInterface;

import flash.system.Capabilities;
import flash.system.Security;

import flash.utils.ByteArray;

import mx.binding.utils.BindingUtils;
import mx.containers.HBox;
import mx.controls.HSlider;

import com.threerings.util.Log;
import com.threerings.util.MethodQueue;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.flash.MediaContainer;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.data.UberClientModes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.UberClient;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.data.ActorInfo;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * A non-network RoomView for testing avatars and other room entities.
 */
public class RoomStudioView extends RoomView
{
    /** Used to construct ItemIdents for the things we're testing. */
    public static const MEMBER_ID :int = 1;
    public static const PET_ID :int = 2;

    public function RoomStudioView (ctx :StudioContext, ctrl :RoomStudioController)
    {
        super(ctx, ctrl);

        _sctx = ctx;
    }

    /**
     * This method is needed for anything registered as a "Viewer" in world.mxml.
     */
    public function loadBytes (bytes :ByteArray) :void
    {
        _testingSprite.setZippedMediaBytes(bytes);
    }

    public function initForViewing (params :Object, uberMode :int) :void
    {
        (_ctrl as RoomStudioController).studioOnStage(uberMode);

        switch (uberMode) {
        case UberClientModes.AVATAR_VIEWER:
            initViewAvatar(params);
            break;

        case UberClientModes.PET_VIEWER:
            initViewPet(params);
            break;

        case UberClientModes.DECOR_VIEWER:
            initViewDecor(params);
            break;

        case UberClientModes.FURNI_VIEWER:
        case UberClientModes.TOY_VIEWER:
            initViewFurni(params);
            break;
        }
    }

    /**
     * Provide access to the pet we're previewing.
     */
    public function getPet () :PetSprite
    {
        return _pet;
    }

    override public function getMyAvatar () :MemberSprite
    {
        return _avatar;
    }

    public function doEntityMove (ident :ItemIdent, newLoc :MsoyLocation) :void
    {
        var sprite :OccupantSprite = _entities.get(ident) as OccupantSprite;
        if (sprite != null) {
            sprite.moveTo(newLoc, _scene);
        }
    }

    public function doAvatarMove (newLoc :MsoyLocation) :void
    {
        emulateIdle(false);
        _avatar.moveTo(newLoc, _scene);
    }

    public function setActorState (ident :ItemIdent, state :String) :void
    {
        var actor :ActorSprite = _entities.get(ident) as ActorSprite;
        if (actor == null) {
            Log.dumpStack();
            return;
        }
        var info :ActorInfo = actor.getActorInfo().clone() as ActorInfo;
        info.setState(state);
        info.status = OccupantInfo.ACTIVE; // un-idle, if needed
        actor.setOccupantInfo(info);
    }

    override public function dispatchSpriteMessage (
        item :ItemIdent, name :String, arg :ByteArray, isAction :Boolean) :void
    {
        // un-idle our avatar
        if (isAction && (_avatar == _entities.get(item))) {
            emulateIdle(false);
        }
        super.dispatchSpriteMessage(item, name, arg, isAction);
    }

    public function setAvatarScale (scale :Number) :void
    {
        var info :StudioMemberInfo = _avatar.getActorInfo().clone() as StudioMemberInfo;
        info.setScale(scale);
        info.status = OccupantInfo.ACTIVE; // while we're at it, un-idle
        _avatar.setOccupantInfo(info);
    }

    override public function setBackground (decor :Decor) :void
    {
        super.setBackground(decor);

        // if we're not specifically viewing a decor, show a wireframe decor
        if (UberClient.getMode() != UberClientModes.DECOR_VIEWER) {
            _backdrop.drawRoom(_bg.graphics, decor.width, decor.height, true, true);
        }
    }

    protected function initViewAvatar (params :Object) :void
    {
        var scale :Number = Number(params["scale"]);
        if (isNaN(scale) || scale == 0) {
            scale = 1;
        }

        // newstyle is that everything comes in on the "media" param, but let's still fall
        // back to "avatar" for a bit.
        var avatar :String = params["media"] || params["avatar"];
        var info :StudioMemberInfo = new StudioMemberInfo(_sctx, avatar);
        info.setScale(scale);
        _avatar = new MemberSprite(_ctx, info);
        addSprite(_avatar);
        _avatar.setEntering(new MsoyLocation(.1, 0, .25));
        _avatar.roomScaleUpdated();
        setCenterSprite(_avatar);
        _testingSprite = _avatar;

        var bar :ControlBar = _ctx.getTopPanel().getControlBar();
        bar.addCustomComponent(new CommandButton(Msgs.GENERAL.get("b.talk"), emulateChat));
        bar.addCustomComponent(new CommandButton(Msgs.GENERAL.get("b.idle"), emulateIdle));

        if ("true" == String(params["scaling"])) {
            createScaleControls(scale);
            _avatar.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
        }
    }

    protected function initViewPet (params :Object) :void
    {
        var pet :String = params["media"];
        var name :String = params["name"] || "Pet";
        var info :StudioPetInfo = new StudioPetInfo(name, pet);
        _pet = new PetSprite(_ctx, info);
        _pet.setEntering(new MsoyLocation(.1, 0, .25));
        addSprite(_pet);
        setCenterSprite(_pet);
        _testingSprite = _pet;

        addDefaultAvatar();
    }

    protected function initViewDecor (params :Object) :void
    {
        // the Backdrop media will be set all up in RoomStudioController
        _testingSprite = _bg;

        addDefaultAvatar();
    }

    protected function initViewFurni (params :Object) :void
    {
        var furni :FurniData = new FurniData();
        furni.id = _scene.getNextFurniId(0);
        furni.itemType = Item.FURNITURE;
        furni.itemId = 150;
        furni.media = new StudioMediaDesc(params["media"] as String);
        furni.loc = new MsoyLocation(0.5, 0, 0);

        _testingSprite = addFurni(furni);

        addDefaultAvatar();
    }

    protected function addDefaultAvatar () :void
    {
        var avatarPath :String;
        if (Security.sandboxType != Security.LOCAL_WITH_FILE) {
            avatarPath = Avatar.getDefaultMemberAvatarMedia().getMediaPath();
        } else {
            var url :String = this.root.loaderInfo.url;
            var fileSep :String = (-1 != Capabilities.os.indexOf("Windows")) ? "\\" : "/";
            var dex :int = url.lastIndexOf(fileSep);
            if (dex == -1) {
                avatarPath = "file:";
            } else {
                avatarPath = url.substring(0, dex + 1);
            }
            avatarPath += "default-avatar.swf";
        }
        _avatar = new MemberSprite(_ctx, new StudioMemberInfo(_sctx, avatarPath));
        _avatar.setEntering(new MsoyLocation(.1, 0, .25));
        addSprite(_avatar);
        setCenterSprite(_avatar);
    }

    protected function createScaleControls (scale :Number) :void
    {
        _scaleReset = new CommandButton(Msgs.GENERAL.get("b.resetScale"), function () :void {
            _scaleSlider.value = 1;
        });

        _scaleSlider = new HSlider();
        _scaleSlider.width = 140; // TODO: This is tiny! The scale slider mayhap needs a new UI?
        _scaleSlider.liveDragging = true;
        _scaleSlider.minimum = 0;
        _scaleSlider.maximum = int.MAX_VALUE;
        _scaleSlider.value = scale;
        _scaleSlider.enabled = false;
        _scaleSlider.tickValues = [ 1 ];
        BindingUtils.bindSetter(scaleUpdated, _scaleSlider, "value");

        var box :HBox = new HBox();
        box.percentHeight = 100;
        box.styleName = "controlBarSpacer";
        box.addChild(_scaleSlider);
        box.addChild(_scaleReset);
        _ctx.getTopPanel().getControlBar().addCustomComponent(box);
    }

    /**
     * Tell the avatar that it chatted.
     */
    protected function emulateChat () :void
    {
        emulateIdle(false);
        _avatar.performAvatarSpoke();
    }

    /**
     * Tell the avatar that it is idle.
     */
    protected function emulateIdle (idle :Boolean = true) :void
    {
        var info :StudioMemberInfo = _avatar.getActorInfo() as StudioMemberInfo;
        var newStatus :int = idle ? OccupantInfo.IDLE : OccupantInfo.ACTIVE;
        if (info.status == newStatus) {
            return; // our work here is done
        }
        info = info.clone() as StudioMemberInfo;
        info.status = newStatus;
        _avatar.setOccupantInfo(info);
    }

    /**
     * Handles the event when our viewer avatar's size is known.
     */
    protected function handleSizeKnown (event :ValueEvent) :void
    {
        var width :int = int(event.value[0]);
        var height :int = int(event.value[1]);

        // the minimum scale makes things 10 pixels in a dimension
        var minScale :Number = Math.max(10 / width, 10 / height);
        // the maximum bumps us up against the overall maximums
        var maxScale :Number = Math.min(OccupantSprite.MAX_WIDTH / width,
                                        OccupantSprite.MAX_HEIGHT / height);

        // but we always ensure that scale 1.0 is selectable, even if it seems it shouldn't be.
        _scaleSlider.minimum = Math.min(1, minScale);
        _scaleSlider.maximum = Math.max(1, maxScale);

        // enable everything
        _scaleSlider.enabled = true;
        scaleUpdated();
    }

    /**
     * Callback when the scale is updated in some way.
     */
    protected function scaleUpdated (... ignored) :void
    {
        var scale :Number = _scaleSlider.value;
        _scaleReset.enabled = (scale != 1);

        setAvatarScale(scale);

        if (ExternalInterface.available) {
            try {
                ExternalInterface.call("updateAvatarScale", scale);
            } catch (e :Error) {
                trace(e);
            }
        }
    }

    // from RoomView
    override protected function getZoom () :Number
    {
        return 1; // don't let a user's zoom pref be used in the viewer
    }

    protected var _sctx :StudioContext;

    protected var _testingSprite :MsoySprite;
    protected var _avatar :MemberSprite;
    protected var _pet :PetSprite;

    /** Used for sizing our own avatar. */
    protected var _scaleReset :CommandButton;
    protected var _scaleSlider :HSlider;

//    [Embed(source="../../../../../../../pages/media/static/avatar/member.swf",
//        mimeType="application/octet-stream")]
//    protected static const DEFAULT_AVATAR :Class;
}
}
