//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.util.HTMLSanitizer;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;

import com.threerings.msoy.fora.data.ForumCodes;
import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.fora.server.persist.ReadTrackingRecord;

import com.threerings.msoy.web.client.ForumService;
import com.threerings.msoy.web.data.GroupCard;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.MessageTooLongException;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ForumService}.
 */
public class ForumServlet extends MsoyServiceServlet
    implements ForumService
{
    // from interface ForumService
    public ThreadResult loadUnreadThreads (WebIdent ident, int maximum)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            // load up said member's group memberships
            Map<Integer, GroupName> groups = Maps.newHashMap();
            for (GroupCard card : MsoyServer.groupRepo.getMemberGroups(mrec.memberId, true)) {
                groups.put(card.name.getGroupId(), card.name);
            }

            // load up the thread records
            List<ForumThreadRecord> thrrecs;
            if (groups.size() == 0) {
                thrrecs = Collections.emptyList();
            } else {
                thrrecs = _forumRepo.loadUnreadThreads(mrec.memberId, groups.keySet(), maximum);
            }

            // load up additional fiddly bits and create a result record
            ThreadResult result = new ThreadResult();
            result.threads = ForumUtil.resolveThreads(mrec, thrrecs, groups, true, false);

            // we cheat on the total count here with the idea that the client basically just asks
            // for "all" of the unread threads and we give them all as long as all is not
            // ridiculously many
            result.threadCount = result.threads.size();

            return result;

        } catch (PersistenceException pe) {
            log.warning("Failed to load unread threads [for=" + who(mrec) +
                    ", max=" + maximum + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public ThreadResult loadThreads (
        WebIdent ident, int groupId, int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            // make sure they have read access to this group
            Group group = getGroup(groupId);
            byte groupRank = getGroupRank(mrec, groupId);
            if (!group.checkAccess(groupRank, Group.ACCESS_READ, 0)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            // load up the requested set of threads
            List<ForumThreadRecord> thrrecs = _forumRepo.loadThreads(groupId, offset, count);

            // load up additional fiddly bits and create a result record
            ThreadResult result = new ThreadResult();
            Map<Integer,GroupName> gmap = Collections.singletonMap(group.groupId, group.getName());
            result.threads = ForumUtil.resolveThreads(mrec, thrrecs, gmap, true, false);

            // fill in this caller's new thread starting privileges
            result.canStartThread = (mrec != null) &&
                group.checkAccess(groupRank, Group.ACCESS_THREAD, 0);

            // fill in our manager status
            result.isManager = (mrec != null && mrec.isSupport()) ||
                (groupRank == GroupMembership.RANK_MANAGER);

            // fill in our total thread count if needed
            if (needTotalCount) {
                result.threadCount = (result.threads.size() < count && offset == 0) ?
                    result.threads.size() : _forumRepo.loadThreadCount(groupId);
            }

            return result;

        } catch (PersistenceException pe) {
            log.warning("Failed to load threads [for=" + who(mrec) +
                    ", gid=" + groupId + ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public List<ForumThread> findThreads (WebIdent ident, int groupId, String search, int limit)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            // make sure they have read access to this group
            Group group = getGroup(groupId);
            if (!group.checkAccess(getGroupRank(mrec, groupId), Group.ACCESS_READ, 0)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            Map<Integer,GroupName> gmap = Collections.singletonMap(group.groupId, group.getName());
            return ForumUtil.resolveThreads(
                mrec, _forumRepo.findThreads(groupId, search, limit), gmap, true, false);

        } catch (PersistenceException pe) {
            log.warning("Failed to search threads [for=" + who(mrec) +
                    ", gid=" + groupId + ", search=" + search + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public MessageResult loadMessages (WebIdent ident, int threadId, int lastReadPostId,
                                       int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            // make sure they have read access to this thread
            ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            Group group = getGroup(ftr.groupId);
            byte rank = getGroupRank(mrec, ftr.groupId);
            if (!group.checkAccess(rank, Group.ACCESS_READ, 0)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            MessageResult result = new MessageResult();

            // fill in this caller's posting privileges and manager status
            result.canPostReply = (mrec != null) && group.checkAccess(rank, Group.ACCESS_POST, 0);
            result.isManager = (mrec != null && mrec.isSupport()) ||
                (rank == GroupMembership.RANK_MANAGER);

            // load up the messages, convert to runtime records, compute highest post id
            List<ForumMessage> messages = resolveMessages(
                _forumRepo.loadMessages(threadId, offset, count));
            int highestPostId = 0;
            for (ForumMessage msg : messages) {
                highestPostId = Math.max(highestPostId, msg.messageId);
            }
            result.messages = messages;

            if (needTotalCount) {
                // convert the thread record to a runtime record if needed
                result.thread = ftr.toForumThread(
                    Collections.singletonMap(ftr.mostRecentPosterId,
                                             // we don't need their display name here
                                             new MemberName("", ftr.mostRecentPosterId)),
                    Collections.singletonMap(group.groupId, group.getName()));

                // load up our last read post information
                if (mrec != null) {
                    for (ReadTrackingRecord rtr : _forumRepo.loadLastReadPostInfo(
                             mrec.memberId, Collections.singleton(threadId))) {
                        result.thread.lastReadPostId = rtr.lastReadPostId;
                        result.thread.lastReadPostIndex = rtr.lastReadPostIndex;
                        // since the client didn't have this info, we need to fill it in
                        lastReadPostId = rtr.lastReadPostId;
                    }
                }
            }

            // if this caller is authenticated, note that they've potentially updated their last
            // read message id
            if (mrec != null && highestPostId > lastReadPostId) {
                _forumRepo.noteLastReadPostId(              // highestPostIndex
                    mrec.memberId, threadId, highestPostId, offset + result.messages.size() - 1);
            }

            return result;

        } catch (PersistenceException pe) {
            log.warning("Failed to load messages [for=" + who(mrec) +
                    ", tid=" + threadId + ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public List<ForumMessage> findMessages (WebIdent ident, int threadId, String search, int limit)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.getAuthedUser(ident);

        try {
            // make sure they have read access to this thread
            ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            Group group = getGroup(ftr.groupId);
            byte rank = getGroupRank(mrec, ftr.groupId);
            if (!group.checkAccess(rank, Group.ACCESS_READ, 0)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            // do the search and return the results
            return resolveMessages(_forumRepo.findMessages(threadId, search, limit));

        } catch (PersistenceException pe) {
            log.warning("Failed to find messages [for=" + who(mrec) +
                    ", tid=" + threadId + ", search=" + search + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    protected List<ForumMessage> resolveMessages (List<ForumMessageRecord> msgrecs)
        throws PersistenceException
    {
        // enumerate the posters and create member cards for them
        IntMap<MemberCard> cards = IntMaps.newHashIntMap();
        IntSet posters = new ArrayIntSet();
        for (ForumMessageRecord msgrec : msgrecs) {
            posters.add(msgrec.posterId);
        }
        for (MemberCardRecord mcrec : MsoyServer.memberRepo.loadMemberCards(posters)) {
            cards.put(mcrec.memberId, mcrec.toMemberCard());
        }

        // convert the messages to runtime format
        List<ForumMessage> messages = Lists.newArrayList();
        for (ForumMessageRecord msgrec : msgrecs) {
            messages.add(msgrec.toForumMessage(cards));
        }
        return messages;
    }

    // from interface ForumService
    public ForumThread createThread (WebIdent ident, int groupId, int flags,
                                     String subject, String message)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            // make sure they're allowed to create a thread in this group
            Group group = checkAccess(mrec, groupId, Group.ACCESS_THREAD, flags);

            // sanitize and recheck the length of the message (note: we never display the subject
            // as raw HTML so we don't need to sanitize it)
            message = sanitizeMessage(message);

            // create the thread (and first post) in the database and return its runtime form
            ForumThread thread = _forumRepo.createThread(
                groupId, mrec.memberId, flags, subject, message).toForumThread(
                    Collections.singletonMap(mrec.memberId, mrec.getName()),
                    Collections.singletonMap(group.groupId, group.getName()));

            // mark this thread as read by the poster
            _forumRepo.noteLastReadPostId(
                mrec.memberId, thread.threadId, thread.mostRecentPostId, 1);

            // if we're posting to the announcement group, add a global feed post about it
            if (groupId == ServerConfig.getAnnounceGroupId()) {
                MsoyServer.feedRepo.publishGlobalMessage(
                    FeedMessageType.GLOBAL_ANNOUNCEMENT, subject + "\t" + thread.threadId);

            // otherwise, if the thread is an announcement thread, post a feed message about it
            } else if (thread.isAnnouncement()) {
                MsoyServer.feedRepo.publishGroupMessage(
                    groupId, FeedMessageType.GROUP_ANNOUNCEMENT,
                    group.name + "\t" + subject + "\t" + thread.threadId);
            }

            return thread;

        } catch (PersistenceException pe) {
            log.warning("Failed to create thread [for=" + who(mrec) +
                    ", gid=" + groupId + ", subject=" + subject + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public void updateThreadFlags (WebIdent ident, int threadId, int flags)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        try {
            ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }

            // make sure they have access to both the old and new flags
            Group group = getGroup(ftr.groupId);
            byte groupRank = getGroupRank(mrec, ftr.groupId);
            if (!group.checkAccess(groupRank, Group.ACCESS_POST, ftr.flags) ||
                !group.checkAccess(groupRank, Group.ACCESS_POST, flags)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            // if we made it this far, then update the flags
            _forumRepo.updateThreadFlags(ftr.threadId, flags);

        } catch (PersistenceException pe) {
            log.warning("Failed to update thread flags [for=" + who(mrec) +
                    ", tid=" + threadId + ", flags=" + flags + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public void ignoreThread (WebIdent ident, int threadId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);
        try {
            _forumRepo.noteLastReadPostId(mrec.memberId, threadId, Integer.MAX_VALUE, 0);

        } catch (PersistenceException pe) {
            log.warning("Failed to mark thread ignored [for=" + mrec.who() +
                    ", threadId=" + threadId + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public ForumMessage postMessage (WebIdent ident, int threadId, int inReplyTo, String message)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            // make sure they're allowed to post a message to this thread's group
            ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            checkAccess(mrec, ftr.groupId, Group.ACCESS_POST, ftr.flags);

            // make sure the user is not doing anything nefarious in their HTML
            message = sanitizeMessage(message);

            // create the message in the database and return its runtime form
            ForumMessageRecord fmr = _forumRepo.postMessage(
                threadId, mrec.memberId, inReplyTo, message);

            // load up the member card for the poster
            IntMap<MemberCard> cards = IntMaps.newHashIntMap();
            for (MemberCardRecord mcrec : MsoyServer.memberRepo.loadMemberCards(
                     Collections.singleton(mrec.memberId))) {
                cards.put(mcrec.memberId, mcrec.toMemberCard());
            }

            // mark this thread as read by the poster
            _forumRepo.noteLastReadPostId(mrec.memberId, ftr.threadId, fmr.messageId, ftr.posts+1);

            // and create and return the runtime record for the post
            return fmr.toForumMessage(cards);

        } catch (PersistenceException pe) {
            log.warning("Failed to post message [for=" + who(mrec) +
                    ", tid=" + threadId + ", irTo=" + inReplyTo + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public void editMessage (WebIdent ident, int messageId, String message)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            // make sure they are the message author
            ForumMessageRecord fmr = _forumRepo.loadMessage(messageId);
            if (fmr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
            }
            if (fmr.posterId != mrec.memberId) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            // make sure the user is not doing anything nefarious in their HTML
            message = sanitizeMessage(message);

            // if all is well then do the deed
            _forumRepo.updateMessage(messageId, message);

        } catch (PersistenceException pe) {
            log.warning("Failed to edit message [for=" + who(mrec) +
                    ", mid=" + messageId + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }

    }

    // from interface ForumService
    public void deleteMessage (WebIdent ident, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            // make sure they are the message author or a group admin or whirled support+
            ForumMessageRecord fmr = _forumRepo.loadMessage(messageId);
            if (fmr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
            }
            if (!mrec.isSupport() && fmr.posterId != mrec.memberId) {
                ForumThreadRecord ftr = _forumRepo.loadThread(fmr.threadId);
                if (ftr == null) {
                    throw new ServiceException(ForumCodes.E_INVALID_THREAD);
                }
                if (getGroupRank(mrec, ftr.groupId) != GroupMembership.RANK_MANAGER) {
                    throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
                }
            }

            // if all is well then do the deed
            _forumRepo.deleteMessage(messageId);

        } catch (PersistenceException pe) {
            log.warning("Failed to delete message [for=" + who(mrec) +
                    ", mid=" + messageId + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public void complainMessage (WebIdent ident, String complaint, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = _mhelper.requireAuthedUser(ident);

        try {
            // load up the message details
            ForumMessageRecord fmr = _forumRepo.loadMessage(messageId);
            if (fmr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
            }
            MsoyServer.supportMan.addMessageComplaint(
                    mrec.getName(), fmr.posterId, fmr.message, complaint,
                    ServerConfig.getServerURL() + "/#whirleds-t_" + fmr.threadId);
        } catch (PersistenceException pe) {
            log.warning("Failed to complain message [for=" + who(mrec) +
                    ", mid=" + messageId + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Loads the specified group, throwing an invalid group exception if it does not exist.
     */
    protected Group getGroup (int groupId)
        throws PersistenceException, ServiceException
    {
        GroupRecord grec = MsoyServer.groupRepo.loadGroup(groupId);
        if (grec == null) {
            throw new ServiceException(ForumCodes.E_INVALID_GROUP);
        }
        return grec.toGroupObject();
    }

    /**
     * Checks that the supplied member has the specified access in the specified group.
     */
    protected Group checkAccess (MemberRecord mrec, int groupId, int access, int flags)
        throws PersistenceException, ServiceException
    {
        Group group = getGroup(groupId);
        if (!group.checkAccess(getGroupRank(mrec, groupId), access, flags)) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }
        return group;
    }

    /**
     * Determines the rank of the supplied member in the specified group. If the member is null or
     * not a member of the specified group, rank-non-member will be returned.
     */
    protected byte getGroupRank (MemberRecord mrec, int groupId)
        throws PersistenceException
    {
        byte rank = GroupMembership.RANK_NON_MEMBER;
        if (mrec != null) {
            if (mrec.isAdmin()) { // admins are always treated as managers
                return GroupMembership.RANK_MANAGER;
            }
            GroupMembershipRecord grm = MsoyServer.groupRepo.getMembership(groupId, mrec.memberId);
            if (grm != null) {
                rank = grm.rank;
            }
        }
        return rank;
    }
    
    /**
     * Runs the supplied HTML message through our sanitizer and rechecks that the length of the
     * message is within our limits. The sanitizer might make the message slightly longer.
     */
    protected String sanitizeMessage (String message)
        throws ServiceException
    {
        if (message.length() > HTMLSanitizer.MAX_PRE_SANITIZE_LENGTH) {
            throw new MessageTooLongException(message.length());
        }
        String sanitized = HTMLSanitizer.sanitize(message);
        if (sanitized.length() > ForumMessage.MAX_MESSAGE_LENGTH) {
            throw new MessageTooLongException(sanitized.length());
        }
        return sanitized;
    }

    protected ForumRepository _forumRepo = MsoyServer.forumRepo;
}
