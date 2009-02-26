//
// $Id$

package com.threerings.msoy.mail.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.annotation.MainInvoker;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.JSONMarshaller;
import com.threerings.msoy.server.util.MailSender;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.mail.gwt.FriendInvitePayload;
import com.threerings.msoy.mail.gwt.MailPayload;
import com.threerings.msoy.mail.gwt.PresentPayload;
import com.threerings.msoy.mail.server.persist.ConvMessageRecord;
import com.threerings.msoy.mail.server.persist.ConversationRecord;
import com.threerings.msoy.mail.server.persist.MailRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides mail related services to servlets and other blocking thread entities.
 */
@Singleton @BlockingThread
public class MailLogic
{
    /**
     * Sends a friend invitation email from the supplied inviter to the specified member.
     */
    public void sendFriendInvite (int inviterId, int friendId)
        throws ServiceException
    {
        MemberRecord sender = _memberRepo.loadMember(inviterId);
        MemberRecord recip = _memberRepo.loadMember(friendId);
        if (sender == null || recip == null) {
            log.warning("Missing records for friend invite [iid=" + inviterId +
                        ", tid=" + friendId + ", irec=" + sender + ", trec=" + recip + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        String subj = _serverMsgs.getBundle("server").get("m.friend_invite_subject");
        String body = _serverMsgs.getBundle("server").get("m.friend_invite_body");
        startConversation(sender, recip, subj, body, new FriendInvitePayload());
    }

    /**
     * Starts a mail conversation between the specified two parties without filling up the author's
     * inbox.
     */
    public void startBulkConversation (MemberRecord sender, MemberRecord recip, String subject,
                                       String body, MailPayload payload)
        throws ServiceException
    {
        // now start the conversation (and deliver the message)
        _mailRepo.startConversation(
            recip.memberId, sender.memberId, subject, body, payload, false);

        // potentially send a real email to the recipient
        sendMailEmail(sender, recip, subject, body);

        // let recipient know they've got mail
        MemberNodeActions.reportUnreadMail(
            recip.memberId, _mailRepo.loadUnreadConvoCount(recip.memberId));
    }

    /**
     * Starts a mail conversation between the specified two parties.
     */
    public void startConversation (MemberRecord sender, MemberRecord recip,
                                   String subject, String body, MailPayload attachment)
        throws ServiceException
    {
        // if the payload is an item attachment, transfer it to the recipient
        processPayload(sender.memberId, recip.memberId, attachment);

        // now start the conversation (and deliver the message)
        _mailRepo.startConversation(
            recip.memberId, sender.memberId, subject, body, attachment, true);

        // potentially send a real email to the recipient
        sendMailEmail(sender, recip, subject, body);

        // let recipient know they've got mail
        MemberNodeActions.reportUnreadMail(
            recip.memberId, _mailRepo.loadUnreadConvoCount(recip.memberId));
    }

    /**
     * Continues the specified mail conversation.
     */
    public ConvMessageRecord continueConversation (MemberRecord poster, int convoId, String body,
                                                   MailPayload attachment)
        throws ServiceException
    {
        ConversationRecord conrec = _mailRepo.loadConversation(convoId);
        if (conrec == null) {
            log.warning("Requested to continue non-existent conversation [by=" + poster.who() +
                        ", convoId=" + convoId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // make sure this member is a conversation participant
        Long lastRead = _mailRepo.loadLastRead(convoId, poster.memberId);
        if (lastRead == null) {
            log.warning("Request to continue conversation by non-member [who=" + poster.who() +
                        ", convoId=" + convoId + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // TODO: make sure body.length() is not too long

        // encode the attachment if we have one
        int payloadType = 0;
        byte[] payloadState = null;
        if (attachment != null) {
            payloadType = attachment.getType();
            try {
                payloadState = JSONMarshaller.getMarshaller(
                    attachment.getClass()).getStateBytes(attachment);
            } catch (Exception e) {
                log.warning("Failed to encode message attachment [for=" + poster.who() +
                            ", attachment=" + attachment + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
        }

        // if the payload is an item attachment, transfer it to the recipient
        processPayload(poster.memberId, conrec.getOtherId(poster.memberId), attachment);

        // store the message in the repository
        ConvMessageRecord cmr =
            _mailRepo.addMessage(conrec, poster.memberId, body, payloadType, payloadState);

        // update our last read for this conversation to reflect that we've read our message
        _mailRepo.updateLastRead(convoId, poster.memberId, cmr.sent.getTime());

        // let other conversation participant know they've got mail
        int otherId = conrec.getOtherId(poster.memberId);
        MemberNodeActions.reportUnreadMail(otherId, _mailRepo.loadUnreadConvoCount(otherId));

        // potentially send a real email to the recipient
        MemberRecord recip = _memberRepo.loadMember(otherId);
        if (recip != null) {
            String subject = _serverMsgs.getBundle("server").get("m.reply_subject", conrec.subject);
            sendMailEmail(poster, recip, subject, body);
        }

        return cmr;
    }

    /**
     * Sends email to all players who have not opted out of Whirled announcements.
     */
    public void spamPlayers (String subject, String body)
    {
        // TODO: if we want to continue to use this mechanism to send mass emails to our members,
        // we will need to farm out the mail deliver task to all nodes in the network so that we
        // don't task one node with sending out a million email messages

        // convert the body into proper-ish HTML
        body = SpamUtil.formatSpam(body);
        if (body == null) {
            return;
        }

        // load up the emails of everyone we want to spam
        List<Tuple<Integer, String>> emails = _memberRepo.loadMemberEmailsForAnnouncement();

        // add the Return Path probe addresses (as long as we're not on dev)
        if (!DeploymentConfig.devDeployment) {
            for (String rpaddr : SpamUtil.getReturnPathAddrs()) {
                emails.add(Tuple.newTuple(0, rpaddr));
            }
        }

        // ship our giant list off to the mail sender
        _mailer.sendSpam(emails, ServerConfig.getFromAddress(),
                         SpamUtil.makeSpamHeaders(subject), subject, body);

        log.info("Queued up announcement email", "subject", subject, "count", emails.size());
    }

    /**
     * Sends a spam preview mailing to the specified address.
     */
    public void previewSpam (int recipId, String recip, String subject, String body,
                             boolean includeProbeList)
    {
        // convert the body into proper-ish HTML
        body = SpamUtil.formatSpam(body);
        if (body == null) {
            return;
        }

        // add the caller to the recipient list
        List<Tuple<Integer, String>> emails = Lists.newArrayList();
        emails.add(Tuple.newTuple(recipId, recip));

        // add the Return Path probe addresses if they were requested
        if (includeProbeList) {
            for (String rpaddr : SpamUtil.getReturnPathAddrs()) {
                emails.add(Tuple.newTuple(0, rpaddr));
            }
        }

        // ship all this off to the mail sender
        _mailer.sendSpam(emails, ServerConfig.getFromAddress(),
                         SpamUtil.makeSpamHeaders(subject), subject, body);
    }

    /**
     * Handles any side-effects of mail payload delivery. Currently that is only the transfer of an
     * item from the sender to the recipient for {@link PresentPayload}.
     */
    protected void processPayload (final int senderId, final int recipId, MailPayload attachment)
        throws ServiceException
    {
        if (attachment instanceof PresentPayload) {
            ItemIdent ident = ((PresentPayload)attachment).ident;
            ItemRepository<?> repo = _itemLogic.getRepository(ident.type);
            final ItemRecord item = repo.loadItem(ident.itemId);

            // validate that they're allowed to gift this item (these are all also checked on the
            // client so we don't need useful error messages)
            String errmsg = null;
            if (item == null) {
                errmsg = "Trying to gift non-existent item";
            } else if (item.ownerId != senderId) {
                errmsg = "Trying to gift un-owned item";
            } else if (item.used != Item.UNUSED) {
                errmsg = "Trying to gift in-use item";
            }
            if (errmsg != null) {
                log.warning(errmsg + " [sender=" + senderId + ", recip=" + recipId +
                            ", ident=" + ident + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }

            final ItemRecord oitem = (ItemRecord)item.clone();
            repo.updateOwnerId(item, recipId);

            // notify the item system that the item has moved
            _itemLogic.itemUpdated(oitem, item);
        }
    }

    /**
     * Send an email to a Whirled mail recipient to report that they received a Whirled mail. Does
     * nothing if the recipient has requested not to receive such mails.
     */
    protected void sendMailEmail (MemberRecord sender, MemberRecord recip,
                                  String subject, String body)
    {
        // if they don't want to hear about it, stop now
        if (recip.isSet(MemberRecord.Flag.NO_WHIRLED_MAIL_TO_EMAIL)) {
            return;
        }
        _mailer.sendTemplateEmail(
            recip.accountName, ServerConfig.getFromAddress(), "gotMail",
            "subject", subject,"sender", sender.name, "senderId", sender.memberId,
            "body", body, "server_url", ServerConfig.getServerURL());
    }

    @Inject protected RootDObjectManager _omgr;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MailSender _mailer;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MailRepository _mailRepo;
    @Inject protected MemberRepository _memberRepo;

    protected static final int MEMBERS_PER_LOOP = 100;
}
