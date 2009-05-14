//
// $Id$

package client.frame;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.BillingUtil;
import client.util.Link;

/**
 * Displays our sub-navigation.
 */
public class SubNaviPanel extends FlowPanel
{
    public SubNaviPanel (Tabs tab)
    {
        reset(tab);
    }

    /**
     * Resets the subnavigation to the default for the specified tab.
     */
    public void reset (Tabs tab)
    {
        clear();

        int memberId = CShell.getMemberId();
        switch (tab) {
        case ME:
            if (CShell.isGuest()) {
                addLink(null, "Home", Pages.LANDING, "");
            } else {
                addLink(null, "Me", Pages.ME, "");
                addImageLink("/images/me/menu_home.png", "Home", Pages.WORLD, "m" + memberId);
                addLink(null, "My Rooms", Pages.PEOPLE, Args.compose("rooms", memberId));
                if (CShell.isRegistered()) {
                    addLink(null, "Friends", Pages.PEOPLE, "");
                    addLink(null, "Account", Pages.ACCOUNT, "edit");
                }
                if (CShell.isSupport()) {
                    addLink(null, "Admin", Pages.ADMINZ, "");
                }
            }
            break;

        case STUFF:
            if (CShell.isMember()) {
                addLink(null, "My Stuff", Pages.STUFF, "");
            }
            break;

        case GAMES:
            addLink(null, "Games", Pages.GAMES, "");
            if (CShell.isMember()) {
                addLink(null, "My Trophies", Pages.GAMES, Args.compose("t", memberId));
            }
            addLink(null, "New Games", Pages.GAMES, Args.compose("g", -1, 1));
            addLink(null, "My Games", Pages.GAMES, "mine");
            break;

        case ROOMS:
            addLink(null, "Rooms", Pages.ROOMS, "");
            if (CShell.isMember()) {
                addImageLink("/images/me/menu_home.png", "Home", Pages.WORLD, "m" + memberId);
                addLink(null, "My Rooms", Pages.PEOPLE, Args.compose("rooms", memberId));
            }
            break;

        case GROUPS:
            addLink(null, "Groups", Pages.GROUPS, "");
            if (CShell.isRegistered()) {
                addLink(null, "My Groups", Pages.GROUPS, "mygroups");
                addLink(null, "My Discussions", Pages.GROUPS, "unread");
                if (CShell.isSupport()) {
                    addLink(null, "Issues", Pages.ISSUES, "");
                    addLink(null, "My Issues", Pages.ISSUES, "mine");
                }
            }
            break;

        case SHOP:
            addLink(null, "Shop", Pages.SHOP, "");
            addLink(null, "My Favorites", Pages.SHOP, "f");
            if (CShell.isRegistered()) {
                addLink(null, "Transactions", Pages.ME, "transactions");
                addExternalLink("Buy Bars", BillingUtil.onBuyBars(), true);
            }
            break;

        case HELP:
            addLink(null, "Help", Pages.HELP, "");
            addLink(null, "Contact Us", Pages.SUPPORT, "");
            addLink(null, "Report Bug", Pages.GROUPS, Args.compose("f", 72));
            if (CShell.isSupport()) {
                addLink(null, "Admin", Pages.SUPPORT, "admin");
            }
            break;

        default:
            // nada
            break;
        }
    }

    public void addExternalLink (String label, ClickHandler listener, boolean sep)
    {
        addSeparator(sep);
        add(MsoyUI.createActionLabel(label, "external", listener));
    }

    public void addLink (String iconPath, String label, Pages page, String args)
    {
        addLink(iconPath, label, page, args, true);
    }

    public void addLink (String iconPath, String label, Pages page, String args, boolean sep)
    {
        addSeparator(sep);
        if (iconPath != null) {
            add(MsoyUI.createActionImage(iconPath, Link.createListener(page, args)));
            add(new HTML("&nbsp;"));
        }
        add(Link.create(label, page, args));
    }

    public void addContextLink (String label, Pages page, String args, int position)
    {
        // sanity check the position
        if (position > getWidgetCount()) {
            position = getWidgetCount();
        }
        insert(new HTML("&nbsp;&nbsp;|&nbsp;&nbsp;"), position++);
        insert(Link.create(label, page, args), position);
    }

    public Image addImageLink (String path, String tip, Pages page, String args)
    {
        addSeparator(true);
        Image icon = MsoyUI.createActionImage(path, Link.createListener(page, args));
        icon.setTitle(tip);
        add(icon);
        return icon;
    }

    protected void addSeparator (boolean sep)
    {
        if (getWidgetCount() > 0) {
            add(new HTML("&nbsp;&nbsp;" + (sep ? "|&nbsp;&nbsp;" : "")));
        }
    }
}
