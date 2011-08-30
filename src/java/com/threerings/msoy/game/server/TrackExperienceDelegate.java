//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.parlor.game.server.GameManagerDelegate;

import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MemberNodeActions;

/**
 * Tracks the user's experience playing a game.
 */
public class TrackExperienceDelegate extends GameManagerDelegate
{
    public TrackExperienceDelegate (GameContent content)
    {
        _content = content;
    }

    @Override
    public void bodyEntered (int bodyOid)
    {
        final PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);
        MemberNodeActions.addExperience(
            plobj.memberName.getId(), _content.game.isAVRG ?
            HomePageItem.ACTION_AVR_GAME : HomePageItem.ACTION_GAME, _content.gameId);
    }

    /** Game description. */
    protected final GameContent _content;
}
