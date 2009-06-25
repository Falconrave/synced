//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.gwt.ui.InlineLabel;

import client.shell.ShellMessages;
import client.util.Link;

/**
 * Displays a creator's name with "by Foozle" where Foozle is a link to the creator's profile page.
 */
public class CreatorLabel extends FlowPanel
{
    public CreatorLabel ()
    {
        this(null);
    }

    public CreatorLabel (MemberName name)
    {
        addStyleName("creator");
        if (name != null) {
            setMember(name);
        }
    }

    public void setMember (MemberName name)
    {
        clear();
        add(new InlineLabel(_cmsgs.creatorBy() + " "));
        add(Link.memberView(name.toString(), name.getMemberId()));
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
