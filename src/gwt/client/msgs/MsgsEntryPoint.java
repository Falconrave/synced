//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.web.client.GroupService;
import com.threerings.msoy.web.client.GroupServiceAsync;
import com.threerings.msoy.web.client.MailService;
import com.threerings.msoy.web.client.MailServiceAsync;
import com.threerings.msoy.web.client.PersonService;
import com.threerings.msoy.web.client.PersonServiceAsync;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.client.ProfileServiceAsync;

import client.shell.MsoyEntryPoint;

/**
 * Configures our {@link CMsgs} for msgs-derived pages.
 */
public abstract class MsgsEntryPoint extends MsoyEntryPoint
{
    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CMsgs.profilesvc = (ProfileServiceAsync)GWT.create(ProfileService.class);
        ((ServiceDefTarget)CMsgs.profilesvc).setServiceEntryPoint("/profilesvc");
        CMsgs.personsvc = (PersonServiceAsync)GWT.create(PersonService.class);
        ((ServiceDefTarget)CMsgs.personsvc).setServiceEntryPoint("/personsvc");
        CMsgs.mailsvc = (MailServiceAsync)GWT.create(MailService.class);
        ((ServiceDefTarget)CMsgs.mailsvc).setServiceEntryPoint("/mailsvc");
        CMsgs.groupsvc = (GroupServiceAsync)GWT.create(GroupService.class);
        ((ServiceDefTarget)CMsgs.groupsvc).setServiceEntryPoint("/groupsvc");

        // load up our translation dictionaries
        CMsgs.mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    }
}
