//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.client.ReportingListener;

public class MemberDirector extends BasicDirector
{
    public const log :Log = Log.getLog(MemberDirector);

    public function MemberDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Request to make the user our friend.
     */
    public function inviteToBeFriend (friendId :int) :void
    {
        _msvc.inviteToBeFriend(_wctx.getClient(), friendId, new ConfirmAdapter(
            function (cause :String) :void {
                log.info("Reporting failure [reason=" + cause + "].");
                _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
             },
             function () :void {
                 _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS, "m.friend_invited");
                 _wctx.getGameDirector().tutorialEvent("friendInvited");
             }));
    }

    /**
     * Request to change our display name.
     */
    public function setDisplayName (newName :String) :void
    {
        _msvc.setDisplayName(_wctx.getClient(), newName, new ReportingListener(_wctx));
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.MEMBER_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _msvc = (client.requireService(MemberService) as MemberService);
    }

    protected var _wctx :WorldContext;
    protected var _msvc :MemberService;
}
}
