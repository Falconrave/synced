//
// $Id$

package com.threerings.msoy.client {

import flash.display.Loader;
import flash.display.Sprite;
import flash.display.MovieClip;

import flash.events.Event;
import flash.events.ProgressEvent;

import flash.external.ExternalInterface;

import flash.net.URLRequest;

import flash.system.Capabilities;
import flash.utils.getTimer;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

// NOTE: minimize dependancies outside of flash.*, since this is our preloader...

import mx.events.FlexEvent;

import mx.preloaders.IPreloaderDisplay

import com.threerings.msoy.data.UberClientModes;
import com.threerings.msoy.ui.LoadingSpinner;

/**
 * Displays a spinny animation during loading, but also validates the required
 * flash version for embedded or stub clients.
 */
public class Preloader extends Sprite
    implements IPreloaderDisplay
{
    /** The minimum flash player version required by whirled.
     * NOTE: if you update this value, you should also examine and update checkOldStub().  */
    public static const MIN_FLASH_VERSION :Array = [ 9, 0, 115, 0 ];
    //public static const MIN_FLASH_VERSION :Array = [ 10, 0, 12, 36 ];

    /** The timestamp at which the preloader was initialized, for hacky access by MsoyClient. */
    public static var preloaderStart :int;

    /**
     * Creates the text field used to display a link to Whirled below the loading spinner. The
     * field will have its x and y position configured. This is used by BlankPlaceView to recreate
     * the same display.
     */
    public static function makeSplashText (text :String, stageWidth :int, spinnerY :int) :TextField
    {
        var field :TextField = new TextField();
        field.autoSize = TextFieldAutoSize.CENTER;
        field.defaultTextFormat = LoadingSpinner.makeTextFormat(18);
        field.htmlText = text;
        field.x = (stageWidth - field.width) / 2;
        field.y = spinnerY + BLURB_Y_OFFSET;
        return field;
    }

    /**
     */
    public function Preloader ()
    {
        _spinner = new LoadingSpinner();
        _spinner.setProgress(0, 1);
        addChild(_spinner);

        // note the time at which we started up
        preloaderStart = getTimer();
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundAlpha (value :Number) :void
    {
        _bgAlpha = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundAlpha () :Number
    {
        return _bgAlpha;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundColor (value :uint) :void
    {
        _bgColor = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundColor () :uint
    {
        return _bgColor;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundImage (value :Object) :void
    {
        _bgImage = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundImage () :Object
    {
        return _bgImage;
    }

    // from IPreloaderDisplay and stupidly so
    public function set backgroundSize (value :String) :void
    {
        _bgSize = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get backgroundSize () :String
    {
        return _bgSize;
    }

    // from IPreloaderDisplay and stupidly so
    public function set stageHeight (value :Number) :void
    {
        _stageH = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get stageHeight () :Number
    {
        return _stageH;
    }

    // from IPreloaderDisplay and stupidly so
    public function set stageWidth (value :Number) :void
    {
        _stageW = value;
    }

    // from IPreloaderDisplay and stupidly so
    public function get stageWidth () :Number
    {
        return _stageW;
    }

    // from IPreloaderDisplay
    public function set preloader (value :Sprite) :void
    {
        // TODO: remove the following debugging
        var mc :MovieClip = value.root as MovieClip;
        var working :Boolean = (mc.framesLoaded < mc.totalFrames);
        trace("----> Preloader " + (working ? "DID" : "did NOT") + " work.");
        // END: TODO

        MsoyParameters.init(value, function () :void {
            // two error cases: we are in a stub that is forcing the version down
            // or we aren't but the user doesn't have the required version.
            if (checkFlashUpgrade() || checkOldStub()) {
                return;
            }
            value.addEventListener(ProgressEvent.PROGRESS, handleProgress);
            value.addEventListener(FlexEvent.INIT_COMPLETE, handleComplete);
        });

        if (isEmbed()) {
            // figure out what upsell message to use (this must == what's in general.properties)
            var msg :String = getContentPage().indexOf("game") == -1 ?
                "Create your own world in " : "Play more multiplayer games on";
            msg += (" <a href=\"" + getWhirledPage("") + "\" target=\"_blank\">" +
                "<u>Whirled.com</u></a>");
            addChild(makeSplashText(msg, stage.stageWidth, _spinner.y));
        }
    }

    // from IPreloaderDisplay
    public function initialize () :void
    {
        _spinner.x = (stage.stageWidth - LoadingSpinner.WIDTH) / 2;
        _spinner.y = (stage.stageHeight - LoadingSpinner.HEIGHT) / 2;
    }

    /**
     * Check to see if the flash version is being forced downwards by being compiled
     * in an old stub.
     *
     * @return true if we are and it is being handled.
     */
    protected function checkOldStub () :Boolean
    {
        // if we're not running in the stub, none of this applies
        if (UberClientModes.STUB != int(MsoyParameters.get()["mode"])) {
            return false;
        }

        // NOTE: this code is flash 9 -> 10 specific. This might need to get more complicated
        // in the future. Molotov's for adobe.
        if (getFlashVersion()[0] > 9) {
            var l :Loader = new Loader();
            if (!l.hasOwnProperty("unloadAndStop")) { // only available in 10
                // the stub is booching us!
                showMessage("This window into the Whirled needs to be updated by the creator.",
                    "Click here to see their whirled live instead.");
                return true;
            }
        }
        return false; // all is ok
    }

    /**
     * Check to see if the we should try auto-upgrading the flash player.
     *
     * @return true if so.
     */
    protected function checkFlashUpgrade () :Boolean
    {
        if (checkFlashVersion()) {
            return false;
        }

        showMessage("This content requires Flash " + MIN_FLASH_VERSION.join(","),
            "Click here to visit Whirled.com and upgrade your flash player.");
        return true;
    }

    /**
     * Check the flash version.
     *
     * @return true if we have an adequate version.
     */
    protected function checkFlashVersion () :Boolean
    {
        var bits :Array = getFlashVersion();
        // pad out the version number to be the same length as our min
        while (bits.length < MIN_FLASH_VERSION.length) {
            bits.push(0);
        }

        // now check each portion of the version number
        for (var ii :int = 0; ii < bits.length; ii++) {
            var required :int = int(MIN_FLASH_VERSION[ii]);
            var actual :int = int(bits[ii]);
            if (actual < required) {
                return false;

            } else if (actual > required) {
                break; // no need to check lesser version numbers
            }
            // else: if two are the same, move to the next minor version number.
        }
        return true;
    }

    /**
     * Get the flash player version as an array of Strings, like [ "9", "0", "115", "0" ].
     */
    protected function getFlashVersion () :Array
    {
        // the version looks like "LNX 9,0,31,0"
        var bits :Array = Capabilities.version.split(" ");
        return (bits[1] as String).split(",");
    }

    protected function showMessage (msg :String, link :String) :void
    {
        removeChild(_spinner);

        // show a fun error
        graphics.beginFill(0x79afd3);
        graphics.drawRect(0, 0, stage.stageWidth, stage.stageHeight);
        graphics.endFill();
        var l :Loader = new Loader();
        addChild(l);
        l.load(new URLRequest(DeploymentConfig.serverURL + "images/stub/error.jpg"));

        // prefer to open the link in the same window/frame/tab, but if we can't, then don't.
        var target :String = "_self";
        try {
            ExternalInterface.call("window.location.href.toString");
        } catch (e :Error) {
            target = "_blank";
        }

        var field :TextField = new TextField();
        field.autoSize = TextFieldAutoSize.CENTER;
        field.wordWrap = true;
        field.multiline = true;
        field.width = _stageW;
        field.textColor = 0xFFFFFF;
        field.htmlText = "<font size=\"18\">" + msg + "<br/><br/>" +
            "<a href=\"" + getContentPage() + "\" target=\"" + target + "\">" +
            "<u>" + link + "</u></a></font>";
        // TODO: remove debugging
        trace("Computed page as " + getContentPage() + ".");
        addChild(field);
    }

    /**
     * Return a link back to Whirled that attempts to visit the same page that was specified in the
     * embed/stub parameters. Ugh!
     */
    protected function getContentPage () :String
    {
        // This is so fucking brittle
        var p :Object = MsoyParameters.get();

//        for (var s :String in p) {
//            trace("parameter: " + s + "  =>  " + p[s]);
//        }

        // see WorldController.goToPlace();
        var page :String = "";
        for each (var stuffs :Array in ARGS_TO_PAGES) {
            if (null != p[stuffs[0]]) {
                page = String(stuffs[1] + p[stuffs[0]]);
                break;
            }
        }

        return getWhirledPage(page);
    }

    /**
     * Returns the URL for the supplied Whirled page, using /welcome if appropriate.
     */
    protected function getWhirledPage (token :String) :String
    {
        // if there's an affiliate, route them through /welcome/
        var p :Object = MsoyParameters.get();
        if (null != p["aff"]) {
            return DeploymentConfig.serverURL + "welcome/" + p["aff"] + "/" + token;
        } else {
            return DeploymentConfig.serverURL + "#" + token;
        }
    }

    protected function isEmbed () :Boolean
    {
        // this is sort of a hack to determine whether we're an embedded game/room or not
        var params :Object = MsoyParameters.get();
        // params could be null here if loading async from the filesystem, in which case we're
        // not an embed anyway.
        return (params != null) && (params["vec"] != null);
    }

    protected function handleProgress (event :ProgressEvent) :void
    {
        _spinner.setProgress(event.bytesLoaded, event.bytesTotal);
    }

    protected function handleComplete (event :Event) :void
    {
        // signal flex to start up our app
        dispatchEvent(new Event(Event.COMPLETE));
    }

    protected var _spinner :LoadingSpinner;

    protected var _bgAlpha :Number;
    protected var _bgColor :uint;
    protected var _bgImage :Object;
    protected var _bgSize :String;
    protected var _stageH :Number;
    protected var _stageW :Number;

    // TODO: fill this out more? Are AVRG games handled?
    protected static const ARGS_TO_PAGES :Array = [
        [ "sceneId", "world-s" ],
        [ "room", "world-s" ], // alias used by stubs
        [ "memberHome", "world-h" ],
        [ "gameLobby", "world-game_p_" ], // legacy?
        [ "gameId", "world-game_p_" ],
        [ "game", "world-game_p_" ] // alias used by stubs
    ];

    /** The number of pixels below the center of the loading spinner that we put the blurb. */
    protected static const BLURB_Y_OFFSET :int = 175;
}
}
