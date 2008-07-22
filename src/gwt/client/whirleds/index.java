//
// $Id$

package client.whirleds;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.fora.data.Issue;
import com.threerings.msoy.group.data.Group;

import client.msgs.ForumModels;
import client.msgs.ForumPanel;
import client.msgs.IssueModels;
import client.msgs.IssuePanel;
import client.msgs.MsgsEntryPoint;
import client.msgs.ThreadPanel;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("d")) {
            setContent(_detail);
            _detail.setGroup(args.get(1, 0), args.get(2, "").equals("r"));

        } else if (action.equals("edit")) {
            int groupId = args.get(1, 0);
            if (groupId == 0) {
                setContent(new GroupEdit());
            } else {
                Group group = _detail.getGroup();
                if (group == null && group.groupId != groupId) {
                    MsoyUI.error("ZOMG! That's not supported yet."); // pants! TODO
                    return;
                }
                setContent(new GroupEdit(group, _detail.getGroupExtras()));
            }

        } else if (action.equals("mywhirleds")) {
            byte sortMethod = (byte) args.get(1, 0);
            MyWhirleds myWhirleds = new MyWhirleds(sortMethod);
            setContent(CWhirleds.msgs.myWhirledsTitle(), myWhirleds);

        } else if (action.equals("unread")) {
            ForumPanel fpanel = new ForumPanel(_fmodels);
            fpanel.displayUnreadThreads(false);
            setContent(CWhirleds.msgs.myForumsTitle(), fpanel);

        } else if (action.equals("f")) {
            ForumPanel forums = new ForumPanel(_fmodels);
            forums.displayGroupThreads(args.get(1, 0));
            setContent(CWhirleds.msgs.forumsTitle(), forums);

        } else if (action.equals("t")) {
            ThreadPanel tpanel = new ThreadPanel();
            tpanel.showThread(_fmodels, args.get(1, 0), args.get(2, 0), args.get(3, 0));
            setContent(CWhirleds.msgs.forumsTitle(), tpanel);

        } else if (action.equals("owned") && CWhirleds.isSupport()) {
            int type = args.get(1, Issue.TYPE_BUG);
            IssuePanel issues = new IssuePanel(_imodels);
            issues.displayOwnedIssues(type, Issue.STATE_OPEN, false);
            setContent(CWhirleds.msgs.myIssuesTitle(), issues);

        } else if (action.equals("assign") && CWhirleds.isSupport()) {
            int messageId = args.get(1, 0), page = args.get(2, 0);
            IssuePanel issues = new IssuePanel(_imodels);
            issues.displayAssignIssues(Issue.TYPE_BUG, messageId, page);
            setContent(CWhirleds.msgs.issuesTitle(), issues);

        } else if (action.equals("b") || action.equals("assign") || action.equals("owned")) {
            int type = args.get(1, Issue.TYPE_BUG), state = args.get(2, Issue.STATE_OPEN);
            IssuePanel issues = new IssuePanel(_imodels);
            issues.displayIssues(type, state, false);
            setContent(CWhirleds.msgs.issuesTitle(), issues);

        } else if (action.equals("a")) {
            int messageId = args.get(1, 0), page = args.get(2, 0), issueId = args.get(3, 0);
            IssuePanel issues = new IssuePanel(_imodels);
            if (CWhirleds.isSupport()) {
                issues.displayIssue(issueId, 0, messageId, page);
            } else {
                issues.displayIssue(issueId, 0);
            }
            setContent(CWhirleds.msgs.issuesTitle(), issues);

        } else if (action.equals("i")) {
            int issueId = args.get(1, 0), owned = args.get(2, 0);
            IssuePanel issues = new IssuePanel(_imodels);
            issues.displayIssue(issueId, owned);
            setContent(CWhirleds.msgs.issuesTitle(), issues);

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

    @Override // from Page
    protected String getPageId ()
    {
        return WHIRLEDS;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CWhirleds.msgs = (WhirledsMessages)GWT.create(WhirledsMessages.class);
    }

    protected ForumModels _fmodels = new ForumModels();
    protected IssueModels _imodels = new IssueModels();
    protected WhirledDetailPanel _detail = new WhirledDetailPanel();
    protected GalaxyPanel _galaxy;
}
