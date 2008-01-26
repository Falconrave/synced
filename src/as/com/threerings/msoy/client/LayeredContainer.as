//
// $Id: PlaceBox.as 6357 2007-10-25 16:24:55Z zell $

package com.threerings.msoy.client {

import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Matrix;

import mx.core.Container;
import mx.core.UIComponent;

import com.threerings.util.Log;

/**
 * Provide an organized way for callers to layer display objects onto one another at
 * different priority levels (which they will have to work out amongst themselves).
 *
 * This is by no means foolproof and calls can easily be made directly to the Container
 * we extend; it's still an improvement on separate pieces of our code base remotely
 * fiddling with rawChildren and competing for the top spot.
 *
 * TODO: We may want to remove the Base Layer concept and replace it with explicitly
 * prioritized layers.
 */
public class LayeredContainer extends Container
{
    public static const log :Log = Log.getLog(LayeredContainer);

    public function setBaseLayer (base :DisplayObject) :void
    {
        clearBaseLayer();
        addChildAt(_base = wrap(base), 0);
        log.debug("Base layer set [base=" + base + "]");
    }

    public function clearBaseLayer () :void
    {
        if (_base != null) {
            removeChild(_base);
            log.debug("Base layer cleared [base=" + _base + "]");
            _base = null;
        }
    }

    /**
     * Snapshot all the overlays.
     */
    public function snapshotOverlays (bitmapData :BitmapData, matrix :Matrix) :void
    {
        for (var ii :int = 0; ii < numChildren; ii++) {
            var disp :DisplayObject = getChildAt(ii);
            if (disp != _base) {
                var m :Matrix = disp.transform.matrix;
                m.concat(matrix);
                bitmapData.draw(disp, m, null, null, null, true);
            }
        }
    }

    /**
     * Adds a display object to overlay the main view as it changes. The lower the layer argument,
     * the lower the overdraw priority the layer has among other layers. The supplied DisplayObject
     * must have a name and it mustn't conflict with any other overlay name. Fortunately if you
     * don't name your display object it will be assigned a unique name.
     */
    public function addOverlay (overlay :DisplayObject, layer :int) :void
    {
        if (overlay.name == null || overlay.name == "") {
            log.warning("Refusing to add overlay with no name. Name that sucker!");
            return;
        } else if (_layers[overlay.name] != null) {
            log.warning("Refusing to add duplicate overlay [overlay=" + overlay.name + "].");
            return;
        }
        _layers[overlay.name] = layer;

        // step through the children until we find one whose layer is larger than ours
        for (var ii :int = 0; ii < numChildren; ii ++) {
            var child :DisplayObject = unwrap(getChildAt(ii));
            var childLayer :int = int(_layers[child.name]);
            if (childLayer > layer) {
                addChildAt(wrap(overlay), ii);
                return;
            }
        }

        // if no such child found, just append
        addChild(wrap(overlay));
    }

    /**
     * Removes a previously added overlay.
     */
    public function removeOverlay (overlay :DisplayObject) :void
    {
        if (_layers[overlay.name]) {
            _layers[overlay.name] = null;
        } else {
            log.warning("Removing unknown overlay [overlay=" + overlay.name + "]");
            // but I guess we'll remove it anyway
        }

        // remove this child from the display the hard way
        for (var ii :int = 0; ii < numChildren; ii++) {
            var child :DisplayObject = unwrap(getChildAt(ii));
            if (child == overlay) {
                child = removeChildAt(ii);
                if (child is FlexWrapper) {
                    (child as FlexWrapper).removeChildAt(0);
                }
                break;
            }
        }
    }

    public function containsOverlay (overlay :DisplayObject) :Boolean
    {
        return _layers[overlay.name] != null;
    }

    protected function wrap (object :DisplayObject) :DisplayObject
    {
        return (object is UIComponent) ? object : new FlexWrapper(object);
    }

    protected function unwrap (object :DisplayObject) :DisplayObject
    {
        return (object is FlexWrapper) ? (object as FlexWrapper).getChildAt(0) : object;
    }

    /** A mapping of overlays to the numerical layer priority at which they were added. */
    protected var _layers :Object = new Object();

    protected var _base :DisplayObject;
}
}

import flash.display.DisplayObject;
import mx.core.UIComponent;

/** Wraps a non-Flex component for use in Flex. */
class FlexWrapper extends UIComponent
{
    public function FlexWrapper (object :DisplayObject)
    {
        mouseEnabled = false;
        addChild(object);
    }
}
