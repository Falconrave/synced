//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumServiceAsync;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.images.msgs.MsgsImages;
import client.shell.CShell;
import client.ui.ComplainPopup;
import client.ui.MiniNowLoadingWidget;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.util.ClickCallback;
import client.util.Link;
import client.util.InfoCallback;

/**
 * Displays the messages in a particular thread.
 */
public class MessagesPanel extends PagedGrid<ForumMessage>
{
    public MessagesPanel (ThreadPanel parent)
    {
        super(ForumThread.MESSAGES_PER_PAGE, 1, NAV_ON_BOTTOM);
        setCellAlignment(HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_TOP);
        addStyleName("dottedGrid");
        setWidth("100%");
        setHeight("100%");

        _parent = parent;
    }

    public void display (ForumModels.ThreadMessages model, int page, int scrollToId)
    {
        _tmodel = model;
        _scrollToId = scrollToId;
        setModel(_tmodel, page);
    }

    public void restoreThread ()
    {
        setModel(_tmodel, 0);
    }

    public void refreshDisplay ()
    {
        displayPage(_page, true);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return (_model == _tmodel) ? _mmsgs.noMessages() : _mmsgs.noMatches();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always show our navigation for consistency
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for starting a new message that will optionally be enabled later
        _postReply = new Button(_mmsgs.postReply(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                _parent.postReply(null, false);
            }
        });
        _postReply.setEnabled(false);
        controls.setWidget(0, 0, _postReply);

        // add a button for ignoring this thread
        _ignoreThread = new Button(_mmsgs.ignoreThread());
        _ignoreThread.setTitle(_mmsgs.ignoreThreadTip());
        new ClickCallback<Void>(_ignoreThread) {
            @Override protected boolean callService () {
                _forumsvc.ignoreThread(_parent.getThreadId(), this);
                return true;
            }
            @Override protected boolean gotResult (Void result) {
                MsoyUI.info(_mmsgs.threadIgnored());
                return false;
            }
        };
        controls.setWidget(0, 1, _ignoreThread);

