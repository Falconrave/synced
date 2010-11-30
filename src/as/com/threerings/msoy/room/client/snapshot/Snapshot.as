//
// $Id$

package com.threerings.msoy.room.client.snapshot {

import com.threerings.util.Log;

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;

import flash.geom.Matrix;
import flash.geom.Rectangle;

import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLRequestMethod;

import flash.utils.ByteArray;

import mx.controls.scrollClasses.ScrollBar;

import com.threerings.display.BackgroundJPGEncoder;

import com.threerings.util.Log;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.LayeredContainer;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PlaceBox;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.EntitySprite;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.client.RoomElement;
import com.threerings.msoy.room.client.RoomView;

/**
 * Represents a particular snapshot
 */
public class Snapshot extends EventDispatcher
{
    public static const THUMBNAIL_WIDTH :int = 350;
    public static const THUMBNAIL_HEIGHT :int = 200;

    /** This is the maximum bitmap dimension, a flash limitation. */
    public static const MAX_BITMAP_DIM :int = 2880; // TODO: this is larger in FP10!

    public var bitmap :BitmapData;

    public const log :Log = Log.getLog(this);

    /**
     * Creates a snapshotter configured for the thumbnail Snapshot.
     */
    public static function createThumbnail (
        ctx :WorldContext, view :RoomView, onComplete :Function, onError :Function) :Snapshot
    {
        // for the canonical image, we create a new framer that centers the image within the frame,
        // introducing black bars if necessary.
        const frame :Rectangle = new Rectangle(0, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
        const framer :Framer = new CanonicalFramer(
            view.getScrollBounds(), frame, view.getScrollOffset());
        return new Snapshot(
            ctx, true, view, framer, frame.width, frame.height, onComplete, onError);
    }

    /**
     * Creates a snapshotter configured for a gallery Snapshot.
     */
    public static function createGallery (
        ctx :WorldContext, view :RoomView, onComplete :Function, onError :Function) :Snapshot
    {
        // TODO: we want the room bounds, not the room *view* bounds....
        var galWidth :int = view.getScene().getWidth();
        var galHeight :int = view.getScene().getHeight();
        var galFramer :Framer;
        if (galWidth > MAX_BITMAP_DIM || galWidth > MAX_BITMAP_DIM) {
            const galScale :Number = Math.min(
                MAX_BITMAP_DIM / galWidth, MAX_BITMAP_DIM / galHeight);
            galWidth *= galScale;
            galHeight *= galScale;
//            galFramer = new CanonicalFramer(view.getScrollBounds(),
//                new Rectangle(0, 0, galWidth, galHeight), view.getScrollOffset());
            // TODO: sort out real offset?
            galFramer = new NoopFramer();
        } else {
            galFramer = new NoopFramer();
        }
        return new Snapshot(ctx, false, view, galFramer, galWidth, galHeight, onComplete, onError);
    }

    /**
     * Create a 'Snapshot' of the provided view.  With a frame of the provided size.
     *
     * @param onCompleteFn informed when *encoding* is complete.
     * @param onErrorFn informed when *uploading* errors.
     */
    public function Snapshot (
        ctx :WorldContext, thumbnail :Boolean, view :RoomView, framer :Framer,
        width :int, height :int, onCompleteFn :Function, onErrorFn :Function)
    {
        _ctx = ctx;
        _view = view;
        _thumbnail = thumbnail;

        _frame = new Rectangle(0, 0, width, height);
        _framer = framer;
        bitmap = new BitmapData(width, height);

        addEventListener(Event.COMPLETE, onCompleteFn);
        addEventListener(IOErrorEvent.IO_ERROR, onErrorFn);
        addEventListener(SecurityErrorEvent.SECURITY_ERROR, onErrorFn);
    }

    public function get ready () :Boolean
    {
        return (_data != null);
    }

    public function startEncode () :void
    {
        if (!ready && _encoder == null) {
            _encoder = new BackgroundJPGEncoder(bitmap, 70);
            _encoder.addEventListener("complete", onJpegEncoded);
            _encoder.start();
        }
    }

    /**
     * Update the snapshot.
     */
    public function updateSnapshot (
        includeOccupants :Boolean, includeChat :Boolean, doEncode :Boolean) :Boolean
    {
        cancelEncoding();

        // first let's fill the bitmap with the room's background color
        bitmap.fillRect(_frame, 0xff000000 | _view.getScene().getBackgroundColor());

        var occPredicate :Function = null;
        if (!includeOccupants) {
            occPredicate = function (child :DisplayObject) :Boolean {
                var element :RoomElement = _view.vizToEntity(child);
                return (element == null) || !(element is OccupantSprite);
            };
        }

        var matrix :Matrix = _framer.getMatrix();

        // first snapshot the room
        var allSuccess :Boolean = _view.snapshot(bitmap, matrix, occPredicate);

        // then, add the overlays
        // find the layered container...
        var d :DisplayObject = _view;
        while (!(d is LayeredContainer) && d.parent != null) {
            d = d.parent;
        }
        if (d is LayeredContainer) {
            var lc :LayeredContainer = LayeredContainer(d);
            var layerPredicate :Function = function (child :DisplayObject) :Boolean {
                // if it's not even a layer, we must be further down: include
                if (!lc.containsOverlay(child)) {
                    if (child is ScrollBar) {
                        return false; // never snapshot the chat scrollbar...
                    }
                    return true;
                }
                // blacklist certain layers
                switch (lc.getLayer(child)) {
                default:
                    return true;

                case PlaceBox.LAYER_ROOM_SPINNER:
                case PlaceBox.LAYER_CHAT_LIST:
                case PlaceBox.LAYER_TRANSIENT:
                case PlaceBox.LAYER_FEATURED_PLACE:
                    return false;

                case PlaceBox.LAYER_CHAT_SCROLL:
                case PlaceBox.LAYER_CHAT_STATIC:
                case PlaceBox.LAYER_CHAT_HISTORY:
                    return includeChat;
                }
            };

            if (!lc.snapshot(bitmap, matrix, layerPredicate)) {
                allSuccess = false;
            }
        }

        _data = null; // clear old encoded data
        if (doEncode) {
            startEncode();
        }

        return allSuccess;
    }

    public function cancelAll () :void
    {
        if (_loader != null) {
            try {
                _loader.close();
            } catch (e :Error) {
                //ignore
            }
            clearLoader();
        }
        cancelEncoding();
    }

    /**
     * Cancel encoding if it's underway.  We don't cancel uploads.
     */
    public function cancelEncoding () :void
    {
        if (_encoder) {
            _encoder.cancel();
            _encoder = null;
        }
    }

    public function upload (createItem :Boolean = false, doneFn :Function = null) :void
    {
        const mimeBody :ByteArray = makeMimeBody(_data, createItem);

        const request :URLRequest = new URLRequest();
        request.url = DeploymentConfig.serverURL +
            (_thumbnail ? THUMBNAIL_SERVICE : SNAPSHOT_SERVICE);
        request.method = URLRequestMethod.POST;
        request.contentType = "multipart/form-data; boundary=" + BOUNDARY;
        request.data = mimeBody;

        _doneFn = doneFn;
        _loader = new URLLoader();
        _loader.addEventListener(Event.COMPLETE, onSuccess);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, onError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onError);
        _loader.load(request);
    }

    protected function onJpegEncoded (event :ValueEvent) :void
    {
        log.debug("jpeg encoded");
        _data = ByteArray(event.value);
        _encoder = null;

        dispatchEvent(new Event(Event.COMPLETE));

        // call whatever we're supposed to call with the jpeg data now that we have it
    }

    /** Creates an HTTP POST upload request. */
    protected function makeMimeBody (data :ByteArray, createItem :Boolean) :ByteArray
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        var itemName :String = StringUtil.truncate(
            Msgs.WORLD.get("m.sceneItemName", scene.getName()), MsoyCodes.MAX_NAME_LENGTH, "...");
        var sessionToken :String = MsoyCredentials(_ctx.getClient().getCredentials()).sessionToken;

        const b :String = "--" + BOUNDARY + "\r\n";
        const mediaIds :String = "snapshot" + (createItem ? ";furni;thumb" : "");
        var output :ByteArray = new ByteArray();
        output.writeUTFBytes(
            "\r\n" + b +
//            "Content-Disposition: form-data; name=\"auth\"\r\n" +
//            "\r\n" + sessionToken + "\r\n" + b +
            "Content-Disposition: form-data; name=\"member\"\r\n" +
            "\r\n" + String(_ctx.getMyId()) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"scene\"\r\n" +
            "\r\n" + String(scene.getId()) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"name\"\r\n" +
            "\r\n" + escape(itemName) + "\r\n" + b +
            "Content-Disposition: form-data; name=\"makeItem\"\r\n" +
            "\r\n" + createItem + "\r\n" + b +
            "Content-Disposition: form-data; name=\"" + mediaIds + "\"; " +
            "filename=\"snapshot.jpg\"\r\n" +
            "Content-Type: image/jpeg\r\n" +
            "\r\n");
        output.writeBytes(data);
        output.writeUTFBytes("\r\n--" + BOUNDARY + "--\r\n");
        return output;
    }

    protected function onError (event :ErrorEvent) :void
    {
        // re-dispatch it
        dispatchEvent(event);
        clearLoader();
    }

    protected function onSuccess (event :Event) :void
    {
        var fn :Function = _doneFn;
        var data :String = String(_loader.data);
        clearLoader();
        if (fn != null) {
            fn(data);
        }
    }

    protected function clearLoader () :void
    {
        _loader = null;
        _doneFn = null;
    }

    protected var _ctx :WorldContext;
    protected var _thumbnail :Boolean;

    protected var _encoder :BackgroundJPGEncoder;

    protected var _data :ByteArray;

    protected var _uploadOperation :Function;
    protected var _args :Array;

    protected var _view :RoomView;
    protected var _frame :Rectangle;
    protected var _framer :Framer;

   /** The currently operating uploader. */
    protected var _loader :URLLoader;
    protected var _doneFn :Function;

    protected static const THUMBNAIL_SERVICE :String = "scenethumbsvc";
    protected static const SNAPSHOT_SERVICE :String = "snapshotsvc";

    protected static const BOUNDARY :String = "why are you reading the raw http stream?";
}
}
