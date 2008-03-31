//
// $Id$

package com.threerings.msoy.ui {

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.MovieClip;
import flash.display.Sprite;

import flash.events.Event;

import flash.utils.ByteArray;

// NOTE: minimize any dependancies on non-builtin packages, because this class is
// used by our application preloader.

public class LoadingSpinner extends Sprite
{
    public static const WIDTH :int = 168;
    public static const HEIGHT :int = 116;

    public function LoadingSpinner ()
    {
        // avoiding using MultiLoader to minimize dependancies
        var l :Loader = new Loader();
        l.contentLoaderInfo.addEventListener(Event.COMPLETE, handleComplete);
        l.loadBytes(new SPINNER() as ByteArray);
    }

    /**
     * Set the progress to be displayed on the spinner.
     * Call with no args to set to "indeterminate" mode.
     */
    public function setProgress (partial :Number = NaN, total :Number = NaN) :void
    {
        _progress = Math.round(partial * 100 / total); // evals to NaN if any args are NaN
        updateSpinner();
    }

    protected function handleComplete (event :Event) :void
    {
        _spinner = (event.target as LoaderInfo).loader.content as MovieClip;
        addChild(_spinner);
        updateSpinner();

        // TODO: do we need to unload?
    }

    protected function updateSpinner () :void
    {
        if (_spinner == null) {
            return;
        }

        if (!isNaN(_progress)) {
            var frame :int = 1 + _progress;
            _spinner.gotoAndStop(frame);

        } else if (_spinner.currentFrame < 102) {
            _spinner.gotoAndPlay(102);
        }
    }

    protected var _progress :Number = NaN;

    protected var _spinner :MovieClip;

    [Embed(source="../../../../../../rsrc/media/loading.swf", mimeType="application/octet-stream")]
    protected static const SPINNER :Class;
}
}
