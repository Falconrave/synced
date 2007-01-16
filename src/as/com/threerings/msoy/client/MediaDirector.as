//
// $Id$

package com.threerings.msoy.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.ClientEvent;

import com.threerings.msoy.item.web.ItemIdent;

import com.threerings.msoy.data.ActorInfo;

import com.threerings.msoy.world.client.ActorSprite;
import com.threerings.msoy.world.client.AvatarSprite;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.WorldMemberInfo;
import com.threerings.msoy.world.data.WorldPetInfo;

public class MediaDirector extends BasicDirector
{
    public static const log :Log = Log.getLog(MediaDirector);

    public function MediaDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    /**
     * Get an actor sprite for the specified occupant info, caching as appropriate.
     */
    public function getActor (occInfo :ActorInfo) :ActorSprite
    {
        var isOurs :Boolean = (occInfo.bodyOid == _ctx.getClient().getClientOid());
        if (isOurs && _ourAvatar != null) {
            _ourAvatar.setActorInfo(occInfo);
            return _ourAvatar;
        }

        var sprite :ActorSprite;
        if (occInfo is WorldMemberInfo) {
            sprite = new AvatarSprite(occInfo);
            if (isOurs) {
                _ourAvatar = (sprite as AvatarSprite);
            }
        } else if (occInfo is WorldPetInfo) {
            sprite = new ActorSprite(occInfo);
        } else {
            log.warning("Requested to create sprite for unknown occupant " + occInfo + ".");
            // TODO: freakout?
        }
        return sprite;
    }

    /**
     * Get a Furni sprite for the specified furni data, caching as appropriate.
     */
    public function getFurni (furni :FurniData) :FurniSprite
    {
        return new FurniSprite(_mctx, furni);
    }

    /**
     * Release any references to the specified sprite, if appropriate.
     */
    public function returnSprite (sprite :MsoySprite) :void
    {
        if (sprite != _ourAvatar) {
            sprite.shutdown();

        } else {
            // prevent it from continuing to move, but don't shut it down
            _ourAvatar.stopMove();
        }
    }

    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // release our hold on our avatar
        _ourAvatar = null;
    }

    /** A casted copy of the context. */
    protected var _mctx :MsoyContext;

    /** Our very own avatar: avoid loading and unloading it. */
    protected var _ourAvatar :AvatarSprite;
}
}
