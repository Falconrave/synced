//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface PartyService extends InvocationService
{
    /** Requests to boot a player from the party. */
    void bootMember (Client client, int memberId, InvocationListener il);

    /** Requests to reassign leadership to another player. */
    void assignLeader (Client client, int memberId, InvocationListener il);

    /** Requests to update the party status. */
    void updateStatus (Client client, String status, InvocationListener il);

    /** Requests to change the party access control. */
    void updateRecruitment (Client client, byte recruitment, InvocationListener il);

    /** Invites a specific player to this party. */
    void inviteMember (Client client, int memberId, InvocationListener il);

    /** Called by the leader to move the party to a new scene. */
    void moveParty (Client client, int sceneId, InvocationListener il);

    /** Called by the leader to update the party's game. */
    void setGame (Client client, int gameId, boolean avrGame, InvocationListener il);
}
