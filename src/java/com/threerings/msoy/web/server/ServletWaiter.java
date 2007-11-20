//
// $Id$

package com.threerings.msoy.web.server;

import java.util.logging.Level;

import com.samskivert.servlet.util.ServiceWaiter;

import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Used to bridge the gap between synchronous servlets and our asynchronous
 * game server architecture.
 *
 * <p> We don't want to use service waiters as a permanent solution, as that
 * will result in a zillion servlet threads hanging around waiting for results
 * all over the goddamned place. Instead we want to rearchitect Jetty to
 * support asynchronous servlets that give up their thread when they need to
 * block and grab a new thread out of the pool when they're ready to go
 * again. This is a tall order, but I think they might be working on something
 * like this for Jetty 6.0.
 */
public class ServletWaiter<T> extends ServiceWaiter<T>
{
    public ServletWaiter (String ident)
    {
        _ident = ident;
    }

    /**
     * Waits for our asynchronous result and returns it if all is well. If
     * anything goes wrong (a timeout or a asynchronous call failure) the
     * exception is logged and a {@link ServiceException} is thrown.
     */
    public T waitForResult ()
        throws ServiceException
    {
        try {
            if (waitForResponse()) {
                return getArgument();
            } else {
                throw getError();
            }

        } catch (InvocationException ie) {
            // pass these through without a fuss
            throw new ServiceException(ie.getMessage());

        } catch (Exception e) {
            log.log(Level.WARNING, _ident + " failed.", e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    protected String _ident;
}
