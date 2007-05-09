//
// $Id$

package com.threerings.msoy.world.client.updates {

import com.threerings.io.TypedArray;

import com.threerings.whirled.data.SceneUpdate;

/**
 * The stack collects individual room updates. Every time an update is pushed, it is applied
 * to the room; every time it is popped, its effects are undone.
 */
public class UpdateStack
{
    /**
     * Creates a new update stack. The updateFn is a function that performs server access, of type:
     *   function (array :TypedArray of SceneUpdates) :void
     * Whenever an action is pushed or popped off the stack, its SceneUpdate object will be
     * recreated, and passed to updateFn, which will notify the server.
     */
    public function UpdateStack (updateFn :Function)
    {
        reset();
        _updateFn = updateFn;
    }

    /**
     * Push a new update on the stack, and apply its effects to the room.
     */
    public function push (action :UpdateAction) :void
    {
        _stack.push(action);
        update(action.makeApply());
    }

    /**
     * Get the top update on the stack (if one exists), undo its effects on the room,
     * and return it to the called. Returns null if the stack is empty.
     */
    public function pop () :UpdateAction
    {
        var action :UpdateAction = _stack.pop() as UpdateAction;
        if (action != null) {
            update(action.makeUndo());
        }

        return action;        
    }

    /**
     * Removes all updates from the stack, without undoing any of them.
     */
    public function reset () :void
    {
        _stack = new Array();
    }

    /**
     * Returns the size of the stack.
     */
    public function get length () :int
    {
        return _stack.length;
    }

    /**
     * Packages the given SceneUpdate into a typed array, and calls the update function.
     */
    protected function update (sceneUpdate :SceneUpdate) :void
    {
        var edits :TypedArray = TypedArray.create(SceneUpdate);
        edits.push(sceneUpdate);
        _updateFn(edits);
    }


    protected var _stack :Array;
    protected var _updateFn :Function;
}
}
