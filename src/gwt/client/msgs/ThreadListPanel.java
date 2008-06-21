//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.fora.data.ForumThread;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ClickCallback;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;
import client.util.SearchBox;

/**
 * Displays a list of threads.
 */
public class ThreadListPanel extends PagedGrid
    implements SearchBox.Listener
{
    public ThreadListPanel (ForumPanel parent)
    {
        super(THREADS_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setWidth("100%");
        _parent = parent;
    }

    public void displayGroupThreads (int groupId, ForumModels fmodels)
    {
        _groupId = groupId;
        _fmodels = fmodels;
        setModel(fmodels.getGroupThreads(groupId), 0);
    }

    public void displayUnreadThreads (ForumModels fmodels, boolean refresh)
    {
        _groupId = 0;
        _fmodels = fmodels;
        setModel(fmodels.getUnreadThreads(refresh), 0);
    }
    
    // from interface SearchBox.Listener
    public void search (String search)
    {
        CMsgs.forumsvc.findThreads(CMsgs.ident, _groupId, search, MAX_RESULTS, new MsoyCallback() {
            public void onSuccess (Object result) {
                setModel(new SimpleDataModel((List)result), 0);
            }
        });
    }

    // from interface SearchBox.Listener
    public void clearSearch ()
    {
        setModel(_fmodels.getGroupThreads(_groupId), 0);
    }

    @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new ThreadSummaryPanel((ForumThread)item);
    }

    @Override // from PagedGrid
    protected Widget createEmptyContents ()
    {
        if (_groupId != 0) {
            return super.createEmptyContents();
        }

        HTML empty = new HTML(CMsgs.mmsgs.noUnreadThreads());
        empty.setStyleName("Empty");
        return empty;
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CMsgs.mmsgs.noThreads();
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

        // add a button for starting a new thread that will optionally be enabled later
        _startThread = new Button(CMsgs.mmsgs.tlpStartNewThread(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.startNewThread(_groupId);
            }
        });
        _startThread.setEnabled(false);
        controls.setWidget(0, 0, _startThread);

        // add a button for refreshing our unread thread list
        _refresh = new Button(CMsgs.mmsgs.tlpRefresh(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.displayUnreadThreads(true);
            }
        });
        controls.setWidget(0, 1, _refresh);
    }

    @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        super.displayResults(start, count, list);

        if (_model instanceof ForumModels.GroupThreads) { 
            _startThread.setVisible(true);
            _startThread.setEnabled(((ForumModels.GroupThreads)_model).canStartThread());
            _refresh.setVisible(false);
            _refresh.setEnabled(false);
        } else {
            _startThread.setVisible(false);
            _startThread.setEnabled(false);
            if (_model instanceof ForumModels.UnreadThreads) {
                _refresh.setVisible(true);
                _refresh.setEnabled(true);
            }
        }
    }

    protected class ThreadSummaryPanel extends FlexTable
    {
        public ThreadSummaryPanel (final ForumThread thread)
        {
            setStyleName("threadSummaryPanel");
            setCellPadding(0);
            setCellSpacing(0);

            int col = 0;
            Image statusImage = new Image();
            if (thread.hasUnreadMessages()) {
                statusImage.setUrl("/images/msgs/unread.png");
                statusImage.setTitle(CMsgs.mmsgs.tlpStatusUnreadTip());
            }
            else {
                statusImage.setUrl("/images/msgs/read.png");
                statusImage.setTitle(CMsgs.mmsgs.tlpStatusReadTip());
            }
            setWidget(0, col, statusImage);
            getFlexCellFormatter().setStyleName(0, col++, "Status");

            RowPanel bits = new RowPanel();
            for (int ii = 0; ii < FLAG_IMAGES.length; ii++) {
                if ((thread.flags & (1 << ii)) != 0) {
                    Image image = new Image("/images/msgs/" + FLAG_IMAGES[ii] + ".png");
                    image.setTitle(FLAG_TIPS[ii]);
                    bits.add(image);
                }
            }

            Hyperlink toThread;
            if (thread.hasUnreadMessages()) {
                String args = threadArgs(
                    thread.threadId, thread.lastReadPostIndex, thread.lastReadPostId);
                toThread = Application.createLink(thread.subject, Page.WHIRLEDS, args);
                toThread.setTitle(CMsgs.mmsgs.tlpFirstUnreadTip());
            } else {
                toThread = Application.createLink(
                    thread.subject, Page.WHIRLEDS, threadArgs(thread.threadId, 0, 0));
            }
            bits.add(toThread);

            // if we're displaying unread threads from many groups, display the group name after
            // the subject
            if (_groupId == 0) {
                bits.add(MsoyUI.createLabel(CMsgs.mmsgs.tlpFromGroup(thread.group.toString()),
                                            "tipLabel"), HasAlignment.ALIGN_BOTTOM);
            }

            setWidget(0, col, bits);
            getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_LEFT);
            getFlexCellFormatter().setStyleName(0, col++, "Subject");

            setText(0, col, "" + thread.posts);
            getFlexCellFormatter().setStyleName(0, col++, "Posts");

            VerticalPanel mrp = new VerticalPanel();
            mrp.add(new Label(_pdate.format(thread.mostRecentPostTime)));
            Hyperlink latest = Application.createLink(
                CMsgs.mmsgs.tlpBy(thread.mostRecentPoster.toString()),
                Page.WHIRLEDS, threadArgs(thread.threadId, thread.posts-1, 
                thread.mostRecentPostId));
            latest.setTitle(CMsgs.mmsgs.tlpLastTip());
            mrp.add(latest);
            setWidget(0, col, mrp);
            getFlexCellFormatter().setStyleName(0, col++, "LastPost");
            
            // add an ignore button when displaying unread threads from many groups
            if (_groupId == 0) {
                Image ignoreThread = MsoyUI.createImage("/images/msgs/ignore.png", "Ignore");
                ignoreThread.setTitle(CMsgs.mmsgs.ignoreThreadTip());
                new ClickCallback<Void>(ignoreThread) {
                    public boolean callService () {
                        CMsgs.forumsvc.ignoreThread(CMsgs.ident, thread.threadId, this);
                        return true;
                    }
                    public boolean gotResult (Void result) {
                        MsoyUI.info(CMsgs.mmsgs.threadIgnored());
                        setModel(_fmodels.getUnreadThreads(true), getPage());
                        return false;
                    }
                };
                setWidget(0, col, ignoreThread);
                getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
                getFlexCellFormatter().setStyleName(0, col++, "IgnoreThread");
            }
        }
    }

    protected String threadArgs (int threadId, int msgIndex, int msgId)
    {
        String[] args = new String[msgIndex > 0 ? 4 : 2];
        args[0] = "t";
        args[1] = String.valueOf(threadId);
        if (msgIndex > 0) {
            args[2] = String.valueOf(msgIndex / MessagesPanel.MESSAGES_PER_PAGE);
            args[3] = String.valueOf(msgId);
        }
        return Args.compose(args);
    }

    /** The forum panel in which we're hosted. */
    protected ForumPanel _parent;

    /** Provides access to our forum models. */
    protected ForumModels _fmodels;

    /** Contains the id of the group whose threads we are displaying or zero. */
    protected int _groupId;

    /** Our unread threads data model. */
    protected DataModel _unreadModel;

    /** A button for starting a new thread. */
    protected Button _startThread;

    /** A button for refreshing the current model. */
    protected Button _refresh;

    /** Used to format the most recent post date. */
    protected static SimpleDateFormat _pdate = new SimpleDateFormat("MMM dd, yyyy h:mm aa");

    /** The number of threads displayed per page (TODO: base this on browser height). */
    protected static final int THREADS_PER_PAGE = 10;

    /** The maximum number of thread search results. */
    protected static final int MAX_RESULTS = 20;

    /** Images displayed next to threads that have special flags. */
    protected static final String[] FLAG_IMAGES = { "announce", "sticky", "locked" };

    /** Tooltips for our image icons. */
    protected static final String[] FLAG_TIPS = {
        CMsgs.mmsgs.tlpAnnounceTip(), CMsgs.mmsgs.tlpStickyTip(), CMsgs.mmsgs.tlpLockedTip()
    };
}
