//
// $Id$

package com.threerings.msoy.world.client.layout {

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.util.ClassUtil;

/**
 * Collection of static classes for testing and creating layout instances.
 */
public class RoomLayoutFactory {
    
    /**
     * Returns true if the specified layout supports the specified decor type.
     * Single layout can support multiple decor types.
     */
    public static function isDecorSupported (layout :RoomLayout, decor :Decor) :Boolean
    {
        var layoutClass :Class = layoutClassForDecor(decor);
        return (ClassUtil.getClass(layout) === layoutClass);
    }

    /**
     * Creates a new, uninitialized room layout instance for the specified decor.
     */
    public static function createLayout (decor :Decor, view :RoomView) :RoomLayout
    {
        var layoutClass :Class = layoutClassForDecor(decor);
        return new layoutClass(view);
    }

    /**
     * Returns a layout class appropriate for the given decor type.
     */
    protected static function layoutClassForDecor (decor :Decor) :Class
    {
        if (decor == null) {
            // this should only happen during room initialization
            return RoomLayoutStandard;
        }
        
        // since we only have two layout classes right now, don't worry about a lookup table :)
        if (decor.type == Decor.FLAT_LAYOUT) {
            return RoomLayoutFlatworld;
        } else {
            return RoomLayoutStandard;
        }
    }
}
}
