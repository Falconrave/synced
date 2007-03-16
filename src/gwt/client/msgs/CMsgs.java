//
// $Id$

package client.msgs;

import com.threerings.msoy.web.client.GroupServiceAsync;
import com.threerings.msoy.web.client.ItemServiceAsync;
import com.threerings.msoy.web.client.MailServiceAsync;
import com.threerings.msoy.web.client.ProfileServiceAsync;

import client.shell.CShell;

/**
 * Extends {@link CShell} and provides mail-message-specific services.
 */
public class CMsgs extends CShell
{
    /** Provides profile-related services. */
    public static ProfileServiceAsync profilesvc;

    /** Provides group-related services. */
    public static GroupServiceAsync groupsvc;

    /** Provides mail-related services. */
    public static MailServiceAsync mailsvc;

    /** Provides item-related services. */
    public static ItemServiceAsync itemsvc;

    /** Messages used by the msgs interfaces. */
    public static MsgsMessages mmsgs;
}
