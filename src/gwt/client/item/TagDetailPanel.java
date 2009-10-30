//
// $Id$

package client.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedWidget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.TagCodes;

import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;

import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.TagHistory;

import client.shell.CShell;
import client.ui.BorderedDialog;
import client.ui.BorderedPopup;
import client.ui.ComplainPopup;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.ui.PromptPopup;
import client.ui.RowPanel;
import client.util.InfoCallback;
import client.util.Link;
import client.shell.ShellMessages;

/**
 * Displays tagging information for a particular item.
 */
public class TagDetailPanel extends VerticalPanel
{
    /**
     * Interface to the interaction between this panel and the service handling tagging in
     * the background.
     */
    public interface TagService
    {
        void tag (String tag, AsyncCallback<TagHistory> callback);
        void untag (String tag, AsyncCallback<TagHistory> callback);
        void getTagHistory (AsyncCallback<List<TagHistory>> callback);
        void getTags (AsyncCallback<List<String>> callback);

        /**
         * If additional entries are required on the Menu that pops up when a tag is clicked, this
         * method can add menu items for that purpose.
         */
        void addMenuItems (String tag, PopupMenu menu, boolean canEdit);
    }

    /**
     * Interface to the interaction between this panel and the service handling flagging in
     * the background.
     */
    public interface FlagService
    {
        void addFlag (ItemFlag.Kind kind, String comment);
    }

    /**
     * Interface to the optional service supporting tag complaints.
     */
    public interface ComplainService
    {
        void complain (String tag, String reason, AsyncCallback<Void> callback);
    }

    public TagDetailPanel (TagService service, FlagService flagger, ComplainService complainer,
        List<String> tags, boolean showAddUI)
    {
        setStyleName("tagDetailPanel");
        _service = service;
        _flagger = flagger;
        _complainer = complainer;
        _canEdit = !CShell.isGuest() && showAddUI;

        _tags = new FlowPanel();
        _tags.setStyleName("Tags");
        _tags.add(new Label(_cmsgs.tagLoading()));
        add(_tags);

        if (_canEdit || _flagger != null || CShell.isSupport()) {
            RowPanel addRow = new RowPanel();
            if (_canEdit) {
                addRow.add(new InlineLabel(_cmsgs.tagAddTag(), false, false, false),
                           HasAlignment.ALIGN_MIDDLE);
                addRow.add(new NewTagBox());
            }

            if (_flagger != null) {
                InlineLabel flagLabel = new InlineLabel(_cmsgs.tagFlag());
                new PopupMenu(flagLabel) {
                    protected void addMenuItems () {
                        addCommand(_cmsgs.tagMatureFlag(), ItemFlag.Kind.MATURE);
                        addCommand(_cmsgs.tagCopyrightFlag(), ItemFlag.Kind.COPYRIGHT);
                        addCommand(_cmsgs.tagStolenFlag(), ItemFlag.Kind.STOLEN);
                        addCommand(_cmsgs.tagUnattributedFlag(), ItemFlag.Kind.UNATTRIBUTED);
                        addCommand(_cmsgs.tagScamFlag(), ItemFlag.Kind.SCAM);
                        addCommand(_cmsgs.tagBrokenFlag(), ItemFlag.Kind.BROKEN);
                    }
                    protected void addCommand (String label, ItemFlag.Kind kind) {
                        addMenuItem(label, new FlagCommand(kind));
                    }
                };
                addRow.add(flagLabel, HasAlignment.ALIGN_MIDDLE);
            }

            if (CShell.isSupport()) {
                InlineLabel historyLabel = new InlineLabel(_cmsgs.tagHistory());
                historyLabel.addStyleName("LabelLink");
                historyLabel.addClickHandler(new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        toggleTagHistory();
                    }
                });
                addRow.add(historyLabel, HasAlignment.ALIGN_MIDDLE);
            }

