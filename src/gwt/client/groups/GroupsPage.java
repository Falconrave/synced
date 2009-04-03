//
// $Id$

package client.groups;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.msgs.ForumModels;
import client.msgs.ForumPanel;
import client.msgs.ThreadPanel;
import client.shell.Page;
import client.ui.MsoyUI;

public class GroupsPage extends Page
{
    public enum Nav {
        DETAIL("d"), EDIT("edit"), MYGROUPS("mygroups"), UNREAD("unread"), FORUM("f"), POST("p"),
        THREAD("t"), MEDALS("m"), CREATEMEDAL("cm"), EDITMEDAL("em"), DEFAULT("");

        public static Nav getGroupPage (Args args)
        {
            String action = args.get(0, "");
            for (Nav page : Nav.values()) {
                if (page.getNavToken().equals(action)) {
                    return page;
                }
            }
            return Nav.DEFAULT;
        }

        public String getNavToken ()
        {
            return _navToken;
        }

        public String composeArgs (Object... args)
        {
            List<String> stringArgs = new ArrayList<String>();
            stringArgs.add(getNavToken());
            for (int ii = 0; ii < args.length; ii++) {
                stringArgs.add("" + args[ii]);
            }
            return Args.compose(stringArgs);
        }

        Nav (String navToken)
        {
            _navToken = navToken;
        }

        protected String _navToken;
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        Nav page = Nav.getGroupPage(args);

        if (page == Nav.DETAIL) {
            setContent(_detail);
            _detail.setGroup(args.get(1, 0), args.get(2, "").equals("r"));

        } else if (page == Nav.EDIT) {
            int groupId = args.get(1, 0);
            if (groupId == 0) {
                setContent(new GroupEdit());
            } else {
                Group group = _detail.getGroup();
                if (group == null || group.groupId != groupId) {
                    MsoyUI.error("ZOMG! That's not supported yet."); // pants! TODO
                    return;
                }
                setContent(new GroupEdit(group, _detail.getGroupExtras()));
            }

        } else if (page == Nav.MYGROUPS) {
            byte sortMethod = (byte) args.get(1, 0);
            MyGroups myGroups = new MyGroups(sortMethod);
            setContent(_msgs.myGroupsTitle(), myGroups);

        } else if (page == Nav.UNREAD) {
            showForumPanel(ForumPanel.Mode.UNREAD, 0, args.get(1, ""), args.get(2, 0));

        } else if (page == Nav.FORUM) {
            showForumPanel(ForumPanel.Mode.GROUPS, args.get(1, 0), args.get(2, ""), args.get(3, 0));

        } else if (page == Nav.POST) {
            showForumPanel(ForumPanel.Mode.NEW_THREAD, args.get(1, 0), null, 0);

        } else if (page == Nav.THREAD) {
            ThreadPanel tpanel = new ThreadPanel();
            tpanel.showThread(_fmodels, args.get(1, 0), args.get(2, 0), args.get(3, 0));
            setContent(_msgs.forumsTitle(), tpanel);

        } else if (page == Nav.MEDALS) {
            int groupId = args.get(1, 0);
            setContent(_msgs.medalListTitle(), new MedalListPanel(groupId));

        } else if (page == Nav.CREATEMEDAL) {
            int groupId = args.get(1, 0);
            setContent(_msgs.editMedalCreate(), new EditMedalPanel(groupId, 0));

        } else if (page == Nav.EDITMEDAL) {
            int medalId = args.get(1, 0);
            setContent(_msgs.editMedalEdit(), new EditMedalPanel(0, medalId));

        } else {
            if (_galaxy == null) {
                _galaxy = new GalaxyPanel();
            }
            if (getContent() != _galaxy) {
                setContent(_galaxy);
            }
            _galaxy.setArgs(args);
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.GROUPS;
    }

    protected void showForumPanel (ForumPanel.Mode mode, int groupId, String search, int page)
    {
        if (_fpanel == null || !_fpanel.isInMode(mode, groupId)) {
            _fpanel = new ForumPanel(_fmodels, mode, groupId);
        }
        _fpanel.setPage(search, page);
        setContent(_msgs.forumsTitle(), _fpanel);
    }

    protected ForumModels _fmodels = new ForumModels();
    protected ForumPanel _fpanel;
    protected GroupDetailPanel _detail = new GroupDetailPanel();
    protected GalaxyPanel _galaxy;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
}
