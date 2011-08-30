//
// $Id$

package com.threerings.msoy.apps.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.gwt.ClientMode;

/**
 * Runtime information for an application.
 */
public class AppInfo
    implements IsSerializable
{
    /** Database-enforced maximum length of the name field. */
    static public final int MAX_NAME_LENGTH = 40;

    /** The unique id of the application. */
    public int appId;

    /** The name of the application. */
    public String name;

    /** The mode for the client when running this app. */
    public ClientMode clientMode;
}
