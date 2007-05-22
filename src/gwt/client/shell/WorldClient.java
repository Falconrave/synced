//
// $Id$

package client.shell;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.WebCreds;

import client.util.FlashClients;

/**
 * Manages our World client (which also handles Flash games).
 */
public class WorldClient extends Widget
{
    public static void displayFlash (String flashArgs)
    {
        // let the page know that we're displaying a client
        boolean newPage = Page.setShowingClient(true, false);

        // create our client if necessary
        if (_fclient == null) {
            clearClient(false); // clear our Java client if we have one
            if (CShell.ident != null) {
                flashArgs = "token=" + CShell.ident.token +
                    (flashArgs == null ? "" : ("&" + flashArgs));;
            }
            RootPanel.get("client").clear();
            RootPanel.get("client").add(_fclient = FlashClients.createWorldClient(flashArgs));

        } else {
            // don't tell the client anything if we're just restoring our URL
            if (newPage) {
                clientGo(flashArgs);
            }
            clientMinimized(false);
        }
    }

    public static void displayJava (Widget client)
    {
        // let the page know that we're displaying a client
        boolean newPage = Page.setShowingClient(false, true);

        if (_jclient != client) {
            if (newPage) {
                clearClient(false); // clear out our flash client if we have one
                RootPanel.get("client").clear();
                RootPanel.get("client").add(_jclient = client);
            }
        } else {
            clientMinimized(false);
        }
    }

    public static void minimize ()
    {
        // note that we don't need to hack our popups
        Page.displayingFlash = false;

        if (_fclient != null || _jclient != null) {
            RootPanel.get("client").setWidth("300px");
            clientMinimized(true);
        }
    }

    public static void clearClient (boolean restoreContent)
    {
        if (_fclient != null || _jclient != null) {
            if (_fclient != null) {
                clientUnload(); // TODO: make this work for jclient
            }
            RootPanel.get("client").clear();
            _fclient = _jclient = null;
        }
        if (restoreContent) {
            RootPanel.get("client").setWidth("0px");
            RootPanel.get("content").setWidth("100%");
        }
    }

    public static void didLogon (WebCreds creds)
    {
        if (_fclient != null) {
            clientLogon(creds.getMemberId(), creds.token);
        }
        // TODO: let jclient know about logon?
    }

    public static void didLogoff ()
    {
        clearClient(true);
    }

    /**
     * Tells the World client to go to a particular location.
     */
    protected static native boolean clientGo (String where) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.clientGo(where);
                return true;
            } catch (e) {
                // nada
            }
        }
        return false;
    }-*/;

    /**
     * Logs on the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientLogon (int memberId, String token) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.clientLogon(memberId, token);
            } catch (e) {
                // nada
            }
        }
    }-*/;

    /**
     * Logs off the MetaSOY Flash client using magical JavaScript.
     */
    protected static native void clientUnload () /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.onUnload();
            } catch (e) {
                // nada
            }
        }
    }-*/;

    /**
     * Notifies the flash client that we're either minimized or not.
     */
    protected static native void clientMinimized (boolean mini) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            try {
                client.setMinimized(mini);
            } catch (e) {
                // nada
            }
        }
    }-*/;

    protected static Widget _fclient, _jclient;
}
