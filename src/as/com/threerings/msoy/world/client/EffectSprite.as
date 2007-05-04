//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.ValueEvent;

import com.threerings.msoy.world.data.EffectData;

public class EffectSprite extends FurniSprite
{
    /** A value event dispatched when the effect is finished. */
    public static const EFFECT_FINISHED :String = "EffectFinished";

    public function EffectSprite (effect :EffectData)
    {
        super(effect);
    }

    override public function getRoomLayer () :int
    {
        return (_furni as EffectData).roomLayer;
    }

    override public function getMaxContentWidth () :int
    {
        return int.MAX_VALUE;
    }

    override public function getMaxContentHeight () :int
    {
        return int.MAX_VALUE;
    }

    // from MsoySprite
    override public function sendMessage (name :String, arg :Object, isAction :Boolean) :void
    {
        if (!isAction && name == "effectFinished") {
            dispatchEvent(new ValueEvent(EFFECT_FINISHED, null));

        } else {
            super.sendMessage(name, arg, isAction);
        }
    }

    override protected function createBackend () :EntityBackend
    {
        return new EffectBackend();
    }

    // from MsoySprite
    override protected function configureMouseProperties () :void
    {
        // effects cannot and should not interact with the mouse
        mouseChildren = false;
        mouseEnabled = false;
    }

    // from FurniSprite
    override protected function isLoadingWatched () :Boolean
    {
        return false; // Effects are not watched.
    }
}
}
