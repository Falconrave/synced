//
// $Id$

package client.shell;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.WebUserServiceAsync;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

/**
 * Contains a reference to the various bits that we're likely to need in the web client interface.
 */
public class CShell
{
    /** Our credentials or null if we are not logged in. */
    public static WebCreds creds;

    /** Provides user-related services. */
    public static WebUserServiceAsync usersvc;

    /** Provides member-related service. */
    public static MemberServiceAsync membersvc;

    /** Messages shared by all client interfaces. */
    public static ShellMessages cmsgs;

    /** Messages that must be looked up dynamically. */
    public static DynamicMessages dmsgs;

    /** Contains translations for server-supplied messages. */
    public static ServerMessages smsgs;

    /**
     * Returns our member id if we're logged in, 0 if we are not.
     */
    public static int getMemberId ()
    {
        return (creds != null) ? creds.getMemberId() : 0;
    }

    /**
     * Returns true if we're logged in and have support privileges.
     */
    public static boolean isSupport ()
    {
        return (creds != null) ? creds.isSupport : false;
    }

    /**
     * Returns true if we're logged in and have admin privileges.
     */
    public static boolean isAdmin ()
    {
        return (creds != null) ? creds.isAdmin : false;
    }

    /**
     * Looks up the appropriate response message for the supplied server-generated error.
     */
    public static String serverError (Throwable error)
    {
        if (error instanceof ServiceException) {
            return serverError(error.getMessage());
        } else {
            return smsgs.getString("internal_error");
        }
    }

    /**
     * Looks up the appropriate response message for the supplied server-generated error.
     */
    public static String serverError (String error)
    {
        // ConstantsWithLookup can't handle things that don't look like method names, yay!
        if (error.startsWith("m.") || error.startsWith("e.")) {
            error = error.substring(2);
        }
        try {
            return smsgs.getString(error);
        } catch (MissingResourceException e) {
            // looking up a missing translation message throws an exception, yay!
            return "[" + error + "]";
        }
    }

    /** Reports a log message to the console. */
    public static void log (String message)
    {
        if (GWT.isScript()) {
            consoleLog(message);
        } else {
            GWT.log(message, null);
        }
    }

    /** Reports a log message and exception stack trace to the console. */
    public static void log (String message, Throwable error)
    {
        if (GWT.isScript()) {
            consoleLog(message + ": " + error); // TODO: log stack trace?
        } else {
            GWT.log(message, error);
        }
    }

    /**
     * Records a log message to the JavaScript console.
     */
    protected static native void consoleLog (String message) /*-{
        if ($wnd.console) {
            $wnd.console.log(message);
        }
    }-*/;
}
