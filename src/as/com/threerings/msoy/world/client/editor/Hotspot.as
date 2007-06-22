//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.geom.Point;

/**
 * A single hotspot that processes mouse clicks on its surface, mouse movement and release.
 */
public class Hotspot extends Sprite
{
    public function Hotspot (editor :FurniEditor)
    {
        _editor = editor;
    }

    /** Called when the editing UI is created. */
    public function init () :void
    {
        // register for mouse clicks on this hotspot
        addEventListener(MouseEvent.MOUSE_DOWN, startAction);
        addEventListener(MouseEvent.CLICK, clickSink);

        // register for mouse over and out, just for bitmap switching
        addEventListener(MouseEvent.ROLL_OVER, rollOver);
        addEventListener(MouseEvent.ROLL_OUT, rollOut);

        initializeDisplay();
        switchDisplay(_displayStandard);
    }

    /** Called before the editing UI is removed. */
    public function deinit () :void
    {
        removeEventListener(MouseEvent.MOUSE_DOWN, startAction);
        removeEventListener(MouseEvent.CLICK, clickSink);
        removeEventListener(MouseEvent.ROLL_OVER, rollOver);
        removeEventListener(MouseEvent.ROLL_OUT, rollOut);
    }

    /** Returns true if the hotspot is currently being dragged around to perform some action. */
    public function isActive () :Boolean
    {
        return _anchor != null;
    }
    
    /**
     * This function is called when the user presses a mouse button on this hotspot.
     * Subclasses should override it to provide their own functionality,
     * but make sure to call this (superclass) handler as well.
     */
    protected function startAction (event :MouseEvent) :void
    {
        // user clicked on the hotspot. let the games begin!
        _editor.setActive(this);

        // remember click location
        _anchor = new Point(event.stageX, event.stageY);

        // also, register for mouse moves and ups anywhere in the scene. if the player
        // pressed the button on the hotspot, we want to know about moves and the subsequent
        // mouse up regardless of where they happen.
        _editor.roomView.addEventListener(MouseEvent.MOUSE_MOVE, updateAction);
        _editor.roomView.addEventListener(MouseEvent.MOUSE_UP, endAction);
    }
        
    /**
     * This function is called when the user moves the mouse while holding down the mouse button.
     * Subclasses should override it to provide their own functionality,
     * but make sure to call this (superclass) handler as well.
     */
    protected function updateAction (event :MouseEvent) :void
    {
        // no op.
        // subclasses, do something here! or don't. whatever. do what you want, you will anyway.
    }

    /**
     * This function is called when the user releases a button that was pressed on this hotspot.
     * Subclasses should override it to provide their own functionality,
     * but make sure to call this (superclass) handler as well.
     */
    protected function endAction (event :MouseEvent) :void
    {
        // we are done, clean up. these events, for example - we no longer need them.
        _editor.roomView.removeEventListener(MouseEvent.MOUSE_MOVE, updateAction);
        _editor.roomView.removeEventListener(MouseEvent.MOUSE_UP, endAction);

        // maybe update the bitmap
        if (_delayedRollout) {
            switchDisplay(_displayStandard);
            _delayedRollout = false;
        }

        if (_editor.isIdle()) {
            Log.getLog(this).warning("Editor was idle before current hotspot finished: " + this);
        }

        _anchor = null;
        _editor.setActive(null);
    }

    /** Accepts mouse click events, and prevents them from propagating into the room view. */
    protected function clickSink (event :MouseEvent) :void
    {
        // don't let the room view see this click, otherwise it will think we're trying to
        // select another object, and all sorts of fun will ensue.
        event.stopPropagation(); 
    }

    /** Switches bitmaps on rollover. */
    protected function rollOver (event :MouseEvent) :void
    {
        // if this spurious rollover is caused by a mouse click, ignore it.
        if (event.relatedObject == null) {
            return;
        }

        // if the user rolled over during dragging, cancel any pending rollouts.
        if (isActive()) {
            _delayedRollout = false;
            return;
        }

        switchDisplay(_displayMouseOver);
    }

    /** Switches bitmaps on rollout. */
    protected function rollOut (event :MouseEvent) :void
    {
        // if this spurious rollover is caused by a mouse click, ignore it.
        if (event.relatedObject == null) {
            return;
        }

        // if the user rolled out during dragging, don't change the bitmap just yet,
        // but remember it for when the mouse button is released.
        if (isActive()) {
            _delayedRollout = true;
            return;
        }

        switchDisplay(_displayStandard);
    }
    
    /** Removes current display object and inserts the specified one in its place. */
    protected function switchDisplay (display :DisplayObject) :void
    {
        if (_currentDisplay != null) {
            removeChild(_currentDisplay);
            _currentDisplay = null;
        }
        if (display != null) {
            _currentDisplay = display;
            addChild(_currentDisplay);
            _currentDisplay.x = - _currentDisplay.width / 2;
            _currentDisplay.y = - _currentDisplay.height / 2;
        }
    }
    
    /**
     * Called during init(), this function initializes the hotspot's _display* variables.
     * This default version draws a boring white square to represent the hotspot.
     * Subclasses should override it to provide their own functionality;
     * calling this superclass function is not necessary.
     */
    protected function initializeDisplay () :void
    {
        const SIZE :int = 9;
        var bitmap :Shape;
        var g :Graphics;
        
        bitmap = new Shape();
        g = bitmap.graphics;
        g.clear();
        g.lineStyle(0, 0x000000, 0.5, true);
        g.beginFill(0xffffff, 1.0);
        g.drawRect(0, 0, SIZE, SIZE);
        g.endFill();
        _displayStandard = bitmap;

        bitmap = new Shape();
        g = bitmap.graphics;
        g.clear();
        g.lineStyle(0, 0x000000, 0.5, true);
        g.beginFill(0xaaaaff, 1.0);
        g.drawRect(0, 0, SIZE, SIZE);
        g.endFill();
        _displayMouseOver = bitmap;
    }


    /** Reference to the editor. */
    protected var _editor :FurniEditor;
    
    /**
     * Mouse position at the beginning of the action. Also used to verify whether
     * a modification action is currently taking place (in which case its value is non-null).
     */
    protected var _anchor :Point;

    /** Bitmap used for hotspot display. */
    protected var _displayStandard :DisplayObject;
    
    /** Bitmap used for hotspot with mouseover. */
    protected var _displayMouseOver :DisplayObject;

    /** Currently used _display* bitmap. */
    protected var _currentDisplay :DisplayObject;

    /**
     * Display helper variable: remembers whether a rollout happened during dragging, and should
     * cause a bitmap update after dragging has finished.
     */
    protected var _delayedRollout :Boolean = false;
}
}
