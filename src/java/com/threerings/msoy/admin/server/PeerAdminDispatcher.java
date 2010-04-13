//
// $Id$

package com.threerings.msoy.admin.server;

import javax.annotation.Generated;

import com.threerings.msoy.admin.data.PeerAdminMarshaller;
import com.threerings.msoy.admin.gwt.StatsModel;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PeerAdminProvider}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PeerAdminService.java.")
public class PeerAdminDispatcher extends InvocationDispatcher<PeerAdminMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PeerAdminDispatcher (PeerAdminProvider provider)
    {
        this.provider = provider;
    }

    @Override
    public PeerAdminMarshaller createMarshaller ()
    {
        return new PeerAdminMarshaller();
    }

    @Override
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PeerAdminMarshaller.COMPILE_STATISTICS:
            ((PeerAdminProvider)provider).compileStatistics(
                source, (StatsModel.Type)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
