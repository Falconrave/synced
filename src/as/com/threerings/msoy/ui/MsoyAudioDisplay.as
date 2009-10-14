//
// $Id$

package com.threerings.msoy.ui {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.text.TextField;

import com.threerings.util.CommandEvent;
import com.threerings.util.Log;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.text.TextFieldUtil;
import com.threerings.media.AudioPlayer;
import com.threerings.media.Mp3AudioPlayer;
import com.threerings.media.MediaPlayerCodes;


import com.threerings.msoy.client.MsoyController;
/**
 * The msoy-skinned audio display.
 */
public class MsoyAudioDisplay extends Sprite
{
    public static const WIDTH :int = MediaControls.WIDTH;

    public static const HEIGHT :int = MediaControls.HEIGHT + ((FIELD_HEIGHT + PAD) * 2);

    /**
     * Create.
     */
    public function MsoyAudioDisplay (player :AudioPlayer, commentCallback :Function = null)
    {
        _player = player;
        _player.addEventListener(MediaPlayerCodes.METADATA, handleMetadata);
        _player.addEventListener(MediaPlayerCodes.STATE, handleState);

        _controls = new MediaControls(player, commentCallback);

        configureUI();
    }

    override public function get width () :Number
    {
        return WIDTH;
    }

    override public function get height () :Number
    {
        return HEIGHT;
    }

    public function getControls () :MediaControls
    {
        return _controls;
    }

    public function unhook () :void
    {
        _player.removeEventListener(MediaPlayerCodes.METADATA, handleMetadata);
        _controls.unhook();
    }

    /**
     * Stop playing our video.
     */
    public function unload () :void
    {
        unhook();
        _controls.unload();
    }

    protected function configureUI () :void
    {
        var masker :Shape = new Shape();
        this.mask = masker;
        addChild(masker);
        var g :Graphics = masker.graphics;
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, WIDTH, HEIGHT);
        g.endFill();

//        const text :String = "N/A"; // i18n is not happening here, this is non-flex.
        const textProps :Object = { selectable: false, x: PAD, width: WIDTH - (PAD * 2) };
        const formatProps :Object = { color: 0x000000, size: 12, bold: true, italic: true };

        _artist = TextFieldUtil.createField("unknown artist", textProps, formatProps);
        addChild(_artist);

        _song = TextFieldUtil.createField("unknown song", textProps, formatProps);
        _song.y += FIELD_HEIGHT + PAD;
        addChild(_song);

        _artist.addEventListener(MouseEvent.CLICK, handleInfoClicked);
        _song.addEventListener(MouseEvent.CLICK, handleInfoClicked);

        _controls.y = (FIELD_HEIGHT + PAD) * 2; //HEIGHT - MediaControls.HEIGHT;
        addChild(_controls);

        checkId3(_player.getMetadata());
    }

    protected function handleState (event :ValueEvent) :void
    {
        if (event.value == MediaPlayerCodes.STATE_UNREADY) {
            _song.text = "";
            _artist.text = "";
        }
    }

    protected function handleMetadata (event :ValueEvent) :void
    {
        checkId3(event.value);
    }

    protected function checkId3 (id3 :Object) :void
    {
        updateField(_artist, id3, "artist", "artist");
        updateField(_song, id3, "songName", "song");
    }

    protected function updateField (
        field :TextField, id3 :Object, prop :String, unkName :String) :void
    {
        const info :String = (id3 != null) ? id3[prop] as String : null;
        const blankInfo :Boolean = StringUtil.isBlank(info);
        TextFieldUtil.updateText(field, blankInfo ? ("unknown " + unkName) : info);
        TextFieldUtil.updateFormat(field, { italic: blankInfo });
    }

    protected function handleInfoClicked (event :MouseEvent) :void
    {
        // TODO: If I use MsoyController.AUDIO_CLICKED, all that links in for audioplayer
        CommandEvent.dispatch(this, "AudioClicked", Mp3AudioPlayer(_player).getClientData());
    }

    protected static const FIELD_HEIGHT :int = 15;

    protected static const PAD :int = 5;

    protected const log :Log = Log.getLog(this);

    protected var _player :AudioPlayer;

    protected var _controls :MediaControls;

    protected var _artist :TextField;

    protected var _song :TextField;
}
}
