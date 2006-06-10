package com.threerings.msoy.ui {

import mx.effects.EffectInstance;
import mx.effects.Effect;

public class FunctionEffect extends Effect
{
    /** The function to call. */
    public var func :Function;

    /** The arguments to pass to the function. */
    public var args :Array;

    public function FunctionEffect (target :Object = null)
    {
        super(target);

        instanceClass = FunctionEffectInstance;
    }

    // documentation inherited
    override protected function initInstance (instance :EffectInstance) :void
    {
        super.initInstance(instance);

        var fe :FunctionEffectInstance = (instance as FunctionEffectInstance);
        fe.func = func;
        fe.args = args;
    }
}
}
