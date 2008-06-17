//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.utils.ByteArray;

import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class FileEditor extends FieldEditor
{
    public function FileEditor (ctx :RemixContext, name :String, serverURL :String)
    {
        var entry :Object = ctx.pack.getFileEntry(name);
        super(ctx, name, entry);
        _serverURL = serverURL;
    }

    override protected function getUI (entry :Object) :Array
    {
        _label = new Label();
        _label.selectable = false;
        //_label.setStyle("color", NAME_AND_VALUE_COLOR);
        _label.text = entry.value as String;

        var change :CommandButton = createEditButton(showFile);
        change.enabled = (entry.value != null);

        return [ _label, change, change ];
    }

    internal function updateValue (filename :String, bytes :ByteArray) :void
    {
        if (filename == null) {
            if (_bytes == null) {
                _used.selected = (_ctx.pack.getFileEntry(_name).value != null);
            }
            return;
        }

        _component.enabled = true;
        _label.text = filename;
        _bytes = bytes;
        updateEntry();

        _ctx.pack.replaceFile(_name, filename, bytes);
        setChanged();
    }

    override protected function handleUsedToggled (selected :Boolean) :void
    {
        if (selected && _bytes == null) {
            // pop up the damn chooser
            showFile();

        } else {
            super.handleUsedToggled(selected);
        }
    }

    override protected function updateEntry () :void
    {
        if (_bytes != null) {
            _ctx.pack.replaceFile(_name, _used.selected ? _label.text : null, _bytes);
            setChanged();
        }
    }

    protected function showFile () :void
    {
        new PopupFilePreview(this, _name, _ctx, _serverURL);
    }

    protected var _label :Label;

    protected var _bytes :ByteArray;

    protected var _serverURL :String;
}
}
