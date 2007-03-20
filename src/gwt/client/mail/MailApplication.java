//
// $Id$

package client.mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailHeaders;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MemberName;

import client.msgs.MailComposition;
import client.msgs.MailPayloadDisplay;
import client.msgs.MailUpdateListener;
import client.shell.Application;
import client.util.BorderedWidget;

/**
 * A mail reading application, with a sidebar listing available folders, the upper
 * half of the main panel listing the message headers in the currently selected folder,
 * and the lower half displaying individual messages.
 */
public class MailApplication extends DockPanel
    implements PopupListener, MailUpdateListener
{
    /** The number of messages we display on-screen at a time. */
    public static final int HEADER_ROWS = 8;
    /** The number of page numbers to display in the pager before we shortcut with ... */
    private static final int PAGES_TO_SHOW = 3;
    
    /**
     * Initialize ths application and build the UI framework.
     */
    public MailApplication ()
    {
        super();
        buildUI();
    }
    
    /**
     * When a composition popup closes, we need to refresh the folder/header displays.
     */ 
    public void onPopupClosed (PopupPanel sender, boolean autoClosed)
    {
        refresh();
    }

    /**
     * Called from the MailPayloadDisplay when the state of the payload has changed
     */
    public void messageChanged (int ownerId, int folderId, int messageId)
    {
        if (folderId == _currentFolder && messageId == _currentMessage) {
            loadMessage(true);
        }
    }

    /**
     * Called to instruct this application to update its view.
     * 
     * The messageId parameter may be -1, if no message body is to be displayed.
     */
    public void show (int folderId, int headerOffset, int messageId)
    {
        clearErrors();
        _currentFolder = folderId;
        _currentOffset = headerOffset;
        _currentMessage = messageId;
        updateHistory();
        if (messageId >= 0) {
            // loadMessage will call loadFolders() and loadHeaders()
            loadMessage(true);
        } else {
            loadHeaders();
            _messageContainer.setVisible(false);
        }
    }

    /**
     * Update the panel/header panels with new data from the backend.
     */
    public void refresh ()
    {
        clearErrors();
        // always clear the current message display on a refresh
        _currentMessage = -1;
        _messageContainer.setVisible(false);
        // always fetch the folders
        loadFolders();
        if (_currentFolder >= 0) {
            // and if we have a selected folder, fetch that too
            loadHeaders();
        } else {
            _headerContainer.clear();
        }
    }

    // the UI is a horizontal top row and underneath it, the message display
    protected void buildUI ()
    {
        setStyleName("App");
        setSpacing(0);

        add(buildTopContent(), DockPanel.NORTH);
        Widget messagePanel = buildMessagePanel();
        add(messagePanel, DockPanel.CENTER);
        setCellWidth(messagePanel, "100%");

        // and below it all, we display any errors
        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("groupDetailErrors");
        add(_errorContainer, DockPanel.SOUTH);
        
        loadFolders();
    }

    // the top row has a folder sidebar to the left and message headers to the right
    protected Widget buildTopContent ()
    {
        HorizontalPanel topContent = new HorizontalPanel();
        topContent.setStyleName("Top");

        Widget sidebarHolder = buildFolderPanel();
        sidebarHolder.setWidth("100%");
        topContent.add(sidebarHolder);
        topContent.setCellWidth(sidebarHolder, "120px");
        topContent.setCellHeight(sidebarHolder, "100%");

        BorderedWidget headerHolder = buildHeaderPanel();
        topContent.add(headerHolder);
        topContent.setCellHeight(headerHolder, "100%");

        return topContent;
    }

    // the folderpanel side bar is just a pretty top bar and a list of folders
    // TODO: display folders in sane human order
    protected BorderedWidget buildFolderPanel ()
    {
        // construct the side bar
        VerticalPanel sideBar = new VerticalPanel();
        sideBar.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        sideBar.setStyleName("FolderPanel");
        sideBar.setSpacing(0);

        // construct the side bar header
        HorizontalPanel sidebarHeader = new HorizontalPanel();
        sidebarHeader.setStyleName("Header");
        sidebarHeader.setSpacing(5);
        Label icon = new Label();
        icon.setStyleName("Left");
        sidebarHeader.add(icon);
        Label mail = new Label(CMail.msgs.appMail());
        mail.setStyleName("Right");
        sidebarHeader.add(mail);
        sideBar.add(sidebarHeader);

        // and add a list of folders
        _folderContainer = new SimplePanel();
        _folderContainer.setStyleName("Folders");
        sideBar.add(_folderContainer);
        sideBar.setCellHeight(_folderContainer, "100%");

        BorderedWidget sidebarHolder =
            new BorderedWidget(BorderedWidget.BORDER_CLOSED, BorderedWidget.BORDER_TILED,
                               BorderedWidget.BORDER_CLOSED, BorderedWidget.BORDER_CLOSED);
        sidebarHolder.setVerticalAlignment(HasAlignment.ALIGN_TOP);
        sidebarHolder.getCellFormatter().setHeight(1, 1, "100%");
        sidebarHolder.setHeight("100%");
        sidebarHolder.setWidget(sideBar);
        return sidebarHolder;
    }

    // the top right side is a list of message headers
    protected BorderedWidget buildHeaderPanel ()
    {
        VerticalPanel headerPanel = new VerticalPanel();
        headerPanel.setStyleName("HeaderPanel");
        // construct the header panel's controls
        FlowPanel headerBar = new FlowPanel();
        headerBar.setStyleName("Bar");

        HorizontalPanel headerControls = new HorizontalPanel();
        headerControls.setStyleName("Controls");
        Button searchButton = new Button(CMail.msgs.appBtnSearch());
        searchButton.setEnabled(false);
        headerControls.add(searchButton);

        Button toggleButton = new Button(CMail.msgs.appBtnToggle());
        toggleButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                Iterator i = _checkboxes.iterator();
                while (i.hasNext()) {
                    MailCheckBox box = (MailCheckBox) i.next();
                    if (box.isChecked()) {
                        box.setChecked(false);
                        _checkedMessages.remove(box.headers);
                    } else {
                        box.setChecked(true);
                        _checkedMessages.add(box.headers);
                    }
                }
                _massDelete.setEnabled(_checkedMessages.size() > 0);
            }
        });
        headerControls.add(toggleButton);

        _massDelete = new Button(CMail.msgs.appBtnDeleteSel());
        _massDelete.setEnabled(false);
        _massDelete.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                deleteMessages(_checkedMessages.toArray());
                _massDelete.setEnabled(false);
            }
        });
        headerControls.add(_massDelete);
        
        headerBar.add(headerControls);

        _headerPager = new HorizontalPanel();
        _headerPager.setStyleName("Pager");
        _headerPager.setVisible(false);
        _pagerPages = new FlowPanel();
        _headerPager.add(_pagerPages);
        _pagerPrevious = new Button(CMail.msgs.appBtnPrevious());
        _pagerPrevious.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                _currentOffset -= HEADER_ROWS;
                refreshHeaderPanel();
                updateHistory();
            }
        });
        _headerPager.add(_pagerPrevious);

        _pagerNext = new Button(CMail.msgs.appBtnNext());
        _pagerNext.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                _currentOffset += HEADER_ROWS;
                refreshHeaderPanel();
                updateHistory();
            }
        });
        _headerPager.add(_pagerNext);;
        headerBar.add(_headerPager);

        headerPanel.add(headerBar);
        
        // construct the header panel's actual header container
        _headerContainer = new SimplePanel();
        headerPanel.add(_headerContainer);
        headerPanel.setCellHeight(_headerContainer, "100%");
        headerPanel.setCellWidth(_headerContainer, "100%");
        BorderedWidget headerHolder =
            new BorderedWidget(BorderedWidget.BORDER_TILED, BorderedWidget.BORDER_CLOSED,
                               BorderedWidget.BORDER_CLOSED, BorderedWidget.BORDER_CLOSED);
        headerHolder.setWidth("100%");
        headerHolder.setHeight("100%");
        headerHolder.setWidget(headerPanel);
        return headerHolder;
    }

    // the main message is displayed at the bottom, with a pretty control bar at the top
    protected Widget buildMessagePanel ()
    {
        // the bottom right side shows an individual message
        _messageContainer = new VerticalPanel();
        _messageContainer.setStyleName("MessagePanel");
        FlowPanel messageBar = new FlowPanel();
        messageBar.setStyleName("Bar");
        Label star = new Label();
        star.setStyleName("Star");
        messageBar.add(star);

        HorizontalPanel messageControls = new HorizontalPanel();
        messageControls.setStyleName("Controls");

        // reply functionality, which kicks off the composer
        Button replyButton = new Button(CMail.msgs.appBtnReply());
        replyButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                String subject = _message.headers.subject;
                if (subject.length() < 3 || !subject.substring(0, 3).equalsIgnoreCase("re:")) {
                    subject = "re: " + subject;
                }
                MailComposition composition =
                    new MailComposition(_message.headers.sender, subject, null, "");
                composition.addPopupListener(MailApplication.this);
                composition.show();
            }
        });
        messageControls.add(replyButton);

        // a button to reply to all recipients of a message, currently disabled
