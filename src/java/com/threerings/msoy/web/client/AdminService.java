//
// $Id$

package com.threerings.msoy.web.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines remote services available to admins.
 */
public interface AdminService extends RemoteService
{
    /**
     * Loads the configuration needed to run the Dashboard applet.
     */
    public ConnectConfig loadConnectConfig (WebIdent ident)
        throws ServiceException;

    /**
     * Creates accounts for the supplied email addresses and sends invitation emails to same.
     * Returns a string for each address denoting success or failure.
     */
    public String[] registerAndInvite (WebIdent ident, String[] emails)
        throws ServiceException;

    /** 
     * Grants the given number of invitations to the indicated user set.
     *
     * @param activeSince If null, all users will receive invitations
     */
    public void grantInvitations (WebIdent ident, int numberInvitations, Date activeSince)
        throws ServiceException;
}
