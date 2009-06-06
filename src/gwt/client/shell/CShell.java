//
// $Id$

package client.shell;

import java.util.MissingResourceException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;

/**
 * Contains a reference to the various bits that we're likely to need in the web client interface.
 */
public class CShell
{
    /** Our credentials or null if we are not logged in. */
    public static WebCreds creds;

    /** Used to communicate with the frame. */
    public static Frame frame;

    /**
     * Returns our authentication token, or null if we don't have one.
     */
    public static String getAuthToken ()
    {
        return (creds == null) ? null : creds.token;
    }

    /**
     * Returns our member id if we're logged in, 0 if we are not.
     */
    public static int getMemberId ()
    {
        return (creds == null) ? 0 : creds.name.getMemberId();
    }

    /**
     * Returns true <em>iff</em> we're an ephemeral guest (not a permaguest or member).
     */
    public static boolean isGuest ()
    {
        return getMemberId() == 0;
    }

    /**
     * Returns true <em>iff</em> we're a permaguest (not an ephemeral guest or member).
     */
    public static boolean isPermaguest ()
    {
        return (creds != null) && (creds.role == WebCreds.Role.PERMAGUEST);
    }

    /**
     * Returns true if we have some sort of account. We may be a permaguest or a registered member,
     * or anything "greater" (validated, support, admin, etc.).
     */
    public static boolean isMember ()
    {
        return (creds != null) && creds.isMember();
    }

    /**
     * Returns true if we are a registered user or "greater", false if we're a guest or permaguest.
     */
    public static boolean isRegistered ()
    {
        return (creds != null) && creds.isRegistered();
    }

    /**
     * Returns true if we are a registered user with a validated email address, false
     * if we're a guest, permaguest or a registered member with an unvalidated email address.
     */
    public static boolean isValidated ()
    {
        return (creds != null) && creds.validated;
    }

    /**
     * Returns true if we're logged in and have support+ privileges.
     */
    public static boolean isSupport ()
    {
        return (creds != null) && creds.isSupport();
    }

    /**
     * Returns true if we're logged in and have admin+ privileges.
     */
    public static boolean isAdmin ()
    {
        return (creds != null) && creds.isAdmin();
    }

    /**
     * Returns true if we're logged in and have maintainer privileges.
     */
    public static boolean isMaintainer ()
    {
        return (creds != null) && creds.isMaintainer();
    }

    /**
     * Initializes the shell and wires up some listeners.
     */
    public static void init (Frame frame)
    {
        CShell.frame = frame;
    }

    /**
     * Looks up the appropriate response message for the supplied server-generated error.
     */
    public static String serverError (Throwable error)
    {
        if (error instanceof IncompatibleRemoteServiceException) {
            return _smsgs.xlate("version_mismatch");
        } else if (error instanceof ServiceException) {
            return serverError(error.getMessage());
        } else {
            return _smsgs.xlate("internal_error");
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
            return _smsgs.xlate(error);
        } catch (MissingResourceException e) {
            // looking up a missing translation message throws an exception, yay!
            return "[" + error + "]";
        }
    }

    /** Reports a log message to the console. */
    public static void log (String message, Object... args)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        if (args.length > 1) {
            sb.append(" [");
            for (int ii = 0, ll = args.length/2; ii < ll; ii++) {
                if (ii > 0) {
                    sb.append(", ");
                }
                sb.append(args[2*ii]).append("=").append(args[2*ii+1]);
            }
            sb.append("]");
        }
        Object error = (args.length % 2 == 1) ? args[args.length-1] : null;
        if (GWT.isScript()) {
            if (error != null) {
                sb.append(": ").append(error);
            }
            consoleLog(sb.toString(), error);
        } else {
            GWT.log(sb.toString(), (Throwable)error);
        }
    }

    /**
     * Checks if we are embedded as a facebook app.
     */
    public static boolean isFacebook ()
    {
        return frame.getEmbedding() == Frame.Embedding.FACEBOOK;
    }

    /**
     * Records a log message to the JavaScript console.
     */
    protected static native void consoleLog (String message, Object error) /*-{
        if ($wnd.console) {
            if (error != null) {
                $wnd.console.info(message, error);
            } else {
                $wnd.console.info(message);
            }
        }
    }-*/;

    protected static final ServerLookup _smsgs = GWT.create(ServerLookup.class);
}
