//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PartyBoardService}.
 */
public interface PartyBoardProvider extends InvocationProvider
{
    /**
     * Handles a {@link PartyBoardService#createParty} request.
     */
    void createParty (ClientObject caller, Currency arg1, int arg2, String arg3, int arg4, boolean arg5, PartyBoardService.JoinListener arg6)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#getCreateCost} request.
     */
    void getCreateCost (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#getPartyBoard} request.
     */
    void getPartyBoard (ClientObject caller, byte arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#getPartyDetail} request.
     */
    void getPartyDetail (ClientObject caller, int arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PartyBoardService#locateParty} request.
     */
    void locateParty (ClientObject caller, int arg1, PartyBoardService.JoinListener arg2)
        throws InvocationException;
}
