//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.utils.ByteArray;

import mx.containers.ViewStack;

import mx.core.UIComponent;

import com.threerings.util.Util;

import com.whirled.remix.data.EditableDataPack;

public class RemixContext
{
    /** The data pack we're using. */
    public var pack :EditableDataPack;

    /**
     * Create a RemixContext.
     */
    public function RemixContext (pack :EditableDataPack, viewStack :ViewStack)
    {
        this.pack = pack;
        _viewStack = viewStack;
    }

    public function getViewWidth () :int
    {
        return _viewStack.width;
    }

    public function getViewHeight () :int
    {
        return _viewStack.height;
    }

    public function pushView (view :UIComponent) :void
    {
        _viewStack.addChild(view);
        _viewStack.selectedIndex++;
    }

    public function popView () :void
    {
        _viewStack.selectedIndex--;
        _viewStack.removeChildAt(_viewStack.selectedIndex + 1);
    }

    /**
     * Create a unique filename to be placed in the pack.
     *
     * @param origname the source filename, something like "image.jpg". This will be returned
     *       unmodified if there are no competing filenames.
     * @param bytes if specified, a ByteArray associated with the filename. If any existing file
     * uses the same bytes then that filename will be returned.
     * @param extension the new filename extension, like "png".
     */
    public function createFilename (
        origname :String, bytes :ByteArray = null, extension :String = null) :String
    {
        if (bytes != null) {
            // look through the normal files to see if any have the same bytes
            for each (var filename :String in pack.getFilenames()) {
                if (Util.equals(pack.getFileByFilename(filename), bytes)) {
                    return filename;
                }
            }
        }

        var lastDot :int;
        if (extension != null) {
            // alter the extension of the specified filename
            lastDot = origname.lastIndexOf(".");
            if (lastDot == -1) {
                lastDot = origname.length;
                origname += ".";
            }
            origname = origname.substring(0, lastDot + 1) + extension;
        }

        // now check the name against ALL files
        var names :Array = pack.getFilenames(true);
        names.push(EditableDataPack.METADATA_FILENAME);
        for (var ii :int = 0; ii < 1000; ii++) {
            if (names.indexOf(origname) == -1) {
                return origname;
            }

            lastDot = origname.lastIndexOf(".");
            var base :String = (lastDot > 0) ? origname.substring(0, lastDot) : origname;
            var ext :String = (lastDot > 0) ? origname.substring(lastDot) : "";

            // split the basename up and see if there's any number part
            var result :Object = new RegExp("^(.+\\-)(\\d+)$").exec(base);
            if (result != null) {
                var number :int = parseInt(result[2]);
                origname = result[1] + (number + 1) + ext;
            } else {
                origname = base + "-2" + ext;
            }
            // and try again..
        }
        // once we've tried 1000 times, just fucking stick a fork in it.
        return origname;
    }

    /** The ViewStack hosting the remixer. */
    protected var _viewStack :ViewStack;
}
}