        // add a button for editing this thread's metadata
        _editThread = new Button(_mmsgs.editThread(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                _parent.editThread();
            }
        });
        _editThread.setEnabled(false);
        controls.setWidget(0, 2, _editThread);
    }

    @Override // from PagedGrid
    protected void displayResults (int start, int count, List<ForumMessage> list)
    {
        // if we're displaying results from our main thread model, update our ephemera; this must
        // be done before the call to super because super creates our widgets and those check our
        // ephmera to determine how they lay themselves out
        if (_model == _tmodel) {
            _parent.gotThread(_tmodel.getThread());
            boolean canReply = _tmodel.canPostReply() && !_tmodel.getThread().isLocked();
            _postReply.setEnabled(canReply || CShell.isSupport());
            _editThread.setEnabled(_tmodel.isManager() || CShell.isSupport());
        }

        super.displayResults(start, count, list);

        if (_scrollTarget != null) {
            DeferredCommand.addCommand(new Command() {
                public void execute () {
                    scrollPage(_scrollTarget.getElement().getAbsoluteTop());
                    _scrollTarget = null;
                }
            });
        }
    }

    @Override // from PagedGrid
    protected Widget createWidget (ForumMessage message)
    {
        ThreadMessagePanel panel = new ThreadMessagePanel(
            _tmodel.getThread(), message, _tmodel.isManager());
        if (message.messageId == _scrollToId) {
            _scrollToId = 0;
            _scrollTarget = panel;
        } else if (_scrollTarget == null) {
            _scrollTarget = panel;
        }
        return panel;
    }

    protected void replyPosted (ForumMessage message)
    {
        MsoyUI.info(_mmsgs.msgReplyPosted());
        _tmodel.appendItem(message);
        if (_model == _tmodel) {
            refreshDisplay();
        }
        // TODO: what to do if you post a reply while searching?
    }

    protected Command deletePost (final ForumMessage message)
    {
        return new Command() {
            public void execute () {
                // TODO: if forum admin, make them send a mail to the poster explaining why their
                // post was deleted?
                _forumsvc.deleteMessage(message.messageId, new InfoCallback<Void>() {
                    public void onSuccess (Void result) {
                        removeItem(message);
                        MsoyUI.info(_mmsgs.msgPostDeleted());
                    }
                });
            }
        };
    }

    protected static Widget makeInfoImage (
        AbstractImagePrototype iproto, String tip, ClickHandler onClick)
    {
        Widget image = MsoyUI.makeActionImage(iproto.createImage(), tip, onClick);
        image.addStyleName("ActionIcon");
        return image;
    }

    protected static InlineLabel makeInfoLabel (String text, ClickHandler listener)
    {
        InlineLabel label = new InlineLabel(text, false, true, false);
        label.addClickHandler(listener);
        label.addStyleName("Posted");
        label.addStyleName("actionLabel");
        return label;
    }

    protected class ThreadMessagePanel extends SimpleMessagePanel
    {
        public ThreadMessagePanel (ForumThread thread, ForumMessage message, boolean isManager)
        {
            _isManager = isManager;
            _thread = thread;
            setMessage(message);
        }

        @Override // from MessagePanel
        public void setMessage (ForumMessage message)
        {
            _message = message;
            super.setMessage(message);
            FlowPanel messageFooter = MsoyUI.createFlowPanel("MessageFooter");

            FlowPanel actionButtons = MsoyUI.createFlowPanel("ActionButtons");
            messageFooter.add(actionButtons);
            if (_postReply.isEnabled()) {
                actionButtons.add(makeInfoImage(
                    _images.reply_post_quote(), _mmsgs.inlineQReply(), new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            _parent.postReply(_message, true);
                        }
                    }));
                actionButtons.add(makeInfoImage(
                    _images.reply_post(), _mmsgs.inlineReply(), new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            _parent.postReply(_message, false);
                        }
                    }));
            }
            // this my have already been done in SimpleMessagePanel; if so overwrite it
            if (!_message.lastEdited.equals(_message.created)) {
                messageFooter.add(new Label(
                    _mmsgs.msgPostEditedOn(DateUtil.formatDateTime(_message.lastEdited))));
            }
            getFlexCellFormatter().setRowSpan(0, 0, 3); // extend the photo cell
            setWidget(2, 0, messageFooter);
            getFlexCellFormatter().setStyleName(2, 0, "Posted");
            getFlexCellFormatter().addStyleName(2, 0, "LeftPad");
            getFlexCellFormatter().addStyleName(2, 0, "RightPad");
            getFlexCellFormatter().addStyleName(2, 0, "BottomPad");
        }

        @Override // from MessagePanel
        protected boolean shouldShowRoleCaption ()
        {
            return true;
        }

        @Override // from MessagePanel
        protected void addInfo (FlowPanel info)
        {
            super.addInfo(info);

            final int memberId = CShell.getMemberId();
            final boolean isPoster = (memberId == _message.poster.name.getMemberId());
            if (!isPoster) {
                info.add(makeInfoImage(_images.sendmail(), _mmsgs.inlineMail(),
                                       Link.createHandler(Pages.MAIL, "w", "m",
                                                           _message.poster.name.getMemberId())));
            }

            FlowPanel toolBar = MsoyUI.createFlowPanel("ToolBar");
            info.insert(toolBar, 0);

            if (isPoster || CShell.isSupport()) {
                toolBar.add(makeInfoImage(_images.edit_post(), _mmsgs.inlineEdit(),
                                          new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        _parent.editPost(_message, new InfoCallback<ForumMessage>() {
                            public void onSuccess (ForumMessage message) {
                                setMessage(message);
                            }
                        });
                    }
                }));
            }

            if (!isPoster) {
                toolBar.add(makeInfoImage(_images.complain_post(), _mmsgs.inlineComplain(),
                    new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            if (MsoyUI.requireValidated()) {
                                new ForumMessageComplainPopup(_message).show();
                            }
                        }
                    }));
            }

            if ((CShell.isValidated() && (_isManager || isPoster)) || CShell.isSupport()) {
                toolBar.add(makeInfoImage(_images.delete_post(), _mmsgs.inlineDelete(),
                                          new PromptPopup(_mmsgs.confirmDelete(),
                                                          deletePost(_message))));
            }

            if (_message.issueId > 0) {
                ClickHandler viewClick = Link.createHandler(Pages.ISSUES, "i", _message.issueId);
                toolBar.add(makeInfoImage(_images.view_issue(), _mmsgs.inlineIssue(), viewClick));

            } else if (CShell.isSupport()) {
                toolBar.add(makeInfoImage(_images.new_issue(), _mmsgs.inlineNewIssue(),
                                          Link.createHandler(
                                              Pages.ISSUES, "create", _message.messageId)));
                toolBar.add(makeInfoImage(_images.assign_issue(), _mmsgs.inlineAssignIssue(),
                                          Link.createHandler(
                                              Pages.GROUPS, "assign", _message.messageId, _page)));
            }
        }

        @Override // from MessagePanel
        protected Widget createIcon ()
        {
            String path = "/images/msgs/" +
                ((_message.messageId > _thread.lastReadPostId) ? "unread" : "read") + ".png";
            Args args = _thread.getPagePostArgs(_page, _message.messageId);
            return Link.createImage(path, _mmsgs.permaLink(), Pages.GROUPS, args);
        }

        protected ForumThread _thread;
        protected ForumMessage _message;
        protected boolean _isManager;
    }

    protected class ForumMessageComplainPopup extends ComplainPopup
    {
        public ForumMessageComplainPopup (ForumMessage message)
        {
            super(ForumService.MAX_COMPLAINT_LENGTH);
            _message = message;
        }

        protected boolean callService ()
        {
            _forumsvc.complainMessage(_description.getText(), _message.messageId, this);
            return true;
        }

        protected ForumMessage _message;
    }

    @Override// from PagedWidget
    protected Widget getNowLoadingWidget ()
    {
        return new MiniNowLoadingWidget();
    }

    protected static native void scrollPage (int position) /*-{
        $doc.documentElement.scrollTop = position;
    }-*/;

    /** The thread panel in which we're hosted. */
    protected ThreadPanel _parent;

    /** Our thread messages model. We keep this around because we may temporarily replace it with a
     * search model if the user does a search. */
    protected ForumModels.ThreadMessages _tmodel;

    /** A message to scroll into view when we first receive our messages. */
    protected int _scrollToId;

    /** The panel to which we want to scroll once our page is laid out. */
    protected Widget _scrollTarget;

    protected Button _postReply, _ignoreThread, _editThread;

    protected static final MsgsImages _images = GWT.create(MsgsImages.class);
    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final ForumServiceAsync _forumsvc = (ForumServiceAsync)
        ServiceUtil.bind(GWT.create(ForumService.class), ForumService.ENTRY_POINT);
}
