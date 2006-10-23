package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;

import mx.collections.ListCollectionView;

import mx.containers.VBox;
import mx.containers.ViewStack;

import mx.controls.Button;
import mx.controls.ComboBox;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.core.UIComponent;

import com.threerings.util.ArrayUtil;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.data.FurniData;


public class FurniPanel extends SpritePanel
{
    override public function updateInputFields () :void
    {
        super.updateInputFields();

        _xScale.text = String(_sprite.getMediaScaleX());
        _yScale.text = String(_sprite.getMediaScaleY());

        var furni :FurniData = (_sprite as FurniSprite).getFurniData();
        updateActionType(furni);

        switch (furni.actionType) {
        case FurniData.ACTION_PORTAL:
            updatePortal(furni);
            break;

        case FurniData.ACTION_URL:
            _url.text = furni.actionData;
            break;
        }

        // TEMP: update the show-all/edit-all control
        _actionData.text = furni.actionData;
    }

    protected function updateActionType (furni :FurniData) :void
    {
        var data :Object = _actionType.dataProvider;
        for (var ii :int = 0; ii < data.length; ii++) {
            if (data[ii].data == furni.actionType) {
                _actionType.selectedIndex = ii;
                if (furni.actionType == FurniData.BACKGROUND) {
                    _centering.selected = false;
                }
                return;
            }
        }
    }

    protected function updatePortal (furni :FurniData) :void
    {
        var vals :Array = furni.splitActionData();
        var targetSceneId :int = int(vals[0]);

//        _destPortal.text = String(targetPortalId);

        var data :Object = _destScene.dataProvider;
        for (var ii :int = 0; ii < data.length; ii++) {
            var o :Object = data[ii];
            if (o is SceneBookmarkEntry) {
                var sbe :SceneBookmarkEntry = (o as SceneBookmarkEntry);
                if (sbe.sceneId === targetSceneId) {
                    _destScene.selectedIndex = ii;
                    return;
                }
            } else if (targetSceneId === o) {
                _destScene.selectedIndex = ii;
                return;
            }
        }

        // never found. setting _destScene.text should work, but it
        // doesn't, so add the scene to the dataprovider..
        ListCollectionView(_destScene.dataProvider).addItem(targetSceneId);
        _destScene.selectedIndex = data.length;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // scale
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.xscale")),
            _xScale = new TextInput());
        MsoyUI.enforceNumber(_xScale);
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.yscale")),
            _yScale = new TextInput());
        MsoyUI.enforceNumber(_yScale);

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.action")),
            _actionType = new ComboBox());
        _actionType.dataProvider = [
            { label: _ctx.xlate("editing", "l.action_none"),
              data: FurniData.ACTION_NONE },
            { label: _ctx.xlate("editing", "l.background"),
              data: FurniData.BACKGROUND },
            { label: _ctx.xlate("editing", "l.action_game"),
              data: FurniData.ACTION_GAME },
            { label: _ctx.xlate("editing", "l.action_url"),
              data: FurniData.ACTION_URL },
            { label: _ctx.xlate("editing", "l.action_portal"),
              data: FurniData.ACTION_PORTAL }
        ];

        _actionPanels = new ViewStack();
        _actionPanels.addChild(new VBox()); // ACTION_NONE
        _actionPanels.addChild(new VBox()); // BACKGROUND (nothing to edit) TODO
        _actionPanels.addChild(new VBox()); // ACTION_GAME (nothing to edit)
        _actionPanels.addChild(createURLEditor()); // ACTION_URL
        _actionPanels.addChild(createPortalEditor()); // ACTION_PORTAL
        addRow(_actionPanels, [2, 1]);

        BindingUtils.bindProperty(_actionPanels, "selectedIndex",
            _actionType, "selectedIndex");

        // BEGIN temporary controls
        var lbl :Label;
        var btn :Button = new Button();
        btn.label = "perspective?";
        btn.addEventListener(MouseEvent.CLICK,
            function (evt :MouseEvent) :void {
                (_sprite as FurniSprite).addPersp();
            });
        addRow(
            lbl = MsoyUI.createLabel("testing:"),
            btn);
        lbl.setStyle("color", 0xFF0000);

        // add an "expert control" for directly editing the action
        addRow(
            lbl = MsoyUI.createLabel(_ctx.xlate("editing", "l.action")),
            _actionData = new TextInput());
        lbl.setStyle("color", 0xFF0000);
        // END: temporary things
    }

    protected function createURLEditor () :UIComponent
    {
        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.url")),
            _url = new TextInput());
        return grid;
    }

    protected function createPortalEditor () :UIComponent
    {
        var grid :Grid = new Grid();
        grid.addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_scene")),
            _destScene = new ComboBox());
        _destScene.editable = true;

        // combine recent and owned scenes into one array
        var recent :Array = _ctx.getClientObject().recentScenes.toArray();
        var owned :Array = _ctx.getClientObject().ownedScenes.toArray();
        var scenes :Array = recent.concat();
        for each (var sbe :SceneBookmarkEntry in owned) {
            if (!ArrayUtil.contains(recent, sbe)) {
                scenes.push(sbe);
            }
        }
        _destScene.dataProvider = scenes;

