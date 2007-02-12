//
// $Id$

package client.group;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Hyperlink;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupExtras;
import com.threerings.msoy.web.data.GroupDetail;
import com.threerings.msoy.web.data.GroupMembership;
import com.threerings.msoy.web.data.MemberName;

import client.shell.MsoyEntryPoint;
import client.util.MediaUtil;
import client.util.PromptPopup;
import client.util.PrettyTextPanel;
import client.util.TagDetailPanel;

import client.group.GroupEdit.GroupSubmissionListener;

/**
 * Display the details of a group, including all its members, and let managers remove other members
 * (unless the group's policy is PUBLIC) and pop up the group editor.
 */
public class GroupView extends VerticalPanel
    implements GroupSubmissionListener
{
    public GroupView (int groupId)
    {
        super();
        setWidth("100%");

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        add(_errorContainer);

        _table = new MyFlexTable();
        add(_table);

        loadGroup(groupId);
    }

    /**
     * Called by {@link GroupEdit}; reloads the group.
     */
    public void groupSubmitted (Group group)
    {
        loadGroup(group.groupId);
    }

    /**
     * Fetches the details of the group from the backend and trigger a UI rebuild.
     */
    protected void loadGroup (int groupId)
    {
        CGroup.groupsvc.getGroupDetail(CGroup.creds, groupId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _detail = (GroupDetail) result;
                _group = _detail.group;
                _extras = _detail.extras;
                // in case this object is used more than once, make sure that _me is at least 
                // not stale
                _me = null;
                if (CGroup.creds != null) {
                    _me = GroupView.findMember(_detail.members, CGroup.getMemberId());
                }
                buildUI();
            }
            public void onFailure (Throwable caught) {
                CGroup.log("loadGroup failed", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    /**
     * Rebuilds the UI from scratch.
     */
    protected void buildUI ()
    {
        _table.clear();
        _table.setStyleName("groupView");
        _table.setCellSpacing(0);
        _table.setCellPadding(0);
        if (_extras.backgroundControl == GroupExtras.BACKGROUND_FIT_TO_IMAGE) {
            _table.setWidth((160 + _extras.detailBackgroundWidth) + "px");
        }
        boolean amManager = _me != null && _me.rank == GroupMembership.RANK_MANAGER;

        VerticalPanel infoPanel = new VerticalPanel();
        infoPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        infoPanel.setStyleName("LogoPanel");
        infoPanel.setSpacing(0);
        infoPanel.add(MediaUtil.createMediaView(_group.logo, MediaDesc.THUMBNAIL_SIZE));
        HorizontalPanel links = new HorizontalPanel();
        links.setStyleName("Links");
        links.setSpacing(8);
        links.add(new Anchor("/world/index.html#g" +  _group.groupId, CGroup.msgs.viewHall()));
        // this should be added back in later... probably as "Wiki", instead of "Forum"
        //links.add(new Anchor("", CGroup.msgs.viewForum()));
        if (_extras.homepageUrl != null) {
            links.add(new Anchor(_extras.homepageUrl, CGroup.msgs.viewHomepage()));
        }
        infoPanel.add(links);
        VerticalPanel established = new VerticalPanel();
        established.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        established.setStyleName("Established");
        established.add(new InlineLabel(CGroup.msgs.viewEstablishedAbbreviated() + 
            (new SimpleDateFormat("MMM dd, yyyy")).format(_group.creationDate)));
        HorizontalPanel creatorPanel = new HorizontalPanel();
        // this inline div is not letting space display to the right of it, and we need a space.
        InlineLabel byLabel = new InlineLabel(CGroup.msgs.viewBy());
        DOM.setStyleAttribute(byLabel.getElement(), "marginRight", "3px");
        creatorPanel.add(byLabel);
        creatorPanel.add(new Anchor(MsoyEntryPoint.memberViewPath(  
            _detail.creator.getMemberId()), _detail.creator.toString()));
        established.add(creatorPanel);
        infoPanel.add(established);
        InlineLabel policy = new InlineLabel(getPolicyName(_group.policy));
        policy.setStyleName("Policy");
        infoPanel.add(policy);
        if (amManager) {
            infoPanel.add(new Button(CGroup.msgs.viewEdit(), new ClickListener() {
                public void onClick (Widget sender) {
                    new GroupEdit(_group, _extras, GroupView.this).show();
                }
            }));
        }
        if (_me == null) {
            if (_group.policy == Group.POLICY_PUBLIC) {
                infoPanel.add(new Button(CGroup.msgs.viewJoin(), new ClickListener() {
                    public void onClick (Widget sender) {
                        (new PromptPopup(CGroup.msgs.viewJoinPrompt(_group.name)) {
                            public void onAffirmative () {
                                joinGroup();
                            }
                            public void onNegative () { }
                        }).prompt();
                    }
                }));
            }
        } else {
            infoPanel.add(new Button(CGroup.msgs.viewLeave(), new ClickListener() {
                public void onClick (Widget sender) {
                    (new PromptPopup(CGroup.msgs.viewLeavePrompt(_group.name)) {
                        public void onAffirmative () {
                            removeMember(_me.member.getMemberId(), false);
                        }
                        public void onNegative () { }
                    }).prompt();
                }
            }));
        } 
        _table.setWidget(0, 0, infoPanel);
        if (_extras.infoBackground != null) {
            _table.getMyFlexCellFormatter().setBackgroundImage(0, 0, MsoyEntryPoint.toMediaPath(
                _extras.infoBackground.getMediaPath()));
            if (_extras.backgroundControl == GroupExtras.BACKGROUND_ANCHORED) {
                _table.getMyFlexCellFormatter().setBackgroundNoRepeat(0, 0);
            }
        }

        VerticalPanel description = new VerticalPanel();
        description.setStyleName("DescriptionPanel");
        description.setSpacing(0);
        Label nameLabel = new Label(_group.name);
        nameLabel.setStyleName("Name");
        description.add(nameLabel);
        if (_group.blurb != null) {
            Label blurbLabel = new Label(_group.blurb);
            blurbLabel.setStyleName("Blurb");
            description.add(blurbLabel);
        }
        if (_extras.charter != null) {
            PrettyTextPanel charter = new PrettyTextPanel(_extras.charter);
            charter.setStyleName("Charter");
            description.add(charter);
        }
        if (_extras.backgroundControl == GroupExtras.BACKGROUND_FIT_TO_IMAGE) {
            final Element div = DOM.createElement("div");
            DOM.setStyleAttribute(div, "width", _extras.detailBackgroundWidth + "px");
            DOM.setStyleAttribute(div, "height", (_extras.detailAreaHeight > 270 ? 
                _extras.detailAreaHeight : 270) + "px");
            // when not tiling, we need a sensible default for people that are in a foot-shooting 
            // mood and create more text than fits on their background image.  Here we're creating
            // a div to hold the table that will scroll when overflowing
            DOM.setStyleAttribute(div, "overflow", "auto");
            DOM.appendChild(div, description.getElement());
            Widget descriptionWidget = new Widget() {
                {
                    setElement(div);
                }
            };
            _table.setWidget(0, 1, descriptionWidget);
        } else if (_extras.backgroundControl == GroupExtras.BACKGROUND_TILED ||
            _extras.backgroundControl == GroupExtras.BACKGROUND_ANCHORED) {
            _table.setWidget(0, 1, description);
            _table.getMyFlexCellFormatter().fillWidth(0, 1);
        }
        if (_extras.detailBackground != null) {
            _table.getMyFlexCellFormatter().setBackgroundImage(0, 1, 
                MsoyEntryPoint.toMediaPath(_extras.detailBackground.getMediaPath()));
            if (_extras.backgroundControl == GroupExtras.BACKGROUND_ANCHORED) {
                _table.getMyFlexCellFormatter().setBackgroundNoRepeat(0, 1);
            }
        }

        FlexTable people = new FlexTable();
        people.setStyleName("PeoplePanel");
        people.setWidth("100%");
        people.setText(0, 0, CGroup.msgs.viewManagers());
        people.setText(1, 0, CGroup.msgs.viewMembers());
        people.setText(3, 0, CGroup.msgs.viewTags());
        FlowPanel managers = new FlowPanel();
        FlowPanel members = new FlowPanel();
        Iterator i = _detail.members.iterator();
        boolean firstManager = true;
        boolean firstMember = true;
        while (i.hasNext()) {
            final GroupMembership membership = (GroupMembership) i.next();
            final MemberName name = (MemberName) membership.member;
            FlowPanel peoplePanel;
            if (membership.rank == GroupMembership.RANK_MANAGER) {
                if (firstManager) {
                    firstManager = false;
                } else {
                    managers.add(new InlineLabel(", "));
                }
                peoplePanel = managers;
            } else {
                if (firstMember) {
                    firstMember = false;
                } else {
                    members.add(new InlineLabel(", "));
                }
                peoplePanel = members;
            }
            if (amManager) {
                final PopupPanel personMenuPanel = new PopupPanel(true);
                MenuBar menu = getManagerMenuBar(membership, personMenuPanel);
                personMenuPanel.add(menu);
                final InlineLabel person = new InlineLabel(name.toString());
                person.addStyleName("LabelLink");
                // use a MouseListener instead of ClickListener so we can get at the mouse (x,y)
                person.addMouseListener(new MouseListener() {
                    public void onMouseDown (Widget sender, int x, int y) { 
                        personMenuPanel.setPopupPosition(person.getAbsoluteLeft() + x, 
                            person.getAbsoluteTop() + y);
                        personMenuPanel.show();
                    }
                    public void onMouseLeave (Widget sender) { }
                    public void onMouseUp (Widget sender, int x, int y) { }
                    public void onMouseEnter (Widget sender) { }
                    public void onMouseMove (Widget sender, int x, int y) { }
                });
                peoplePanel.add(person);
            } else {
                peoplePanel.add(new Anchor(MsoyEntryPoint.memberViewPath(
                    name.getMemberId()), name.toString()));
            }
        }
        people.setWidget(0, 1, managers);
        people.setWidget(1, 1, members);
        if (_extras.backgroundControl == GroupExtras.BACKGROUND_FIT_TO_IMAGE) {
            VerticalPanel peoplePlusCaps = new VerticalPanel();
            peoplePlusCaps.setSpacing(0);
            peoplePlusCaps.setWidth("100%");
            final Element upperCap = DOM.createElement("div");
            DOM.setStyleAttribute(upperCap, "backgroundImage", "url(" + MsoyEntryPoint.toMediaPath(
                _extras.peopleUpperCap.getMediaPath()) + ")");
            DOM.setStyleAttribute(upperCap, "height", _extras.peopleUpperCapHeight + "px");
            peoplePlusCaps.add(new Widget () {
                {
                    setElement(upperCap);
                }
            });
            peoplePlusCaps.add(people);
            people.setWidth("100%");
            if (_extras.peopleBackground != null) {
                DOM.setStyleAttribute(people.getElement(), "backgroundImage", 
                    "url(" + MsoyEntryPoint.toMediaPath(_extras.peopleBackground.getMediaPath()) + 
                    ")");
            }
            final Element lowerCap = DOM.createElement("div");
            DOM.setStyleAttribute(lowerCap, "backgroundImage", "url(" + MsoyEntryPoint.toMediaPath(
                _extras.peopleLowerCap.getMediaPath()) + ")");
            DOM.setStyleAttribute(lowerCap, "height", _extras.peopleLowerCapHeight + "px");
            peoplePlusCaps.add(new Widget () {
                {
                    setElement(lowerCap);
                }
            });
            _table.setWidget(1, 0, peoplePlusCaps);
        } else if (_extras.backgroundControl == GroupExtras.BACKGROUND_TILED ||
            _extras.backgroundControl == GroupExtras.BACKGROUND_ANCHORED) {
            _table.setWidget(1, 0, people);
            if (_extras.peopleBackground != null) {
                _table.getMyFlexCellFormatter().setBackgroundImage(1, 0, 
                    MsoyEntryPoint.toMediaPath(_extras.peopleBackground.getMediaPath()));
                if (_extras.backgroundControl == GroupExtras.BACKGROUND_ANCHORED) {
                    _table.getMyFlexCellFormatter().setBackgroundNoRepeat(1, 0);
                }
            }
        }
        final Panel tags = !amManager ? (Panel)new FlowPanel() : (Panel)new TagDetailPanel(
            new TagDetailPanel.TagService() {
                public void tag (String tag, AsyncCallback callback) {
                    CGroup.groupsvc.tagGroup(CGroup.creds, _group.groupId, tag, true, callback);
                }
                public void untag (String tag, AsyncCallback callback) {
                    CGroup.groupsvc.tagGroup(CGroup.creds, _group.groupId, tag, false, callback);
                }
                public void getRecentTags (AsyncCallback callback) {
                    CGroup.groupsvc.getRecentTags(CGroup.creds, callback);
                }
                public void getTags (AsyncCallback callback) {
                    CGroup.groupsvc.getTags(CGroup.creds, _group.groupId, callback);
                }
                public boolean supportFlags () {
                    return false;
                }
                public void setFlags (final byte flag, final Label statusLabel) { }
                public List getMenuItems (final String tag) {
                    final MenuItem gotoItem = new MenuItem(CGroup.msgs.viewTagLink(), 
                        (Command)null);
                    gotoItem.setCommand(new Command () {
                            public void execute () {
                                Widget.setVisible(gotoItem.getParentMenu().getParent().
                                    getElement(), false);
                                History.newItem("tag=" + tag);
                            }
                        }
                    );
                    ArrayList menuItems = new ArrayList();
                    menuItems.add(gotoItem);
                    return menuItems;
                }
            });
        if (!amManager) {
            CGroup.groupsvc.getTags(CGroup.creds, _group.groupId, new AsyncCallback () {
                public void onSuccess (Object result) {
                    Iterator i = ((Collection) result).iterator();
                    while (i.hasNext()) {
                        String tag = (String)i.next();
                        Hyperlink tagLink = new Hyperlink(tag, "tag=" + tag);
                        DOM.setStyleAttribute(tagLink.getElement(), "display", "inline");
                        tags.add(tagLink);
                        if (i.hasNext()) {
                            tags.add(new InlineLabel(", "));
                        }
                    }
                } 
                public void onFailure (Throwable caught) {
                    CGroup.log("Failed to fetch tags");
                    addError(CGroup.serverError(caught));
                }
            });
        }
        people.setWidget(3, 1, tags);
        people.setWidget(2, 0, new Widget() {
            {
                Element hr = DOM.createElement("hr");
                DOM.setStyleAttribute(hr, "color", "black");
                DOM.setStyleAttribute(hr, "backgroundColor", "black");
                setElement(hr);
            }
        });
        people.getFlexCellFormatter().setColSpan(2, 0, 2);
        DOM.setAttribute(people.getFlexCellFormatter().getElement(0, 1), "width", "100%");
        _table.getFlexCellFormatter().setColSpan(1, 0, 2);
        _table.getMyFlexCellFormatter().fillWidth(2, 0);
    }

    /**
     * performs a simple scan of the list of GroupMembership objects to find and return the 
     * first GroupMembership that refers to the requested memberId.
     */
    static protected GroupMembership findMember (List members, int memberId) 
    {
        Iterator i = members.iterator();
        GroupMembership member = null;
        while ((member == null || member.member.getMemberId() != memberId) && i.hasNext()) {
            member = (GroupMembership)i.next();
        }
        return (member != null && member.member.getMemberId() == memberId) ? member : null;
    }

    protected String getPolicyName (int policy)
    {
        String policyName;
        switch(policy) {
        case Group.POLICY_PUBLIC: policyName = CGroup.msgs.policyPublic(); break;
        case Group.POLICY_INVITE_ONLY: policyName = CGroup.msgs.policyInvite(); break;
        case Group.POLICY_EXCLUSIVE: policyName = CGroup.msgs.policyExclusive(); break;
        default: policyName = CGroup.msgs.errUnknownPolicy(Integer.toString(policy));
        }
        return policyName;
    }

    /**
     * Get the menus for use by managers when perusing the members of their group.
     */
    protected MenuBar getManagerMenuBar(final GroupMembership membership, final PopupPanel parent) 
    {
        // MenuBar(true) creates a vertical menu
        MenuBar menu = new MenuBar(true);
        menu.addItem("<a href='" + MsoyEntryPoint.memberViewPath(
            membership.member.getMemberId()) + "'>" + CGroup.msgs.viewViewProfile() + "</a>", true, 
            (Command)null);
        MenuItem promote = new MenuItem(CGroup.msgs.viewPromote(), new Command() {
            public void execute() {
                (new PromptPopup(CGroup.msgs.viewPromotePrompt(membership.member.toString())) {
                    public void onAffirmative () {
                        parent.hide();
                        updateMemberRank(membership.member.getMemberId(),
                            GroupMembership.RANK_MANAGER);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });
        MenuItem demote = new MenuItem(CGroup.msgs.viewDemote(), new Command() {
            public void execute() {
                (new PromptPopup(CGroup.msgs.viewPromotePrompt(membership.member.toString())) {
                    public void onAffirmative () {
                        parent.hide();
                        updateMemberRank(membership.member.getMemberId(),
                            GroupMembership.RANK_MEMBER);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });
        MenuItem remove = new MenuItem(CGroup.msgs.viewRemove(), new Command() {
            public void execute() {
                (new PromptPopup(CGroup.msgs.viewRemovePrompt(membership.member.toString(), 
                    _group.name)) { 
                    public void onAffirmative () {
                        parent.hide();
                        removeMember(membership.member.getMemberId(), true);
                    }
                    public void onNegative () { }
                }).prompt();
            }
        });

        // show actions that we don't have permission to take, but make sure they are
        // disabled
        if (!isSenior(_me, membership)) {
            // you can't do jack!
            promote.setCommand(null);
            promote.addStyleName("Disabled");
            demote.setCommand(null);
            demote.addStyleName("Disabled");
            remove.setCommand(null);
            remove.addStyleName("Disabled");
        } else if (membership.rank == GroupMembership.RANK_MANAGER) {
            promote.setCommand(null);
            promote.addStyleName("Disabled");
        } else {
            demote.setCommand(null);
            demote.addStyleName("Disabled");
        }
        menu.addItem(promote);
        menu.addItem(demote);
        menu.addItem(remove);
        return menu;
    }

    public boolean isSenior (GroupMembership member1, GroupMembership member2) 
    {
        if (member1.rank == GroupMembership.RANK_MANAGER && 
            (member2.rank == GroupMembership.RANK_MEMBER || 
            member1.rankAssignedDate < member2.rankAssignedDate)) {
            return true;
        } else {
            return false;
        }
    }

    protected void updateMemberRank (final int memberId, final byte rank) 
    {
        CGroup.groupsvc.updateMemberRank(CGroup.creds, _group.groupId, memberId, rank,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
            }
            public void onFailure (Throwable caught) {
                CGroup.log("Failed to update member rank [groupId=" + _group.groupId +
                           ", memberId=" + memberId + ", newRank=" + rank + "]", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    /**
     * remove the indicated member from this group. 
     *
     * @param memberId The member to remove.
     * @param refresh if <code>true</code>, this page info is refreshed, otherwise the GroupList 
     * page is loaded.
     */
    protected void removeMember (final int memberId, final boolean reload)
    {
        CGroup.groupsvc.leaveGroup(CGroup.creds, _group.groupId, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (reload) {
                    loadGroup(_group.groupId);
                } else { 
                    // will reload the GroupList page
                    History.newItem("list");
                }
            }
            public void onFailure (Throwable caught) {
                CGroup.log("Failed to remove member [groupId=" + _group.groupId +
                           ", memberId=" + memberId + "]", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    protected void joinGroup () 
    {
        CGroup.groupsvc.joinGroup(
            CGroup.creds, _group.groupId, CGroup.getMemberId(), new AsyncCallback() {
            public void onSuccess (Object result) {
                loadGroup(_group.groupId);
            }
            public void onFailure (Throwable caught) {
                CGroup.log("Failed to join group [groupId=" + _group.groupId +
                           ", memberId=" + CGroup.getMemberId() + "]", caught);
                addError(CGroup.serverError(caught));
            }
        });
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected class MyFlexTable extends FlexTable {
        public class MyFlexCellFormatter extends FlexTable.FlexCellFormatter {
            public void setBackgroundImage (int row, int column, String url) {
                DOM.setStyleAttribute(getElement(row, column), "backgroundImage", "url(" + url + 
                    ")");
            }
            public void setBackgroundRepeat (int row, int column, String repeat) {
                DOM.setStyleAttribute(getElement(row, column), "backgroundRepeat", repeat);
            }
            public void setBackgroundNoRepeat (int row, int column) {
                setBackgroundRepeat(row, column, "no-repeat");
            }
            public void fillWidth (int row, int column) {
                DOM.setStyleAttribute(getElement(row, column), "width", "100%");
            }
        }

        public MyFlexTable () {
            setCellFormatter(new MyFlexCellFormatter());
        }

        public MyFlexCellFormatter getMyFlexCellFormatter() {
            return (MyFlexCellFormatter)getCellFormatter();
        }
    }

    protected Group _group;
    protected GroupExtras _extras;
    protected GroupDetail _detail;
    protected GroupMembership _me;

    protected MyFlexTable _table;
    protected VerticalPanel _errorContainer;
}
