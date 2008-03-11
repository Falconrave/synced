//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.server.CrowdClientResolver;

import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyObjectAccess;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.person.server.persist.ProfileRecord;

import com.threerings.msoy.game.data.PlayerObject;

/**
 * Resolves an MSOY Game client's runtime data.
 */
public class MsoyGameClientResolver extends CrowdClientResolver
{
    @Override // from PresentsClientResolver
    public ClientObject createClientObject ()
    {
        return new PlayerObject();
    }

    @Override // from PresentsClient
    protected void resolveClientData (ClientObject clobj)
        throws Exception
    {
        super.resolveClientData(clobj);

        PlayerObject playerObj = (PlayerObject) clobj;
        playerObj.setAccessController(MsoyObjectAccess.USER);

        // guests have MemberName as an auth username, members have Name
        if (_username instanceof MemberName) {
            resolveGuest(playerObj);
        } else {
            resolveMember(playerObj);
        }
    }

    /**
     * Resolve a msoy member. This is called on the invoker thread.
     */
    protected void resolveMember (PlayerObject playerObj)
        throws Exception
    {
        // load up their member information using on their authentication (account) name
        MemberRecord member = MsoyGameServer.memberRepo.loadMember(_username.toString());

        // NOTE: we avoid using the dobject setters here because we know the object is not out in
        // the wild and there's no point in generating a crapload of events during user
        // initialization when we know that no one is listening

        // we need their profile photo as well
        ProfileRecord precord = MsoyGameServer.profileRepo.loadProfile(member.memberId);
        playerObj.memberName = new VizMemberName(
            member.name, member.memberId,
            (precord == null) ? Profile.DEFAULT_PHOTO : precord.getPhoto());

        // configure various bits directly from their member record
        playerObj.humanity = member.humanity;

        // fill in this member's raw friends list
        playerObj.friends = new DSet<FriendEntry>(
            MsoyGameServer.memberRepo.loadFriends(member.memberId, -1));
    }

    /**
     * Resolve a lowly guest. This is called on the invoker thread.
     */
    protected void resolveGuest (PlayerObject playerObj)
        throws Exception
    {
        // our auth username has our assigned name and member id, so use those
        MemberName aname = (MemberName)_username;
        playerObj.memberName = new VizMemberName(
            aname.toString(), aname.getMemberId(), Profile.DEFAULT_PHOTO);
    }
}
