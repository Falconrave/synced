//
// $Id$

package client.admin;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.MemberAdminInfo;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Displays admin info for a particular member.
 */
public class MemberInfoPanel extends SmartTable
{
    public MemberInfoPanel (int memberId)
    {
        super("memberInfo", 0, 5);

        CAdmin.adminsvc.getMemberInfo(CAdmin.ident, memberId, new MsoyCallback() {
            public void onSuccess (Object result) {
                init((MemberAdminInfo)result);
            }
        });
    }

    protected void init (final MemberAdminInfo info)
    {
        if (info == null) {
            setText(0, 0, "No member with that id.");
            return;
        }

        int row;
        setWidget(0, 0, Application.memberViewLink(info.name), 2, "Name");

        row = addText("Account name:", 1, "Label");
        setText(row, 1, info.accountName);

        row = addText("Perma name:", 1, "Label");
        setText(row, 1, info.permaName == null ? "" : info.permaName);

        if (CAdmin.isAdmin()) {
            row = addText("Admin:", 1, "Label");
            setText(row, 1, ""+info.isAdmin);

            final CheckBox support = new CheckBox();
            support.setChecked(info.isSupport);
            new ClickCallback(support) {
                public boolean callService () {
                    _isSupport = support.isChecked();
                    if (_isSupport == info.isSupport) {
                        return false; // we're reverting due to failure, so do nothing
                    }
                    CAdmin.adminsvc.setIsSupport(
                        CAdmin.ident, info.name.getMemberId(), _isSupport, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    info.isSupport = _isSupport;
                    MsoyUI.info(_isSupport ? CAdmin.msgs.mipMadeSupport() :
                                CAdmin.msgs.mipMadeNotSupport());
                    return true;
                }
                public void onFailure (Throwable cause) {
                    super.onFailure(cause);
                    support.setChecked(!_isSupport);
                }
                protected boolean _isSupport;
            };
            row = addText("Support:", 1, "Label");
            setWidget(row, 1, support);

        } else {
            row = addText("Support:", 1, "Label");
            setText(row, 1, ""+info.isSupport);
        }

        row = addText("Flow:", 1, "Label");
        setText(row, 1, ""+info.flow);

        row = addText("Accum Flow:", 1, "Label");
        setText(row, 1, ""+info.accFlow);

        row = addText("Gold:", 1, "Label");
        setText(row, 1, ""+info.gold);

        row = addText("Sessions:", 1, "Label");
        setText(row, 1, ""+info.sessions);

        row = addText("Session Mins:", 1, "Label");
        setText(row, 1, ""+info.sessionMinutes);

        row = addText("Last session:", 1, "Label");
        setText(row, 1, ""+info.lastSession);

        row = addText("Humanity:", 1, "Label");
        setText(row, 1, ""+info.humanity);

        row = addText("Inviter:", 1, "Label");
        if (info.inviter != null) {
            setWidget(row, 1, infoLink(info.inviter));
        } else {
            setText(row, 1, "none");
        }

        row = addText("Invited:", 1, "Label");
        FlowPanel invited = new FlowPanel();
        for (int ii = 0; ii < info.invitees.size(); ii++) {
            if (ii > 0) {
                invited.add(MsoyUI.createHTML(", ", "inline"));
            }
            invited.add(infoLink(info.invitees.get(ii)));
        }
        setWidget(row, 1, invited);

        // TODO
    }

    protected Widget infoLink (MemberName name)
    {
        return Application.createLink(
            "" + name, Page.ADMIN, Args.compose("info", name.getMemberId()));
    }
}
