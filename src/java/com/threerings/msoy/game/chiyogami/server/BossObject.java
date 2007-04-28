//
// $Id$

package com.threerings.msoy.game.chiyogami.server;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.util.Name;

import com.threerings.msoy.data.MsoyBodyObject;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.data.WorldActorInfo;

/**
 * The BodyObject that backs a Chiyogami boss.
 */
public class BossObject extends MsoyBodyObject
{
    /**
     * Initialize the boss.
     */
    // TODO
    public void init (Avatar avatar)
    {
        _avatar = avatar;
        setUsername(new Name(avatar.name));
    }

    public void init (MediaDesc desc)
    {
        _avatar = null;
        _ident = new ItemIdent(Item.OCCUPANT, this.getOid());
        _desc = desc;
    }

    @Override
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        if (_avatar != null) {
            return new WorldActorInfo(this, _avatar.getIdent(), _avatar.avatarMedia);
        } else {
            return new WorldActorInfo(this, _ident, _desc);
        }
    }

    /** The avatar item being used for this boss. */
    protected transient Avatar _avatar;

    protected transient ItemIdent _ident;
    protected transient MediaDesc _desc;
}
