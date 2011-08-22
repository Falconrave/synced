//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.threerings.msoy.web.gwt.MessageUtil;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import static com.threerings.msoy.Log.log;

/**
 * An RSS feed for whirled forums.
 */
public class RSSServlet extends HttpServlet
{
    @Override // from HttpServlet
    protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
        throws IOException
    {
        // Read the groupId off of the path
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        int groupId;
        try {
            groupId = Integer.parseInt(pathInfo.substring(1));
        } catch (NumberFormatException nfe) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            // load the group
            Group group = getGroup(groupId);
            if (group == null) {
                rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Make sure the group discussion is readable by non-members
            if (!group.checkAccess(GroupMembership.Rank.NON_MEMBER, Group.Access.READ, 0)) {
                rsp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // use cached feed if number of threads hasn't changed
            int numThreads = _forumRepo.loadThreadCount(groupId);
            String rss = loadCachedRSS(groupId, numThreads);

            if (rss == null) {
                List<ForumThreadRecord> threads =
                    _forumRepo.loadRecentThreads(group.groupId, THREAD_COUNT);
                List<ForumMessageRecord> messages = Lists.newArrayList();
                for (ForumThreadRecord thread : threads) {
                    List<ForumMessageRecord> message =
                        _forumRepo.loadMessages(thread.threadId, 0, 1);
                    messages.add(message.size() == 0 ? null : message.get(0));
                }
                rss = generateRSS(group, threads, messages, numThreads);
            }
            rsp.setContentType("application/xhtml+xml; charset=UTF-8");
            rsp.getWriter().print(rss);
            rsp.getWriter().flush();

        } catch (Exception e) {
            log.warning("Failed to generate rss feed [groupId=" + groupId + "].", e);
            rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
    }

    /**
     * Loads the specified group, returning null if the group doesn't exist.
     */
    protected Group getGroup (int groupId)
    {
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        return grec == null ? null : grec.toGroupObject();
    }

    /**
     * Checks for a cached rss xml file and returns it if found and up-to-date.
     */
    protected String loadCachedRSS (int groupId, int numThreads)
    {
        RSSCache cache = _rssCache.get(groupId);
        if (cache == null || cache.numThreads != numThreads) {
            return null;
        }
        return cache.rss;
    }

    /**
     * Generates the RSS XML from the group and threads.
     */
    protected String generateRSS (Group group, List<ForumThreadRecord> threads,
                                  List<ForumMessageRecord> messages, int numThreads)
    {
        StringBuilder rss = new StringBuilder("<?xml version=\"1.0\"?>");
        rss.append("<rss version=\"2.0\"><channel>");
        rss.append("<title>").append(group.name).append("</title>");
        rss.append("<link>").append(Pages.GROUPS.makeURL("f", group.groupId));
        rss.append("</link>");
        rss.append("<description>").append(group.blurb).append("</description>");
        if (messages.size() > 0) {
            String createdDate = _sdf.format(messages.get(0).created);
            rss.append("<pubDate>").append(createdDate).append("</pubDate>");
            rss.append("<lastBuildDate>").append(createdDate).append("</lastBuildDate>");
        }
        for (int ii = 0, nn = threads.size(); ii < nn; ii++) {
            ForumThreadRecord thread = threads.get(ii);
            ForumMessageRecord message = messages.get(ii);
            if (message == null) {
                continue;
            }
            rss.append("<item>");
            rss.append("<title>").append(thread.subject).append("</title>");
            rss.append("<link>");
            rss.append(Pages.GROUPS.makeURL("t", thread.threadId));
            rss.append("</link>");
            rss.append("<description>");
            rss.append("<![CDATA[ ").append(MessageUtil.expandMessage(message.message));
            rss.append("]]></description>");
            rss.append("<pubDate>").append(_sdf.format(message.created)).append("</pubDate>");
            rss.append("<guid>");
            rss.append(Pages.GROUPS.makeURL("t", thread.threadId));
            rss.append("</guid>");
            rss.append("</item>");
        }
        rss.append("</channel></rss>");
        String result = rss.toString();
        _rssCache.put(group.groupId, new RSSCache(result, numThreads));
        return result;
    }

    protected class RSSCache
    {
        /** The generated rss feed */
        String rss;

        /** The number of threads when this feed was generated. */
        int numThreads;

        public RSSCache (String rss, int numThreads)
        {
            this.rss = rss;
            this.numThreads = numThreads;
        }
    }

    // our dependencies
    @Inject protected ForumRepository _forumRepo;
    @Inject protected GroupRepository _groupRepo;

    /** Cache our generated rss files. */
    protected static Map<Integer, RSSCache> _rssCache = Maps.newHashMap();

    /** Used for RFC 822 compliant date strings. */
    protected static SimpleDateFormat _sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    /** The number of threads to have in the feed. */
    protected static final int THREAD_COUNT = 15;
}
