//
// $Id$

package client.msgs;

import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ListenerList;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumServiceAsync;
import com.threerings.msoy.fora.gwt.ForumThread;

import client.util.PagedServiceDataModel;
import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Various data models used by the forum services.
 */
public class ForumModels
{
    /** A data model that provides a particular group's threads. */
    public static class GroupThreads
        extends PagedServiceDataModel<ForumThread, ForumService.ThreadResult>
    {
        public GroupThreads (int groupId) {
            _groupId = groupId;
            _group = new GroupName("", _groupId);
        }

        public GroupName getGroup () {
            return _group;
        }

        public boolean canStartThread () {
            return _canStartThread;
        }

        public boolean isManager () {
            return _isManager;
        }

        public boolean isAnnounce () {
            return _isAnnounce;
        }

        /**
         * Are the above calls returning data from the server or just their default constructed
         * values?
         */
        public boolean isFetched () {
            return _fetched;
        }

        /**
         * Get the information from the server to be able to accurately respond to the above
         * requests.
         */
        public void doFetch (final AsyncCallback<Void> callback) {
            doFetchRows(0, 1, new AsyncCallback<List<ForumThread>>() {
                public void onFailure (Throwable caught) {
                    callback.onFailure(caught);
                }
                public void onSuccess (List<ForumThread> result) {
                    callback.onSuccess(null);
                }
            });
        }

        /**
         * Requests to be informed when we obtain our group name from the first batch of thread
         * results. {@link AsyncCallback#onSuccess} will be called with the {@link GroupName} when
         * we learn it. {@link AsyncCallback#onFailure} will never be called but this interface is
         * more convenient than Command which does not allow us to pass an argument.
         */
        public void addGotNameListener (AsyncCallback<GroupName> onGotGroupName) {
            _gotNameListeners = ListenerList.addListener(_gotNameListeners, onGotGroupName);
            // if we already have our group name, fire the callback immediately
            if (!_group.toString().equals("")) {
                gotGroupName(_group);
            }
        }

        /**
         * Looks up the specified thread in the set of all threads ever fetched by this model.
         */
        public ForumThread getThread (int threadId)
        {
            return _threads.get(threadId);
        }

        @Override // from ServiceBackedDataModel
        public void prependItem (ForumThread thread) {
            super.prependItem(thread);
            mapThread(thread);
        }

        @Override // from ServiceBackedDataModel
        public void appendItem (ForumThread thread) {
            super.appendItem(thread);
            mapThread(thread);
        }

        @Override // from ServiceBackedDataModel
        protected void setCurrentResult (ForumService.ThreadResult result)
        {
            _fetched = true;
            _canStartThread = result.canStartThread;
            _isManager = result.isManager;
            _isAnnounce = result.isAnnounce;
            // note all of our threads so that we can provide them later to non-PagedGrid consumers
            for (ForumThread thread : result.page) {
                mapThread(thread);
            }
            // grab our real group name from one of our thread records
            if (result.page.size() > 0) {
                gotGroupName(result.page.get(0).group);
            }
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<ForumService.ThreadResult> callback)
        {
            _forumsvc.loadThreads(_groupId, start, count, needCount, callback);
        }

        protected void mapThread (ForumThread thread) {
            _threads.put(thread.threadId, thread);
        }

        protected void gotGroupName (GroupName group) {
            _group = group;
            if (_gotNameListeners != null) {
                _gotNameListeners.notify(new ListenerList.Op<AsyncCallback<GroupName>>() {
                    public void notify (AsyncCallback<GroupName> listener) {
                        listener.onSuccess(_group);
                    }
                });
                _gotNameListeners = null;
            }
        }

        protected int _groupId;
        protected GroupName _group;
        protected boolean _fetched;
        protected boolean _canStartThread, _isManager, _isAnnounce;

        protected ListenerList<AsyncCallback<GroupName>> _gotNameListeners;
        protected HashMap<Integer, ForumThread> _threads = new HashMap<Integer, ForumThread>();
    }

    /** A data model that provides all threads unread by the authenticated user. */
    public static class UnreadThreads extends SimpleDataModel<ForumThread>
    {
        public UnreadThreads ()
        {
            super(null);
        }