            add(WidgetUtil.makeShim(5, 5));
            add(addRow);
        }

        if (tags == null) {
            _service.getTags(new AsyncCallback<List<String>>() {
                public void onSuccess (List<String> tags) {
                    gotTags(tags);
                }
                public void onFailure (Throwable caught) {
                    _tags.clear();
                    _tags.add(new Label(CShell.serverError(caught)));
                }
            });
        } else {
            gotTags(tags);
        }
    }

    protected void toggleTagHistory ()
    {
         if (_tagHistory != null) {
             if (_tagHistory.isShowing()) {
                 _tagHistory.hide();
             } else {
                 _tagHistory.show();
             }
             return;
         }

         final PagedWidget<TagHistory> pager = new PagedWidget<TagHistory>(12) {
             @Override public void displayPage (final int page, boolean forceRefresh) {
                 super.displayPage(page, forceRefresh);
                 if (page == 0) {
                     _tagHistory.center();
                 }
             }

            protected Widget createContents (int start, int count, List<TagHistory> list) {
                FlexTable contents = new FlexTable();
                contents.setStyleName("tagHistory");
                contents.getColumnFormatter().setWidth(0, "150px");
                contents.getColumnFormatter().setWidth(1, "180px");
                contents.getColumnFormatter().setWidth(2, "20px");
                contents.getColumnFormatter().setWidth(3, "150px");
                contents.setBorderWidth(1);
                contents.setCellSpacing(0);
                contents.setCellPadding(5);
                FlexCellFormatter formatter = contents.getFlexCellFormatter();

                int tRow = 0;
                for (TagHistory history : list) {
                    String fullDate = history.time.toString();
                    // Fri Sep 2006 12:46:12 GMT 2006
                    String date = fullDate.substring(4, 20) + fullDate.substring(26);
                    contents.setText(tRow, 0, date);
                    formatter.setHorizontalAlignment(tRow, 0, HasAlignment.ALIGN_LEFT);

                    String memName = history.member.toString();
                    if (memName.length() > MAX_NAME_LENGTH-3) {
                        memName = memName.substring(0, MAX_NAME_LENGTH) + "...";
                    }
                    contents.setWidget(tRow, 1, Link.create(
                        memName, Pages.PEOPLE, history.member.getMemberId()));
                    formatter.setHorizontalAlignment(tRow, 1, HasAlignment.ALIGN_LEFT);

                    String actionString;
                    switch(history.action) {
                    case TagHistory.ACTION_ADDED:
                        actionString = "+";
                        break;
                    case TagHistory.ACTION_COPIED:
                        actionString = "C";
                        break;
                    case TagHistory.ACTION_REMOVED:
                        actionString = "-";
                        break;
                    default:
                        actionString = "?";
                        break;
                    }
                    contents.setText(tRow, 2, actionString);
                    contents.setText(
                        tRow, 3, history.tag == null ? "N/A" : "'" + history.tag + "'");
                    formatter.setHorizontalAlignment(tRow, 3, HasAlignment.ALIGN_LEFT);
                    tRow ++;
                }
                return contents;
            }

            protected String getEmptyMessage () {
                return "No known tag history.";
            }
         };

         // while it's not, let this pager pretend to be a pagedGrid because our CSS is
         // so oddly organized
         pager.setStyleName("pagedGrid");


         _tagHistory = new BorderedPopup(true);
         _tagHistory.setWidget(pager);

         // kick off the request to load the history
         _service.getTagHistory(new InfoCallback<List<TagHistory>>() {
             public void onSuccess (List<TagHistory> result) {
                 pager.setModel(new SimpleDataModel<TagHistory>(result), 0);
             }
         });

         // we don't actually show ourselves until we receive the data (bad UI)
    }

    protected void refreshTags ()
    {
    }

    protected void gotTags (List<String> tags)
    {
        _tlist = tags;
        _tags.clear();
        _tags.add(new InlineLabel("Tags:", false, false, true));

        final List<String> addedTags = new ArrayList<String>();
        for (Iterator<String> iter = tags.iterator(); iter.hasNext() ; ) {
            final String tag = iter.next();
            InlineLabel tagLabel = new InlineLabel(tag);

            final Command remove;
            if (_canEdit) {
                remove = new Command() {
                    public void execute () {
                        _service.untag(tag, new InfoCallback<TagHistory>() {
                            public void onSuccess (TagHistory result) {
                                _tlist.remove(tag);
                                gotTags(_tlist);
                            }
                        });
                    }
                };
            } else {
                remove = null;
            }
            new PopupMenu(tagLabel) {
                protected void addMenuItems () {
                    _service.addMenuItems(tag, this, _canEdit);
                    if (remove != null) {
                        addMenuItem(_cmsgs.tagRemove(),
                            new PromptPopup(_cmsgs.tagRemoveConfirm(tag), remove));
                    }
                    if (CShell.isRegistered() && _complainer != null) {
                        addMenuItem(_cmsgs.tagComplain(),
                            new Command() {
                                @Override public void execute () {
                                    new ComplainTagPopup(tag).show();
                                }
                            });
                    }
                }
            };
            _tags.add(tagLabel);
            addedTags.add(tag);
            if (iter.hasNext()) {
                _tags.add(new InlineLabel(", "));
            }
        }

        if (addedTags.size() == 0) {
            _tags.add(new InlineLabel("none"));
        }
    }

    protected class NewTagBox extends TextBox
        implements ClickHandler
    {
        public NewTagBox () {
            setMaxLength(20);
            setVisibleLength(12);
            addKeyPressHandler(new EnterClickAdapter(this));
        }

        public void onClick (ClickEvent event) {
            final String tagName = getText().trim().toLowerCase();
            if (tagName.length() == 0) {
                return;
            }
            if (tagName.length() < TagCodes.MIN_TAG_LENGTH) {
                MsoyUI.error(_cmsgs.errTagTooShort("" + TagCodes.MIN_TAG_LENGTH));
                return;
            }
            if (tagName.length() > TagCodes.MAX_TAG_LENGTH) {
                MsoyUI.error(_cmsgs.errTagTooLong("" + TagCodes.MAX_TAG_LENGTH));
                return;
            }
            for (int ii = 0; ii < tagName.length(); ii ++) {
                char c = tagName.charAt(ii);
                if (c == '_' || Character.isLetterOrDigit(c)) {
                    continue;
                }
                MsoyUI.error(_cmsgs.errTagInvalidCharacters());
                return;
            }
            _service.tag(tagName, new InfoCallback<TagHistory>() {
                public void onSuccess (TagHistory result) {
                    _tlist.add(tagName);
                    gotTags(_tlist);
                }
            });
            setText(null);
        }
    }

    protected class FlagCommand
        implements Command
    {
        public FlagCommand (ItemFlag.Kind kind)
        {
            _kind = kind;
        }

        public void execute ()
        {
            final BorderedDialog dialog = new BorderedDialog(false) {};
            dialog.setHeaderTitle(_cmsgs.tagFlagDialogTitle());

            String[] prompts;
            boolean showLinkBox = false;
            switch (_kind) {
            default:
            case MATURE:
                prompts = new String[] { _cmsgs.tagMaturePrompt1(), _cmsgs.tagMaturePrompt2() };
                break;

            case COPYRIGHT:
                prompts = new String[] {
                    _cmsgs.tagCopyrightPrompt1(), _cmsgs.tagCopyrightPrompt2() };
                showLinkBox = true;
                break;

            case STOLEN:
                prompts = new String[] { _cmsgs.tagStolenPrompt1(), _cmsgs.tagStolenPrompt2() };
                showLinkBox = true;
                break;

            case UNATTRIBUTED:
                prompts = new String[] {
                    _cmsgs.tagUnattributedPrompt1(), _cmsgs.tagUnattributedPrompt2() };
                showLinkBox = true;
                break;

            case SCAM:
                prompts = new String[] { _cmsgs.tagScamPrompt1(), _cmsgs.tagScamPrompt2() };
                break;

            case BROKEN:
                prompts = new String[] { _cmsgs.tagBrokenPrompt1(), _cmsgs.tagBrokenPrompt2() };
                break;
            }

            final SmartTable content = new SmartTable(0, 10);
            content.setWidth("300px");
            int row = 0;
            content.setText(row++, 0, prompts[0], 2);
            content.setText(row++, 0, prompts[1], 2);
            if (showLinkBox) {
                content.setText(row, 0, _cmsgs.tagFlagDialogLinkLabel());
                content.setWidget(row++, 1, _link = MsoyUI.createTextBox("", 255, 48));
            }
            content.setWidget(row++, 0, _comment = MsoyUI.createTextArea(null, 64, 4), 2);
            dialog.setContents(content);
            dialog.addButton(new Button(_cmsgs.cancel(), dialog.onCancel()));
            dialog.addButton(new Button(_cmsgs.tagFlagDialogAccept(), new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    if (_link != null && _link.getText().trim().equals("")) {
                        MsoyUI.error(_cmsgs.errFlagLinkRequired());
                        return;
                    }
                    String comment = _comment.getText();
                    if (_link != null) {
                        comment = "Link: " + _link.getText() + " " + comment;
                    }
                    _flagger.addFlag(_kind, comment);
                    MsoyUI.info(_cmsgs.tagThanks());
                    dialog.hide();
                }
            }));
            dialog.show();
        }

        protected ItemFlag.Kind _kind;
        protected TextArea _comment;
        protected TextBox _link;
    }

    protected class ComplainTagPopup extends ComplainPopup
    {
        public ComplainTagPopup (String tag)
        {
            super(MAX_COMPLAINT_LENGTH);
            _tag = tag;
        }

        @Override
        protected boolean callService ()
        {
            _complainer.complain(_tag, _description.getText(), this);
            return true;
        }

        protected String _tag;
    }

    protected TagService _service;
    protected FlagService _flagger;
    protected ComplainService _complainer;
    protected boolean _canEdit;

    protected List<String> _tlist;
    protected FlowPanel _tags;
    protected ListBox _quickTags;
    protected Label _quickTagLabel;
    protected BorderedPopup _tagHistory;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ItemServiceAsync _itemsvc = GWT.create(ItemService.class);

    protected static final int MAX_NAME_LENGTH = 22;

    /** Maximum length allowed for a comment complaint. Note: this must be the same as the maximum
     * length of {@link com.threerings.underwire.server.persist.EventRecord#subject}, but we cannot
     * easily share code here. */
    public static final int MAX_COMPLAINT_LENGTH = 255;
}
