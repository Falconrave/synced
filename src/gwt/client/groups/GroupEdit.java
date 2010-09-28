//
// $Id$

package client.groups;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.gwt.GroupExtras;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.TagHistory;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

import client.item.TagDetailPanel;
import client.money.BuyPanel;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.util.Link;
import client.util.InfoCallback;

/**
 * A popup that lets a member of sufficient rank modify a group's metadata.
 */
public class GroupEdit extends FlexTable
{
    /**
     * Create a new group.
     *
     * Please note that this constructor ends up triggering a service request to
     * get the PriceQuote for creating a group.
     */
    public GroupEdit ()
    {
        this(new Group(), new GroupExtras());
    }

    /**
     * Edit an existing group.
     */
    public GroupEdit (Group group, GroupExtras extras)
    {
        // permaguests are not allowed to create groups
        String error = CShell.isRegistered() ?
            (CShell.isValidated() ? null : _msgs.editMustValidate()) : _msgs.editMustRegister();
        if (error != null) {
            setHTML(0, 0, error);
            getFlexCellFormatter().setStyleName(0, 0, "infoLabel");
            return;
        }

        _group = group;
        _extras = extras;
        boolean isCreate = (_group.groupId == 0);

        // if this is a blank group, set up some defaults
        if (isCreate) {
            _group.policy = Group.Policy.PUBLIC;
            _group.threadPerm = Group.Perm.MEMBER;
            _group.postPerm = Group.Perm.ALL;
            _group.partyPerm = Group.Perm.MEMBER;
        }

        setStyleName("groupEditor");
        setCellSpacing(5);
        setCellPadding(0);

        CShell.frame.setTitle(isCreate ? _msgs.editCreateTitle() : group.name);

        // set up our editor contents
        // TEMP: only allow group name editing when creating
        if (isCreate || CShell.isSupport()) {
            _name = MsoyUI.createTextBox(_group.name, GroupName.LENGTH_MAX, 20);
            addRow(_msgs.editName(), _name);

        } else {
            addRow(_msgs.editName(), new Label(_group.name));
        }

        // make sure the group's configured policy is consistent with what's shown in the GUI
        _policy = new ListBox();
        _policy.addItem(_msgs.policyPublic());
        _policy.addItem(_msgs.policyInvite());
        _policy.addItem(_msgs.policyExclusive());
        _policy.setSelectedIndex(_group.policy.ordinal());
        addRow(_msgs.editPolicy(), _policy);

        _party = new ListBox();
        _party.addItem(_msgs.permsMember());
        _party.addItem(_msgs.permsManager());
        _party.setSelectedIndex(_group.partyPerm.ordinal() - Group.Perm.MEMBER.ordinal());
        addRow(_msgs.partyPerm(), _party);

        _thread = new ListBox();
        _thread.addItem(_msgs.permsAll());
        _thread.addItem(_msgs.permsMember());
        _thread.addItem(_msgs.permsManager());
        _thread.setSelectedIndex(_group.threadPerm.ordinal());
        addRow(_msgs.editThread(), _thread);

        _post = new ListBox();
        _post.addItem(_msgs.permsAll());
        _post.addItem(_msgs.permsMember());
        _post.addItem(_msgs.permsManager());
        _post.setSelectedIndex(_group.postPerm.ordinal());
        addRow(_msgs.editPost(), _post);

        addRow(_msgs.editLogo(), _logo = new PhotoChoiceBox(true, null));
        _logo.setMedia(_group.getLogo());

        _blurb = MsoyUI.createTextBox(_group.blurb, Group.MAX_BLURB_LENGTH, 40);
        addRow(_msgs.editBlurb(), _blurb);

        _homepage = MsoyUI.createTextBox(_extras.homepageUrl, 255, 40);
        addRow(_msgs.editHomepage(), _homepage);

        _charter = new LimitedTextArea(Group.MAX_CHARTER_LENGTH, 60, 3);
        _charter.setText(_extras.charter);
        addRow(_msgs.editCharter(), _charter);

        int jj = 0;
        _catalogTypes = new MsoyItemType[MsoyItemType.values().length];
        _catalogType = new ListBox();
        for (MsoyItemType type : MsoyItemType.values()) {
            if (!type.isShopType()) {
                continue;
            }
            _catalogType.addItem(_dmsgs.xlateItemType(type));
            if (_extras.catalogItemType == type) {
                _catalogType.setSelectedIndex(jj);
            }
            _catalogTypes[jj] = type;
            jj ++;
        }
        addRow(_msgs.editCatalogType(), _catalogType);
        addRow(_msgs.editCatalogTag(),
            _catalogTag = MsoyUI.createTextBox(_extras.catalogTag, 24, 24));

        if (CShell.isAdmin()) {
            addRow(_msgs.editOfficial(), _official = new CheckBox());
            _official.setValue(_group.official);
        }

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(_cancel = new Button(_cmsgs.cancel(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                Link.go(Pages.GROUPS, (_group.groupId == 0) ? "" :
                        Args.compose("d", _group.groupId));
            }
        }));
        footer.add(WidgetUtil.makeShim(5, 5));
        if (isCreate) {
            footer.add(new GroupBuyPanel().createPromptHost(_msgs.createNew()));

        } else {
            footer.add(_submit = new Button(_cmsgs.change(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    updateGroup();
                }
            }));
        }
        int frow = getRowCount();
        setWidget(frow, 1, footer);
        getFlexCellFormatter().setHorizontalAlignment(frow, 1, HasAlignment.ALIGN_RIGHT);

        // TODO integrate tags into the main form
        if (!isCreate && (_group.policy != Group.Policy.EXCLUSIVE)) {
            TagDetailPanel tags = new TagDetailPanel(new TagDetailPanel.TagService() {
                public void tag (String tag, AsyncCallback<TagHistory> cback) {
                    _groupsvc.tagGroup(_group.groupId, tag, true, cback);
                }
                public void untag (String tag, AsyncCallback<TagHistory> cback) {
                    _groupsvc.tagGroup(_group.groupId, tag, false, cback);
                }
                public void getTagHistory (AsyncCallback<List<TagHistory>> cback) {
                    _groupsvc.getTagHistory(_group.groupId, cback);
                }
                public void getTags (AsyncCallback<List<String>> cback) {
                    _groupsvc.getTags(_group.groupId, cback);
                }
                public void addMenuItems (final String tag, PopupMenu menu, boolean canEdit) {
                    menu.addMenuItem(_msgs.detailTagLink(), new Command() {
                        public void execute () {
                            Link.go(Pages.GROUPS, "tag", "0", tag);
                        }
                    });
                }
            }, null, null, null, true);
            addRow("", WidgetUtil.makeShim(1, 20));
            addRow("", tags);
        }
    }

    protected void addRow (String label, Widget contents)
    {
        int row = getRowCount();
        setText(row, 0, label);
        getFlexCellFormatter().setStyleName(row, 0, "nowrapLabel");
        getFlexCellFormatter().addStyleName(row, 0, "rightLabel");
        setWidget(row, 1, contents);
    }

    /**
     * Copy settings in UI fields back into _group and _extras.
     *
     * @return true if we're ready to go
     */
    protected boolean commitEdits ()
    {
        // extract our values
        if (_name != null) {
            // validate the name
            String name = _name.getText().trim();
            if (!GroupName.isValidName(name)) {
                MsoyUI.error(_msgs.errInvalidGroupName());
                return false;
            }
            _group.name = name;
        } // else keep name the same
        _group.logo = _logo.getMedia();
        _group.blurb = _blurb.getText().trim();
        _group.policy = Group.Policy.values()[_policy.getSelectedIndex()];
        _group.partyPerm = Group.Perm.values()[
            Group.Perm.MEMBER.ordinal() + _party.getSelectedIndex()];
        _group.threadPerm = Group.Perm.values()[_thread.getSelectedIndex()];
        _group.postPerm = Group.Perm.values()[_post.getSelectedIndex()];
        if (_official != null) {
            _group.official = _official.getValue();
        }
        _extras.charter = _charter.getText().trim();
        _extras.homepageUrl = _homepage.getText().trim();
        _extras.catalogItemType = _catalogTypes[_catalogType.getSelectedIndex()];
        _extras.catalogTag = _catalogTag.getText().trim();
        return true;
    }

    /**
     * Called to save changes when editing an existing group.
     */
    protected void updateGroup ()
    {
        if (commitEdits()) {
            _groupsvc.updateGroup(_group, _extras, new InfoCallback<Void>(_submit) {
                public void onSuccess (Void result) {
                    Link.go(Pages.GROUPS, "d", _group.groupId, "r");
                }
            });
        }
    }

    protected class GroupBuyPanel extends BuyPanel<Group>
    {
        public GroupBuyPanel ()
        {
            _groupsvc.quoteCreateGroup(new InfoCallback<PriceQuote>(_cancel) {
                public void onSuccess (PriceQuote quote) {
                    init(quote, new AsyncCallback<Group>() {
                        public void onSuccess (Group group) {
                            Link.go(Pages.GROUPS, "d", group.groupId, "r");
                        }
                        public void onFailure (Throwable t) {} /* not used */
                    });
                }
            });
        }

        @Override
        protected boolean makePurchase (
            Currency currency, int amount, AsyncCallback<PurchaseResult<Group>> listener)
        {
            boolean editsOk = commitEdits();
            if (editsOk) {
                _groupsvc.createGroup(_group, _extras, currency, amount, listener);
            }
            return editsOk;
        }
    }

    protected Group _group;
    protected GroupExtras _extras;

    protected TextBox _name, _blurb, _homepage, _catalogTag;
    protected PhotoChoiceBox _logo;
    protected ListBox _policy, _party, _thread, _post, _catalogType;
    protected LimitedTextArea _charter;
    protected Button _cancel, _submit;
    protected CheckBox _official;

    protected MsoyItemType[] _catalogTypes;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
