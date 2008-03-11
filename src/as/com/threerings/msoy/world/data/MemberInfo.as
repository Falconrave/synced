//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.GameSummary;

/**
 * Contains published information about a member in a scene.
 */
public class MemberInfo extends ActorInfo
{
    /**
     * Get the member id for this user, or 0 if they're a guest.
     */
    public function getMemberId () :int
    {
        return (username as MemberName).getMemberId();
    }

    /**
     * Return true if we represent a guest.
     */
    public function isGuest () :Boolean
    {
        return MemberName.isGuest(getMemberId());
    }

    /**
     * Returns information on a game this user is currently lobbying or playing.
     */
    public function getGameSummary () :GameSummary
    {
        return _game;
    }

    /**
     * Return the scale that should be used for the media.
     */
    public function getScale () :Number
    {
        return _scale;
    }

    /**
     * Update the scale. This method currently only exists on the actionscript side.  We update the
     * scale immediately when someone is futzing with the scale in the avatar viewer.
     */
    public function setScale (scale :Number) :void
    {
        _scale = scale;
    }

    // from ActorInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _scale = ins.readFloat();
        _game = (ins.readObject() as GameSummary);
    }

    protected var _scale :Number;
    protected var _game :GameSummary;
}
}
