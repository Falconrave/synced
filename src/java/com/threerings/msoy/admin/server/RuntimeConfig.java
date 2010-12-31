//
// $Id$

package com.threerings.msoy.admin.server;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ProxySubscriber;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.admin.server.ConfigRegistry;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.data.MoneyConfigObject;
import com.threerings.msoy.admin.data.ServerConfigObject;
import com.threerings.msoy.admin.data.SubscriptionConfigObject;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.money.server.MoneyExchange;
import com.threerings.msoy.server.MemberLocator;

import static com.threerings.msoy.Log.log;

/**
 * Provides access to runtime reconfigurable configuration data.
 */
@Singleton
public class RuntimeConfig
{
    /** Contains general server configuration data. */
    public final ServerConfigObject server = new ServerConfigObject();

    /** Contains money configuration data. */
    public final MoneyConfigObject money = new MoneyConfigObject();

    /** Contains costs of wares configuration data. */
    public final CostsConfigObject costs = new CostsConfigObject();

    /** Contains subscription configuration. */
    public final SubscriptionConfigObject subscription = new SubscriptionConfigObject();

    /**
     * Creates and registers the runtime configuration objects.
     */
    public void init (RootDObjectManager omgr)
    {
        registerObject(omgr, _confReg, "server", server);
        registerObject(omgr, _confReg, "money", money);
        registerObject(omgr, _confReg, "costs", costs);
        registerObject(omgr, _confReg, "subscription", subscription);
    }

    /**
     * Get the cost, in coins, of the specified field of the CostsConfigObject.
     */
    public int getCoinCost (String costsFieldName)
    {
        int value = ((Integer)costs.getAttribute(costsFieldName)).intValue();
        return (value >= 0) ? value : _exchange.barsToCoinsFloor(-value);
    }

    /**
     * Get the cost, in bars, of the specified field of the CostsConfigObject.
     */
    public int getBarCost (String costsFieldName)
    {
        int value = ((Integer)costs.getAttribute(costsFieldName)).intValue();
        return (value <= 0) ? -value : _exchange.coinsToBars(value);
    }

    protected void registerObject (RootDObjectManager omgr, ConfigRegistry confReg,
                                   String key, DObject object)
    {
        // register the object with the distributed object system
        omgr.registerObject(object);
        // set the tight-ass access controller
        object.setAccessController(new AdminAccessController(omgr));
        // register the object with the config object registry
        confReg.registerObject(key, key, object);
    }

    /** An access controller that provides stricter-than-normal access for config objects. */
    protected static class AdminAccessController implements AccessController {
        public AdminAccessController (RootDObjectManager omgr) {
            _omgr = omgr;
        }

        public boolean allowSubscribe (DObject object, Subscriber<?> subscriber) {
            // if the subscriber is a presents proxy, just say no - we don't use this
            if (subscriber instanceof ProxySubscriber) {
                return false;
            }

            // allow any server subscription
            return true;
        }

        public boolean allowDispatch (DObject object, DEvent event) {
            // look up the user object of the event originator
            int sourceOid = event.getSourceOid();
            if (sourceOid == -1) {
                return true; // server: ok
            }

            DObject obj = _omgr.getObject(sourceOid);
            if (!(obj instanceof MemberObject)) {
                return false;
            }

            // make sure the originator is an admin
            MemberObject user = (MemberObject)obj;
            if (!user.tokens.isAdmin()) {
                return false;
            }

            // non-maintainers can only update reboot related fields
            if (!isRebootUpdate(event) && !user.tokens.isMaintainer()) {
                return false;
            }

            // admins are allowed to change things, but let's log it
            log.info("Admin configuration change", "who", user.username,
                     "object", object.getClass().getName(), "change", event);
            return true;
        }

        protected boolean isRebootUpdate (DEvent event) {
            if (!(event instanceof AttributeChangedEvent)) {
                return false;
            }
            AttributeChangedEvent ace = (AttributeChangedEvent)event;
            return (ace.getName().equals(ServerConfigObject.CUSTOM_REBOOT_MSG) ||
                    ace.getName().equals(ServerConfigObject.NEXT_REBOOT));
        }

        protected RootDObjectManager _omgr;
    };

    // our dependencies
    @Inject protected ConfigRegistry _confReg;
    @Inject protected MemberLocator _locator;
    @Inject protected MoneyExchange _exchange;
}
