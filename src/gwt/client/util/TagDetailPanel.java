//
// $Id$

package client.util;

import client.shell.CShell;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HasVerticalAlignment;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.data.TagHistory;

/**
 * Displays tagging information for a particular item.
 */
public class TagDetailPanel extends FlexTable
{
    /**
     * Interface to the interaction between this panel and the service handling tagging/flagging in
     * the background.
     */
    public interface TagService 
    {
        public void tag (String tag, AsyncCallback callback);
        public void untag (String tag, AsyncCallback callback);
        public void getRecentTags (AsyncCallback callback);
        public void getTags (AsyncCallback callback);
        public boolean supportFlags ();

        /** 
         * In this case, the implementor is responsible for editing the flags on the local object
         * that is being flagged, and is therefore responsible for providing the callback.
         * 
         * @param flag the flag to send to the server and set on the local object on success
         * @param statusLabel A label to set with an error message on failure
         */
        public void setFlags (byte flag, Label statusLabel);

        /**
         * If additional entries are required on the Menu that pops up when a tag is clicked, this
         * method can add menu items for that purpose.
         */
        public void addMenuItems (String tag, PopupMenu menu);
    }

    public TagDetailPanel (TagService service)
    {
        setStyleName("tagDetailPanel");
        _service = service;

        int col = 0;
        _tags = new FlowPanel();
        _tags.add(new Label(CShell.cmsgs.tagLoading()));
        setWidget(0, col, _tags);
        getFlexCellFormatter().setStyleName(0, col++, "Tags");

        if (CShell.getMemberId() > 0) {
            setWidget(0, col++, new Label(CShell.cmsgs.tagAddTag()));
            getFlexCellFormatter().setVerticalAlignment(1, 0, HasVerticalAlignment.ALIGN_MIDDLE);
            setWidget(0, col++, new NewTagBox());

//             _quickTagLabel = new Label(CShell.cmsgs.tagQuickAdd());
//             setWidget(0, col++, _quickTagLabel);
//             getFlexCellFormatter().setVerticalAlignment(1, 2, HasVerticalAlignment.ALIGN_MIDDLE);
//             _quickTags = new ListBox();
//             _quickTags.addChangeListener(new ChangeListener() {
//                 public void onChange (Widget sender) {
//                     ListBox box = (ListBox) sender;
//                     String value = box.getValue(box.getSelectedIndex());
//                     _service.tag(value, new AsyncCallback() {
//                         public void onSuccess (Object result) {
//                             refreshTags();
//                         }
//                         public void onFailure (Throwable caught) {
//                             GWT.log("tagItem failed", caught);
//                             _status.setText(CShell.serverError(caught));
//                         }
//                     });
//                 }
//             });
//             setWidget(0, col++, _quickTags);

            if (_service.supportFlags()) {
                InlineLabel flagLabel = new InlineLabel(CShell.cmsgs.tagFlag());
                PopupMenu menu = new PopupMenu(flagLabel) {
                    protected void addMenuItems () {
                        addMenuItem(CShell.cmsgs.tagMatureFlag(), new Command() {
                            public void execute () {
                                maybeUpdateFlag(CShell.cmsgs.tagMatureFlag(),
                                                Item.FLAG_FLAGGED_MATURE);
                            }
                        });
                        addMenuItem(CShell.cmsgs.tagCopyrightFlag(), new Command() {
                            public void execute () {
                                maybeUpdateFlag(CShell.cmsgs.tagCopyrightFlag(),
                                                Item.FLAG_FLAGGED_COPYRIGHT);
                            }
                        });
                    }
                };
                setWidget(0, col++, flagLabel);
            }
        }

        setWidget(1, 0, _status = new Label(""));
        getFlexCellFormatter().setColSpan(1, 0, getCellCount(0));

        refreshTags();
    }

    protected void maybeUpdateFlag (String menuLabel, final byte flag)
    {
        new PromptPopup(CShell.cmsgs.tagFlagPrompt(menuLabel), CShell.cmsgs.tagFlagFlagButton(),
                        CShell.cmsgs.tagFlagCancelButton()) {
            public void onAffirmative () {
                _service.setFlags(flag, _status);
            }
            public void onNegative () {
            }
        }.prompt();
    }
    
