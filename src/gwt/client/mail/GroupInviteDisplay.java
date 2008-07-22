//
// $Id$

package client.mail;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.person.data.GroupInvitePayload;
import com.threerings.msoy.web.client.GroupService;

import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Displays a group invitation payload.
 */
public class GroupInviteDisplay extends MailPayloadDisplay
{
    @Override // from MailPayloadDisplay
    public Widget widgetForRecipient ()
    {
        return new DisplayWidget(_invitePayload.responded == false);
    }

    @Override // from MailPayloadDisplay
    protected void didInit ()
    {
        // no sanity checks: if anything breaks here, it's already a disaster
        _invitePayload = (GroupInvitePayload) _message.payload;
    }

    protected class DisplayWidget extends FlowPanel
    {
        public DisplayWidget (boolean enabled)
        {
            super();
            _enabled = enabled;
            setStyleName("groupInvitation");
            refreshUI();
        }

        protected void refreshUI ()
        {
            CMail.groupsvc.getGroupInfo(CMail.ident, _invitePayload.groupId,
                new MsoyCallback<GroupService.GroupInfo>() {
                    public void onSuccess (GroupService.GroupInfo result) {
                        _info = result;
                        buildUI();
                    }
                });
        }

        protected void buildUI ()
        {
            clear();
            if (_enabled && _info.rank != GroupMembership.RANK_NON_MEMBER) {
                add(MsoyUI.createLabel(CMail.msgs.groupAlreadyMember(""+_info.name), null));
                return;
            }

            add(new InlineLabel(CMail.msgs.groupInvitation(""+_info.name), true, false, true));
            add(WidgetUtil.makeShim(5, 5));
            Button joinButton = new Button(CMail.msgs.groupBtnJoin(), new ClickListener() {
                public void onClick (Widget sender) {
                    joinGroup();
                    refreshUI();
                }
            });
            joinButton.addStyleName("JoinButton");
            joinButton.setEnabled(_enabled);
            add(joinButton);
        }

        protected void joinGroup ()
        {
            CMail.groupsvc.joinGroup(CMail.ident, _invitePayload.groupId,
                new MsoyCallback<Void>() {
                    // if joining the group succeeds, mark this invitation as accepted
                    public void onSuccess (Void result) {
                        _invitePayload.responded = true;
                        updateState(_invitePayload, new MsoyCallback<Void>() {
                            // and if that succeded to, let the mail app know to refresh
                            public void onSuccess (Void result) {
//                               if (_listener != null) {
//                                  _listener.messageChanged(_convoId, _message);
//                               }
                            }
                        });
                    }
                });
        }

        protected boolean _enabled;
        protected GroupService.GroupInfo _info;
    }

    protected GroupInvitePayload _invitePayload;
}
