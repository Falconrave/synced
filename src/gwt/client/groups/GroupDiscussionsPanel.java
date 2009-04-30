//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MessageUtil;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Displays the members of a particular Group. Allows managers to manage ranks and membership.
 */
public class GroupDiscussionsPanel extends FlowPanel
{
    public GroupDiscussionsPanel (GroupDetail detail)
    {
        setStyleName("groupDiscussionPanel");
        _detail = detail;

        FlowPanel rss = new FlowPanel();
        rss.setStyleName("RSS");
        ClickHandler rssClick = new ClickHandler() {
            public void onClick (ClickEvent event) {
                Window.open("/rss/" + _detail.group.groupId, "_blank", "");
            }
        };
        Anchor rssText = new Anchor("/rss/" + _detail.group.groupId, "", "_blank");
        rssText.setHTML(_msgs.discussionRss());
        rssText.setStyleName("RssText");
        rss.add(rssText);
        Image rssImage = MsoyUI.createActionImage(
            "/images/group/thread_rss.png", _msgs.discussionRssTip(), rssClick);
        rss.add(rssImage);
        add(rss);

        // there are no threads, print a message
        if (_detail.threads.size() == 0) {
            add(MsoyUI.createHTML(_msgs.discussionNoThreads(), "NoThreads"));
            return;
        }

        // add a ThreadWidget and divider for each thread
        for (int ii = 0; ii < _detail.threads.size(); ii++) {
            ForumThread thread = _detail.threads.get(ii);
            add(new ThreadWidget(thread));
            if (_detail.threads.size() > ii + 1) {
                SimplePanel divider = new SimplePanel();
                divider.setStyleName("Divider");
                add(divider);
            }
        }
    }

    /**
     * Displays the original post of a single thread.
     */
    protected class ThreadWidget extends FlowPanel
    {
        public ThreadWidget (final ForumThread thread) {
            setStyleName("Thread");

            Label date = new Label(MsoyUI.formatDate(thread.mostRecentPostTime));
            date.setStyleName("Date");
            add(date);

            Widget subject = Link.create(
                thread.subject, Pages.GROUPS, Args.compose("t", thread.threadId));
            subject.setStyleName("Subject");
            add(subject);

            add(MsoyUI.createHTML(MessageUtil.expandMessage(thread.firstPost.message), "Text"));

            ClickHandler posterClick = Link.createListener(
                Pages.PEOPLE, "" + thread.firstPost.poster.name.getMemberId());
            Widget posterIcon = MediaUtil.createMediaView(
                thread.firstPost.poster.photo, MediaDesc.HALF_THUMBNAIL_SIZE, posterClick);
            posterIcon.setStyleName("PostedByIcon");
            add(posterIcon);

            // posted by <a href="#people-{ID}">{NAME}</a> at {TIME}
            FlowPanel postedBy = new FlowPanel();
            postedBy.addStyleName("PostedBy");
            postedBy.add(new InlineLabel(_msgs.discussionPostedBy() + " "));
            InlineLabel author = new InlineLabel(thread.firstPost.poster.name.toString());
            author.addClickHandler(posterClick);
            author.addStyleName("actionLabel");
            postedBy.add(author);
            postedBy.add (new InlineLabel(
                " " + _msgs.discussionAt(MsoyUI.formatTime(thread.firstPost.created))));
            add(postedBy);

            final String repliesText;
            if (thread.posts == 2) {
                // one reply - singular
                repliesText = _msgs.discussionReply("1");
            } else {
                // 0 or 2+ replies
                repliesText = _msgs.discussionReplies("" + (thread.posts-1));
            }
            Widget replies = Link.create(
                repliesText, Pages.GROUPS, Args.compose("t", thread.threadId));
            replies.setStyleName("Replies");
            add(replies);
        }
    }

    /** Infoes about the group we're in for constructing links etc */
    protected GroupDetail _detail;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
}