//        Button replyAllButton = new Button(CMail.msgs.appBtnReplyAll());
//        replyAllButton.setEnabled(false);
//        messageControls.add(replyAllButton);

        // a button to forward a message, currently disabled
//        Button forwardButton = new Button(CMail.msgs.appBtnForward());
//        forwardButton.setEnabled(false);
//        messageControls.add(forwardButton);

        // a button to delete a single message
        final Button deleteButton = new Button(CMail.msgs.appBtnDelete());
        deleteButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                deleteButton.setEnabled(false);
                deleteMessages(new MailHeaders[] { _message.headers });
            }
        });
        messageControls.add(deleteButton);
        messageBar.add(messageControls);
        _messageContainer.add(messageBar);
        _messageHolder = new SimplePanel();
        _messageContainer.add(_messageHolder);
        _messageContainer.setCellWidth(_messageHolder, "100%");
        return _messageContainer;
    }


    // fetch the folder list from the backend and trigger a display
    protected void loadFolders ()
    {
        CMail.mailsvc.getFolders(CMail.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                _folders = (List) result;
                refreshFolderPanel();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to fetch mail folders from database: " + caught.getMessage());
            }
        });
    }

    // construct the list of folders, including unread count
    protected void refreshFolderPanel ()
    {
        VerticalPanel folderList = new VerticalPanel();
        folderList.setVerticalAlignment(HasAlignment.ALIGN_TOP);

        List folders = new ArrayList();
        MailFolder inbox = null, sent = null, trash = null;

        Iterator i = _folders.iterator();
        while (i.hasNext()) {
            MailFolder folder = (MailFolder) i.next();

            if (folder.folderId == MailFolder.INBOX_FOLDER_ID) {
                inbox = folder;
            } else if (folder.folderId == MailFolder.SENT_FOLDER_ID) {
                sent = folder;
            } else if (folder.folderId == MailFolder.TRASH_FOLDER_ID) {
                trash = folder;
            } else {
                folders.add(folder);
            }
        }
        // inbox and sent go at the head of the list
        if (sent != null) {
            folders.add(0, sent);
        }
        if (inbox != null) {
            folders.add(0, inbox);
        }
        // trash goes at the end
        if (trash != null) {
            folders.add(trash);
        }
        i = folders.iterator();
        while (i.hasNext()) {
            MailFolder folder = (MailFolder) i.next();
            String name = folder.name;
            if (folder.unreadCount > 0) {
                name += " (" + folder.unreadCount + ")";
            }
            Hyperlink link = Application.createLink(name, "mail", "f" + folder.folderId);
            link.setStyleName("Folder");
            if (folder.unreadCount > 0) {
                link.addStyleName("Folder-unread");
            }
            if (folder.folderId == MailFolder.TRASH_FOLDER_ID) {
                link.addStyleName("Folder-trash");
            }
            folderList.add(link);
        }
        _folderContainer.setWidget(folderList);
    }

    // fetch the message headers for the currently selected folder, trigger a redisplay
    protected void loadHeaders ()
    {
        if (_currentFolder < 0) {
            addError("Internal error: asked to load headers, but no folder selected.");
            return;
        }
        CMail.mailsvc.getHeaders(CMail.creds, _currentFolder, new AsyncCallback() {
            public void onSuccess (Object result) {
                _headers = (List) result;
                refreshHeaderPanel();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to fetch mail headers from database: " + caught.getMessage());
            }
            
        });
    }
    

    // construct the list of message headers
    protected void refreshHeaderPanel ()
    {
        // initialize our collection of currently checked messages
        _checkedMessages.clear();
        // and keep track of the currently displayed checkboxes too
        _checkboxes.clear();

        if (_headers.size() == 0) {
            _headerContainer.setWidget(new HTML("&nbsp;"));
            _headerPager.setVisible(false);
            return;
        }

        // build the actual headers
        FlexTable rows = new FlexTable();
        rows.setCellPadding(0);
        rows.setCellSpacing(0);
        rows.setStyleName("Rows");
        rows.setWidth("100%");

        // now build row after row of data
        int lastMsg = Math.min(_headers.size(), _currentOffset + HEADER_ROWS);
        for (int msg = _currentOffset; msg < lastMsg; msg ++) {
            MailHeaders headers = (MailHeaders) _headers.get(msg);
            int rowCnt = rows.getRowCount();

            rows.getRowFormatter().setStyleName(rowCnt, "Row");
            if (headers.unread) {
                rows.getRowFormatter().addStyleName(rowCnt, "Row-unread");
            }
            if (_currentMessage == headers.messageId) {
                rows.getRowFormatter().addStyleName(rowCnt, "Row-selected");
            }

            // first, a checkbox with a listener that maintains a set of checked messages
            MailCheckBox cBox = new MailCheckBox(headers);
            cBox.setStyleName("CheckBox");
            cBox.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    if (sender instanceof MailCheckBox) {
                        MailCheckBox box = (MailCheckBox) sender;
                        if (box.isChecked()) {
                            _checkedMessages.add(box.headers);
                            _massDelete.setEnabled(true);
                        } else {
                            _checkedMessages.remove(box.headers);
                            _massDelete.setEnabled(_checkedMessages.size() > 0);
                        }
                    }
                }
            });
            rows.setWidget(rowCnt, 0, cBox);
            rows.getFlexCellFormatter().setWidth(rowCnt, 0, "20px");
            _checkboxes.add(cBox);

            // next, the subject line, the only variable-width element in the row
            Widget link = Application.createLink(
                headers.subject, "mail",
                "f" + _currentFolder + "." + _currentOffset + "." + headers.messageId);
            link.setStyleName("Subject");
            rows.setWidget(rowCnt, 1, link);
