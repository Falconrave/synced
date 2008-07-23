//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.TextEvent;

import flash.geom.Point;
import flash.geom.Rectangle;

import mx.controls.CheckBox;
import mx.controls.ColorPicker;
import mx.controls.HSlider;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.controls.TextInput;

import mx.validators.NumberValidator;
import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class DataEditor extends FieldEditor
{
    public function DataEditor (ctx :RemixContext, name :String)
    {
        var entry :Object = ctx.pack.getDataEntry(name);
        super(ctx, name, entry);

        _value = entry.value;
    }

    override protected function getUI (entry :Object) :Array
    {
        try {
            return Object(this)["setup" + entry.type](entry);
        } catch (err :Error) {
            // fall through to super
        }
        return super.getUI(entry);
    }

    protected function setupBoolean (entry :Object) :Array
    {
        var tog :CheckBox = new CheckBox();
        tog.selected = Boolean(entry.value);
        tog.addEventListener(Event.CHANGE, function (... ignored) :void {
            updateValue(tog.selected);
        });

        return [ tog, new Spacer(), tog ];
    }

    protected function setupString (entry :Object, validator :Validator = null) :Array
    {
        var valToString :Function = function (val :Object) :String {
            return String(val);
        };
        var createFn :Function = function () :PopupEditor {
            return new PopupStringEditor(validator);
        };
        return setupPopper(entry, valToString, createFn);
    }

    protected function setupNumber (entry :Object) :Array
    {
        var min :Number = Number(entry.min);
        var max :Number = Number(entry.max);

        // TODO: allow min/max either way, but allow a hint to specify to use the slider
        if (!isNaN(max) && !isNaN(min)) {
            var hslider :HSlider = new HSlider();
            hslider.minimum = min;
            hslider.maximum = max;
            hslider.value = Number(entry.value);
            hslider.addEventListener(Event.CHANGE, function (... ignored) :void {
                updateValue(hslider.value);
            });

            return [ hslider, new Spacer(), hslider ];

        } else {
            var val :NumberValidator = new NumberValidator();
            val.minValue = min;
            val.maxValue = max;
            return setupString(entry, val);
        }
    }

    protected function setupColor (entry :Object) :Array
    {
        var picker :ColorPicker = new ColorPicker();
        picker.selectedColor = uint(entry.value);
        _value = picker.selectedColor;
        picker.addEventListener(Event.CLOSE, function (... ignored) :void {
            updateValue(picker.selectedColor);
        });

        return [ picker, new Spacer(), picker ];
    }

    protected function setupPoint (entry :Object) :Array
    {
        var valToString :Function = function (val :Object) :String {
            var p :Point = val as Point;
            if (p == null) {
                return "";
            }
            return "Point(" + p.x + ", " + p.y + ")";
        };
        var createFn :Function = function () :PopupEditor {
            return new PopupPointEditor();
        };
        return setupPopper(entry, valToString, createFn);
    }

    protected function setupRectangle (entry :Object) :Array
    {
        var valToString :Function = function (val :Object) :String {
            var r :Rectangle = val as Rectangle;
            if (r == null) {
                return "";
            }
            return "Rectangle(" + r.x + ", " + r.y + ", " + r.width + ", " + r.height + ")";
        };
        var createFn :Function = function () :PopupEditor {
            return new PopupRectangleEditor();
        };
        return setupPopper(entry, valToString, createFn);
    }

    protected function setupArray (entry :Object) :Array
    {
        var valToString :Function = function (val :Object) :String {
            return (val as Array).join();
        };
        var createFn :Function = function () :PopupEditor {
            return new PopupArrayEditor();
        };
        return setupPopper(entry, valToString, createFn);
    }

    /**
     * Configure an editor that uses a pop-up to edit the actual value.
     */
    protected function setupPopper (
        entry :Object, valToString :Function, createPopper :Function) :Array
    {
        var handleEntryChange :Function = function (event :ValueEvent) :void {
            if (event.value === entry.name) {
                // we don't use entry, above, because we want to get the latest data every
                // time we the button is pushed
                updateLabel(label, _ctx.pack.getDataEntry(entry.name).value, valToString);
            }
        };
        var dataEditor :DataEditor = this;
        var doPopFn :Function = function () :void {
            // here also, we don't use entry, we fetch it fresh
            var popper :PopupEditor = createPopper();
            popper.open(_ctx, dataEditor, _ctx.pack.getDataEntry(entry.name));
        };

        var label :Label = new Label();
        label.selectable = false;
        label.setStyle("color", NAME_AND_VALUE_COLOR);
        updateLabel(label, entry.value, valToString);
        _ctx.pack.addEventListener(EditableDataPack.DATA_CHANGED, handleEntryChange);

        var change :CommandButton = createEditButton(doPopFn);
        return [ label, change, change ];
    }

    protected function updateLabel (label :Label, value :Object, valToString :Function) :void
    {
        label.text = (value == null) ? "" : valToString(value);
    }

    internal function updateValue (value :*) :void
    {
        if (value != _value) {
            _value = value;
            updateEntry();
        }
    }

    override protected function updateEntry () :void
    {
        _ctx.pack.setData(_name, _used.selected ? _value : null);
        setChanged();
    }

    protected var _value :*;
}
}