    protected void toggleTagHistory ()
    {
        // TODO: if this is used again, it will need to be abstracted like everything else in this
        // class
//         if (_tagHistory != null) {
//             if (_content.getWidgetDirection(_tagHistory) == null) {
//                 _content.add(_tagHistory, DockPanel.EAST);
//             } else {
//                 _content.remove(_tagHistory);
//             }
//             return;
//         }

//         CItem.itemsvc.getTagHistory(CItem.creds, _itemId, new AsyncCallback() {
//             public void onSuccess (Object result) {
//                 _tagHistory = new FlexTable();
//                 _tagHistory.setBorderWidth(0);
//                 _tagHistory.setCellSpacing(0);
//                 _tagHistory.setCellPadding(2);

//                 int tRow = 0;
//                 Iterator iterator = ((Collection) result).iterator();
//                 while (iterator.hasNext()) {
//                     TagHistory history = (TagHistory) iterator.next();
//                     String date = history.time.toGMTString();
//                     // Fri Sep 29 2006 12:46:12
//                     date = date.substring(0, 23);
//                     _tagHistory.setText(tRow, 0, date);
//                     _tagHistory.setText(tRow, 1, history.member.toString());
//                     String actionString;
//                     switch(history.action) {
//                     case TagHistory.ACTION_ADDED:
//                         actionString = "added";
//                         break;
//                     case TagHistory.ACTION_COPIED:
//                         actionString = "copied";
//                         break;
//                     case TagHistory.ACTION_REMOVED:
//                         actionString = "removed";
//                         break;
//                     default:
//                         actionString = "???";
//                         break;
//                     }
//                     _tagHistory.setText(tRow, 2, actionString);
//                     _tagHistory.setText(
//                         tRow, 3, history.tag == null ? "N/A" : "'" + history.tag + "'");
//                     tRow ++;
//                 }
//                 _content.add(_tagHistory, DockPanel.EAST);
//             }

//             public void onFailure (Throwable caught) {
//                 GWT.log("getTagHistory failed", caught);
//                 _status.setText("Internal error fetching item tag history: " + caught.getMessage());
//             }
//         });
    }

    protected void refreshTags ()
    {
        _service.getTags(new AsyncCallback() {
            public void onSuccess (Object result) {
                gotTags((Collection)result);
            }
            public void onFailure (Throwable caught) {
                _tags.clear();
                _tags.add(new Label(CShell.serverError(caught)));
            }
        });
    }

    protected void gotTags (Collection tags)
    {
        _tags.clear();
        final ArrayList addedTags = new ArrayList();
        for (Iterator iter = tags.iterator(); iter.hasNext() ; ) {
            final String tag = (String) iter.next();
            InlineLabel tagLabel = new InlineLabel(tag);
            if (CShell.getMemberId() > 0) {
                final Command remove = new Command() {
                    public void execute () {
                        new PromptPopup(CShell.cmsgs.tagRemoveConfirm(tag)) {
                            public void onAffirmative () {
                                _service.untag(tag, new AsyncCallback() {
                                    public void onSuccess (Object result) {
                                        refreshTags();
                                    }
                                    public void onFailure (Throwable caught) {
                                        GWT.log("tagItem failed", caught);
                                        _status.setText(CShell.serverError(caught));
                                    }
                                });
                            }
                            public void onNegative () { 
                            }
                        }.prompt();
                    }
                };
                new PopupMenu(tagLabel) {
                    protected void addMenuItems () {
                        addMenuItem(CShell.cmsgs.tagRemove(), remove);
                        _service.addMenuItems(tag, this);
                    }
                };
            }
            _tags.add(tagLabel);
            addedTags.add(tag);
            if (iter.hasNext()) {
                _tags.add(new InlineLabel(", "));
            }
        }

//         if (CShell.creds != null) {
//             _service.getRecentTags(new AsyncCallback() {
//                 public void onSuccess (Object result) {
//                     _quickTags.clear();
//                     _quickTags.addItem(CShell.cmsgs.tagSelectOne());
//                     Iterator i = ((Collection) result).iterator();
//                     while (i.hasNext()) {
//                         TagHistory history = (TagHistory) i.next();
//                         String tag = history.tag;
//                         if (tag != null && !addedTags.contains(tag) && 
//                             history.member.getMemberId() == CShell.getMemberId()) {
//                             _quickTags.addItem(tag);
//                             addedTags.add(tag);
//                         }
//                     }
//                     boolean visible = _quickTags.getItemCount() > 1;
//                     _quickTags.setVisible(visible);
//                     _quickTagLabel.setVisible(visible);
//                 }
//                 public void onFailure (Throwable caught) {
//                     GWT.log("getTagHistory failed", caught);
//                     _status.setText(CShell.serverError(caught));
//                 }
//             });
//         }
    }

    protected class NewTagBox extends TextBox
        implements ClickListener
    {
        public NewTagBox () {
            setMaxLength(20);
            setVisibleLength(12);
            addKeyboardListener(new EnterClickAdapter(this));
        }

        public void onClick (Widget sender) {
            String tagName = getText().trim().toLowerCase();
            if (tagName.length() == 0) {
                return;
            }
            if (tagName.length() > 24) {
                _status.setText(CShell.cmsgs.errTagTooLong());
                return;
            }
            for (int ii = 0; ii < tagName.length(); ii ++) {
                char c = tagName.charAt(ii);
                if (Character.isLetter(c) || Character.isDigit(c) || c == '_') {
                    continue;
                }
                _status.setText(CShell.cmsgs.errTagInvalidCharacters());
                return;
            }
            _service.tag(tagName, new AsyncCallback() {
                public void onSuccess (Object result) {
                    refreshTags();
                }
                public void onFailure (Throwable caught) {
                    GWT.log("tagItem failed", caught);
                    _status.setText(CShell.serverError(caught));
                }
            });
            setText(null);
        }
    }

    protected TagService _service;

    protected Label _status;
    protected FlowPanel _tags;
    protected ListBox _quickTags;
    protected Label _quickTagLabel;
    protected FlexTable _tagHistory;
}