        /**
         * Looks up the specified thread in the set of all threads ever fetched by this model.
         */
        public ForumThread getThread (int threadId)
        {
            return _threads.get(threadId);
        }

        // from interface DataModel
        public void removeItem (ForumThread thread)
        {
            _threads.remove(thread.threadId);
            super.removeItem(thread);
        }

        // from interface DataModel
        public void doFetchRows (
            final int start, final int count, final AsyncCallback<List<ForumThread>> callback)
        {
            if (_items != null) {
                super.doFetchRows(start, count, callback);
                return;
            }

            _forumsvc.loadUnreadThreads(MAX_UNREAD_THREADS,
                                        new AsyncCallback<List<ForumThread>>() {
                public void onSuccess (List<ForumThread> result) {
                    _items = result;
                    for (ForumThread thread : result) {
                        _threads.put(thread.threadId, thread);
                    }
                    doFetchRows(start, count, callback);
                }
                public void onFailure (Throwable failure) {
                    callback.onFailure(failure);
                }
           });
        }

        protected HashMap<Integer, ForumThread> _threads = new HashMap<Integer, ForumThread>();
    }

    /** A data model that provides a particular thread's messages. */
    public static class ThreadMessages
        extends ServiceBackedDataModel<ForumMessage, ForumService.MessageResult>
    {
        public ThreadMessages (int threadId, ForumThread thread) {
            _threadId = threadId;
            if (thread != null) {
                _thread = thread;
                _count = _thread.posts;
            }
        }

        public ForumThread getThread () {
            return _thread;
        }

        public boolean canPostReply () {
            return _canPostReply;
        }

        public boolean isManager () {
            return _isManager;
        }

        @Override // from ServiceBackedDataModel
        public void appendItem (ForumMessage message) {
            super.appendItem(message);
            _thread.posts++;
            // mark our thread as read up to this message
            _thread.lastReadPostId = message.messageId;
            _thread.lastReadPostIndex = _thread.posts;
        }

        @Override // from ServiceBackedDataModel
        public void removeItem (ForumMessage message) {
            // if we're deleting the last message in this thread...
            if (_thread.mostRecentPostId == message.messageId) {
                // ...locate the new last message and update our thread with its info
                int idx = _pageItems.indexOf(message);
                if (idx > 0) { // it's in the list and not the first item
                    ForumMessage prev = _pageItems.get(idx-1);
                    _thread.mostRecentPostId = prev.messageId;
                    _thread.mostRecentPoster = prev.poster.name;
                    _thread.mostRecentPostTime = prev.created;
                }
            }

            super.removeItem(message);
            _thread.posts--;
        }

        @Override // from ServiceBackedDataModel
        protected void onSuccess (ForumService.MessageResult result,
                                  AsyncCallback<List<ForumMessage>> callback) {
            // note some bits
            if (result.thread != null) {
                _thread = result.thread;
            }
            _canPostReply = result.canPostReply;
            _isManager = result.isManager;

            // let the PagedGrid know that we're good and to render the items
            super.onSuccess(result, callback);

            // finally update our thread's last read post id so that subsequent renders will show
            // messages as having been read
            if (result.messages.size() > 0) {
                int lastReadIndex = result.messages.size()-1;
                int highestPostId = (result.messages.get(lastReadIndex)).messageId;
                if (highestPostId > _thread.lastReadPostId) {
                    _thread.lastReadPostId = highestPostId;
                    _thread.lastReadPostIndex = _pageOffset + lastReadIndex;
                }
            }
        }

        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<ForumService.MessageResult> callback)
        {
            _forumsvc.loadMessages(
                _threadId, _thread.lastReadPostId, start, count, needCount, callback);
        }

        @Override // from ServiceBackedDataModel
        protected int getCount (ForumService.MessageResult result) {
            return result.thread.posts;
        }

        @Override // from ServiceBackedDataModel
        protected List<ForumMessage> getRows (ForumService.MessageResult result) {
            return result.messages;
        }