//            headerRows.getFlexCellFormatter().setWidth(row, 1, "100%");

            // the name column
            MemberName who =
                _currentFolder == MailFolder.SENT_FOLDER_ID ? headers.recipient : headers.sender;
            Label sender = new Label(who.toString());
            sender.setStyleName("Sender");
            rows.setWidget(rowCnt, 2, sender);
            rows.getFlexCellFormatter().setWidth(rowCnt, 2, "150px");

            // the date the message was sent, in fancy shorthand form
            Label date = new Label(formatDate(headers.sent));
            date.setStyleName("Date");
            rows.setWidget(rowCnt, 3, date);
            rows.getFlexCellFormatter().setWidth(rowCnt, 3, "80px");

            rowCnt ++;
        }
        // when the UI is fully constructed, switch it in
        _headerContainer.setWidget(rows);

        boolean nextButton = _currentOffset + HEADER_ROWS < _headers.size();
        boolean prevButton = _currentOffset > 0;
        _pagerNext.setEnabled(nextButton);
        _pagerPrevious.setEnabled(prevButton);
        _pagerPages.clear();
        if (nextButton || prevButton) {
            _headerPager.setVisible(true);
            int pages = (_headers.size()+HEADER_ROWS-1) / HEADER_ROWS;
            for (int i = 0; i < pages; i ++ ) {
                Label page;
                if (i == PAGES_TO_SHOW && pages > 2*PAGES_TO_SHOW) {
                    // skip ahead
                    page = new Label("...");
                    page.setStyleName("CurrentPage");
                    i = pages - PAGES_TO_SHOW - 1;
                } else if (i * HEADER_ROWS == _currentOffset) {
                    page = new Label(String.valueOf(i+1));
                    page.setStyleName("CurrentPage");
                } else {
                    final int offset = i * HEADER_ROWS;
                    page = new Label(String.valueOf(i+1));
                    page.setStyleName("AnotherPage");
                    page.addClickListener(new ClickListener() {
                        public void onClick (Widget sender) {
                            _currentOffset = offset;
                            refreshHeaderPanel();
                            updateHistory();
                        }
                    });
                }
                _pagerPages.add(page);
            }
        } else {
            _headerPager.setVisible(false);
        }
    }

    // fetch the entirity (body, specifically) of a given message from the backend
    protected void loadMessage (final boolean cascadeUpdate)
    {
        if (_currentFolder < 0) {
            addError("Internal error: asked to load a message, but no folder selected.");
            return;
        }
        if (_currentMessage < 0) {
            return;
        }
        CMail.mailsvc.getMessage(CMail.creds, _currentFolder, _currentMessage, new AsyncCallback() {
            public void onSuccess (Object result) {
                _message = (MailMessage) result;
                _payloadDisplay = _message.payload != null ?
                    MailPayloadDisplay.getDisplay(_message) : null;
                refreshMessagePanel();
                // unread status may have changed: update dependent folders
                if (cascadeUpdate) {
                    if (_currentFolder > 0) {
                        loadHeaders();
                    }
                    loadFolders();
                }
            }
            public void onFailure (Throwable caught) {
                addError("Failed to fetch mail message from database: " + caught.getMessage());
            }
            
        });
    }

    // display a message in its full unsummarized glory
    protected void refreshMessagePanel ()
    {
        VerticalPanel messagePanel = new VerticalPanel();
        messagePanel.setStyleName("Message");

        // first the header line
        HorizontalPanel headers = new HorizontalPanel();
        headers.setStyleName("Header");
        Label subject = new Label(CMail.msgs.appHdrSubject() + ": " + _message.headers.subject);
        // TODO: Figure out wrapping for long subject lines
        subject.setStyleName("Subject");
        headers.add(subject); 
        Label sender = new Label(CMail.msgs.appHdrFrom() + ": " + _message.headers.sender);
        sender.setStyleName("Sender");
        headers.add(sender);
        Label date = new Label(formatDate(_message.headers.sent));
        date.setStyleName("Date");
        headers.add(date);
        messagePanel.add(headers);
        messagePanel.setCellWidth(headers, "100%");

        // if there is a payload, display it!
        if (_message.payload != null) {
            Widget widget = CMail.getMemberId() == _message.headers.recipient.getMemberId() ?
                _payloadDisplay.widgetForRecipient(this) : _payloadDisplay.widgetForOthers();
            if (widget != null) {
                HorizontalPanel panel = new HorizontalPanel();
                panel.setWidth("100%");
                panel.setStyleName("Payload");
                panel.add(widget);
                messagePanel.add(panel);
            }
        }
        
        // finally show the message text, if any, propped up with generated HTML
        if (_message.bodyText != null) {
            SimplePanel messageBody = new SimplePanel();
            messageBody.setStyleName("Body");
            messageBody.setWidget(textToHTML(_message.bodyText));
            messagePanel.add(messageBody);
        }

        // switch in the fully built UI
        _messageContainer.setVisible(true);
        _messageHolder.setWidget(messagePanel);
    }

    // since collection-to-array type checking is a lost cause anyway, just accept Object[] here
    protected void deleteMessages (Object[] objects)
    {
        if (objects.length == 0) {
            return;
        }
        // figure out which folder the messages are in
        int folderId = ((MailHeaders) objects[0]).folderId;
        // build an array of message id's from the array of message header objects
        int msgIds[] = new int[objects.length];
        for (int i = 0; i < objects.length; i ++) {
            MailHeaders mail = (MailHeaders) objects[i];
            if (mail.folderId != folderId) {
                // TODO: log this, it should definitely not be able to happen
                continue;
            }
            msgIds[i] = ((MailHeaders) objects[i]).messageId;
        }
        // then send the deletion request off to the backend
        CMail.mailsvc.deleteMessages(CMail.creds, folderId, msgIds, new AsyncCallback() {
            public void onSuccess (Object result) {
                // if it went well, refresh the folder view and whatnot
                refresh();
            }
            public void onFailure (Throwable caught) {
                addError("Failed to delete messages: " + caught.getMessage());
            }
        });
    }

    // anytime we wish to update the URL with a snapshot of the state, we call this
    protected void updateHistory ()
    {
        if (_currentMessage >= 0) {
            History.newItem("f" + _currentFolder + "." + _currentOffset + "." + _currentMessage);
        } else {
            History.newItem("f" + _currentFolder + "." + _currentOffset);
        }
    }

    // scans a text, generating HTML that respects leading/consecutive spaces and newlines,
    // while escaping actual HTML constructs
    protected Widget textToHTML (String message)
    {
        StringBuffer html = new StringBuffer();
        boolean collectSpaces = true;
        for (int i = 0; i < message.length(); i ++) {
            String bit;
            char c = message.charAt(i);
            switch(c) {
            case '\r':
                // completely ignore
                continue;
            case '<':
                // escape HTML
                bit = "&lt;";
                collectSpaces = false;
                break;
            case '>':
                // escape HTML
                bit = "&gt;";
                collectSpaces = false;
                break;
            case '&':
                // escape HTML
                bit = "&amp;";
                collectSpaces = false;
                break;
            case '\n':
                // a newline is replaced by a HTML break
                bit = "<br>\n";
                collectSpaces = true;
                break;
            case ' ': case '\t':
                // a single space is left alone, unless it leads a line
                if (!collectSpaces) {
                    collectSpaces = true;
                    bit = null;
                    break;
                }
                // but a leading space or consecutive spaces are replaced by these
                bit = "&nbsp;";
                break;
            default:
                collectSpaces = false;
                bit = null;
                break;
            }
            if (bit != null) {
                html.append(bit);
            } else {
                html.append(c);
            }
        }
        return new HTML(html.toString());
    }

    // generate a short string to summarize most relevantly a date in the past
    // For reference: Date.toString() returns: Wed Oct 25 2006 15:30:32 GMT-0500 (CDT)
    protected String formatDate (Date date)
    {
        long nowTime = System.currentTimeMillis();
        Date now = new Date(nowTime);
        if (now.getYear() != date.getYear()) {
            // e.g. 25/10/06
            return date.getDay() + "/" + date.getMonth() + "/" + date.getYear();
        }
        long hourDiff = (nowTime - date.getTime()) / (3600 * 1000);
        if (hourDiff > 6*24) {
            // e.g. Oct 25
            return date.toString().substring(4, 10);
        }
        if (hourDiff > 23) {
            // e.g. Wed 15:10
            String str = date.toString();
            return str.substring(0, 3) + " " + str.substring(16, 21);
        }
        // e.g. 15:10
        return date.toString().substring(16, 21);
    }
    
    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected static class MailCheckBox extends CheckBox
    {
        public MailHeaders headers;

        public MailCheckBox (MailHeaders headers)
        {
            this.headers = headers;
        }
    }

    protected List _folders;
    protected List _headers;
    protected MailMessage _message;
    protected MailPayloadDisplay _payloadDisplay;
    protected Set _checkedMessages = new HashSet();
    protected Set _checkboxes = new HashSet();
    protected int _currentFolder;
    protected int _currentOffset;
    protected int _currentMessage;

    protected Button _massDelete;
    
    protected SimplePanel _folderContainer;
    protected SimplePanel _headerContainer;
    protected VerticalPanel _messageContainer;
    protected SimplePanel _messageHolder;
    protected VerticalPanel _errorContainer;

    protected HorizontalPanel _headerPager;
    protected FlowPanel _pagerPages;
    protected Button _pagerPrevious;
    protected Button _pagerNext;
}
