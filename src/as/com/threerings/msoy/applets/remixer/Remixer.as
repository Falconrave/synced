package com.threerings.msoy.applets.remixer {

import flash.display.Sprite;

import flash.events.Event;
import flash.events.ErrorEvent;
import flash.events.MouseEvent;

import flash.external.ExternalInterface;

import flash.utils.ByteArray;

import com.whirled.remix.data.EditableDataPack;

import com.threerings.util.ParameterUtil;
import com.threerings.util.StringUtil;

import com.threerings.flash.SimpleTextButton;

import com.threerings.msoy.utils.Base64Sender;

[SWF(width="540", height="450")]
public class Remixer extends Sprite
{
    public function Remixer ()
    {
        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        var media :String = params["media"] as String;

        _pack = new EditableDataPack(media);
        _pack.addEventListener(Event.COMPLETE, handlePackComplete);
        _pack.addEventListener(ErrorEvent.ERROR, handlePackError);
    }

    protected function handlePackComplete (event :Event) :void
    {
        updatePreview();

        var btn :SimpleTextButton = new SimpleTextButton("change Eye color");
        btn.addEventListener(MouseEvent.CLICK, changeEyeColor);
        addChild(btn);
//
//        var dataFields :Array = _pack.getDataFields();
////        trace(StringUtil.toString(dataFields));
//        for each (var name :String in dataFields) {
//            var obj :Object = _pack.getDataEntry(name);
//            trace("" + name + ":");
//            for (var propName :String in obj) {
//                trace("   " + propName + " -> " + obj[propName]);
//            }
//        }
//        var fileFields :Array = _pack.getFileFields();
//        trace(StringUtil.toString(fileFields));
    }

    protected function handlePackError (event :ErrorEvent) :void
    {
        trace("Error loading: " + event.text)
    }

    protected function changeEyeColor (... ignored) :void
    {
        var color :uint = uint(Math.random() * 0xFFFFFF)
        _pack.setData("eyeColor", color);
        updatePreview();
    }

    protected function updatePreview () :void
    {
        // send the bytes to our previewer
        var b64 :Base64Sender = new Base64Sender("remixPreview", "setMediaBytes");
        b64.sendBytes(_pack.serialize());
    }

    protected var _pack :EditableDataPack;
}
}