        protected int _threadId;
        protected ForumThread _thread = new ForumThread(); // dummy to make logic easier
        protected boolean _canPostReply, _isManager;
    }

    /**
     * Notifies the cache that a new thread was posted.
     */
    public void newThreadPosted (ForumThread thread)
    {
        // mark this thread as already read
        thread.lastReadPostId = thread.mostRecentPostId;
        thread.lastReadPostIndex = thread.posts;

        // if we already have this model loaded, let it know about the new thread
        GroupThreads gmodel = _gmodels.get(thread.group.getGroupId());
        if (gmodel != null) {
            gmodel.prependItem(thread);
        }
    }

    /**
     * Returns, creating if necessary, a data model that provides all of the threads for the
     * specified group.
     */
    public GroupThreads getGroupThreads (int groupId)
    {
        GroupThreads gmodel = _gmodels.get(groupId);
        if (gmodel == null) {
            _gmodels.put(groupId, gmodel = new GroupThreads(groupId));
        }
        return gmodel;
    }

    /**
     * Returns, creating if necessary, the data model that provides all unread threads for the
     * authenticated user.
     */
    public UnreadThreads getUnreadThreads (boolean refresh)
    {
        if (refresh || _unreadModel == null) {
            _unreadModel = new UnreadThreads();
        }
        return _unreadModel;
    }

    /**
     * Locates the thread in question in the cache. Returns null if the thread could not be found.
     */
    public ForumThread findThread (int threadId)
    {
        // check for the thread in our unread threads model if we have one
        if (_unreadModel != null) {
            ForumThread thread = _unreadModel.getThread(threadId);
            if (thread != null) {
                return thread;
            }
        }

        // next, check for the thread in the group models
        for (GroupThreads model : _gmodels.values()) {
            ForumThread thread = model.getThread(threadId);
            if (thread != null) {
                return thread;
            }
        }

        return null;
    }

    /**
     * Searches a group's threads for a string and invokes a callback when the results are ready.
     */
    public void searchGroupThreads (int groupId, String query,
        AsyncCallback<List<ForumThread>> callback)
    {
        if (_search == null || !_search.equals(groupId, query)) {
            _search = new Search(groupId, query);
        }
        _search.execute(callback);
    }

    /**
     * Searches the user's unread threads for a string and invokes a callback when the results are
     * ready.
     */
    public void searchUnreadThreads (String query, AsyncCallback<List<ForumThread>> callback)
    {
        searchGroupThreads(0, query, callback);
    }

    /**
     * Parameters and results of searching a group's threads or the user's unread threads.
     */
    protected static class Search
    {
        public Search (int groupId, String query) {
            _groupId = groupId;
            _query = query;
        }

        public boolean equals (int groupId, String query) {
            return _query.equals(_query) && _groupId == groupId;
        }

        public void execute (final AsyncCallback<List<ForumThread>> callback) {
            if (_result != null) {
                callback.onSuccess(_result);
            }
            doSearch(new AsyncCallback<List<ForumThread>> () {
                public void onSuccess (List<ForumThread> result) {
                    _result = result;
                    callback.onSuccess(result);
                }
                public void onFailure (Throwable cause) {
                    callback.onFailure(cause);
                }
            });
        }

        protected void doSearch (AsyncCallback<List<ForumThread>> callback) {
            if (_groupId == 0) {
                _forumsvc.findUnreadThreads(_query, MAX_RESULTS, callback);
            } else {
                _forumsvc.findThreads(_groupId, _query, MAX_RESULTS, callback);
            }
        }

        protected int _groupId;
        protected String _query;
        protected List<ForumThread> _result;
    }

    /** A cache of GroupThreads data models. */
    protected HashMap<Integer, GroupThreads> _gmodels = new HashMap<Integer, GroupThreads>();

    /** A cached UnreadThreads data model. */
    protected UnreadThreads _unreadModel;

    /** A cached search result. */
    protected Search _search;

    protected static final ForumServiceAsync _forumsvc = (ForumServiceAsync)
        ServiceUtil.bind(GWT.create(ForumService.class), ForumService.ENTRY_POINT);

    /** The maximum number of unread threads we'll download at once. */
    protected static final int MAX_UNREAD_THREADS = 100;

    /** The maximum number of thread search results. */
    protected static final int MAX_RESULTS = 20;
}