//        grid.addRow(
//            MsoyUI.createLabel(_ctx.xlate("editing", "l.dest_portal")),
//            _destPortal = new TextInput());

        return grid;
    }

    override protected function bind () :void
    {
        super.bind();

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setMediaScaleX(val);
                spritePropsUpdated();
            }
        }, _xScale, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setMediaScaleY(val);
                spritePropsUpdated();
            }
        }, _yScale, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            var item :Object = _actionType.selectedItem;
            furni.actionType = int(item.data);

            // force the sprite to recheck props, so that it re-reads
            // whether it's a background
            (_sprite as FurniSprite).update(_ctx, furni);

            spritePropsUpdated();
        }, _actionType, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            furni.actionData = String(o);
            spritePropsUpdated();
        }, _actionData, "text");

        BindingUtils.bindSetter(function (url :String) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            if (furni.actionType != FurniData.ACTION_URL) {
                return; // don't update if we shouldn't
            }
            furni.actionData = url;
            spritePropsUpdated();
        }, _url, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                return; // don't update if we shouldn't
            }
            var item :Object = _destScene.selectedItem;
            var targetSceneId :int;
            if (item is SceneBookmarkEntry) {
                targetSceneId = (item as SceneBookmarkEntry).sceneId;
                
            } else if (item is int) {
                targetSceneId = int(item);

            } else {
                // parse the 'text' value
                var val :Number = Number(o);
                if (isNaN(val)) {
                    return;
                }
                targetSceneId = int(val);
            }
//            var vals :Array = furni.splitActionData();
//            vals.shift(); // remove the previous first entry
//            vals.unshift(targetSceneId);
//            furni.actionData = vals.join(":");
            furni.actionData = String(targetSceneId);
            spritePropsUpdated();
        }, _destScene, "text");

//        BindingUtils.bindSetter(function (o :Object) :void {
//            var furni :FurniData = (_sprite as FurniSprite).getFurniData();
//            if (furni.actionType != FurniData.ACTION_PORTAL) {
//                return; // don't update if we shouldn't
//            }
//            var val :Number = Number(o);
//            if (isNaN(val)) {
//                return;
//            }
//            var vals :Array = furni.splitActionData();
//            vals[1] = int(val); // replace the target portal id
//            furni.actionData = vals.join(":");
//            spritePropsUpdated();
//        }, _destPortal, "text");
    }

    protected var _xScale :TextInput;
    protected var _yScale :TextInput;

    protected var _actionType :ComboBox;
    protected var _actionData :TextInput;

    protected var _actionPanels :ViewStack;

    protected var _destScene :ComboBox;
//    protected var _destPortal :TextInput;

    protected var _url :TextInput;
}
}
