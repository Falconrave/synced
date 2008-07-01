//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.gwt.ui.InlineLabel;

import client.shell.Application;
import client.shell.CShell;

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
        setMember(name, null);
    }

    public void setMember (MemberName name, PopupMenu menu) 
    {
        while (getWidgetCount() > 0) {
            remove(0);
        }

        add(new InlineLabel(CShell.cmsgs.creatorBy() + " "));
        if (menu == null) {
            add(Application.memberViewLink(name.toString(), name.getMemberId()));
        } else {
            InlineLabel text = new InlineLabel(name.toString());
            text.addStyleName("LabelLink");
            menu.setTrigger(text);
            add(text);
        }
    }
}
