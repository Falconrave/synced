//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;
import java.util.Set;

import octazen.addressbook.AddressBookAuthenticationException;
import octazen.addressbook.AddressBookException;
import octazen.addressbook.Contact;
import octazen.addressbook.SimpleAddressBookImporter;
import octazen.addressbook.UnexpectedFormatException;
import octazen.http.UserInputRequiredException;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.net.MailUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.server.persist.GameDetailRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.server.persist.GameInvitationRecord;
import com.threerings.msoy.server.persist.InvitationRecord;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.mail.gwt.GameInvitePayload;
import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.person.gwt.InvitationResults;
import com.threerings.msoy.person.gwt.MemberInvites;
import com.threerings.msoy.person.gwt.ProfileCodes;
import com.threerings.msoy.person.gwt.InviteService;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link InviteService}.
 */
public class InviteServlet extends MsoyServiceServlet
    implements InviteService
{
    // from InviteService
    public List<EmailContact> getWebMailAddresses (
        String email, String password, boolean skipFriends)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();

        try {
            // don't let someone attempt more than 5 imports in a 5 minute period
            long now = System.currentTimeMillis();
            if (now > _webmailCleared + WEB_ACCESS_CLEAR_INTERVAL) {
                _webmailAccess.clear();
                _webmailCleared = now;
            }
            if (_webmailAccess.increment(memrec.memberId, 1) > MAX_WEB_ACCESS_ATTEMPTS) {
                throw new ServiceException(ProfileCodes.E_MAX_WEBMAIL_ATTEMPTS);
            }
            List<Contact> contacts = SimpleAddressBookImporter.fetchContacts(email, password);
            List<EmailContact> results = Lists.newArrayList();

            for (Contact contact : contacts) {
                // don't invite the account owner
                if (email.equals(contact.getEmail())) {
                    continue;
                }
                EmailContact ec = new EmailContact();
                ec.name = contact.getName();
                ec.email = contact.getEmail();
                MemberRecord member = _memberRepo.loadMember(ec.email);
                if (member != null) {
                    if (member.memberId == memrec.memberId) {
                        // skip self invites
                        continue;
                    }
                    ec.friend = _memberRepo.getFriendStatus(memrec.memberId, member.memberId);
                    if (skipFriends && ec.friend) {
                        // skip friends if requested
                        continue;
                    }
                    ec.mname = member.getName();
                }
                results.add(ec);
            }

            return results;

        } catch (AddressBookAuthenticationException e) {
            throw new ServiceException(ProfileCodes.E_BAD_USERNAME_PASS);
        } catch (UnexpectedFormatException e) {
            log.warning("getWebMailAddresses failed [email=" + email + "].", e);
            throw new ServiceException(ProfileCodes.E_INTERNAL_ERROR);
        } catch (AddressBookException e) {
            throw new ServiceException(ProfileCodes.E_UNSUPPORTED_WEBMAIL);
        } catch (UserInputRequiredException e) {
            throw new ServiceException(ProfileCodes.E_USER_INPUT_REQUIRED);
        } catch (Exception e) {
            log.warning("getWebMailAddresses failed", "who", memrec.who(), "email", email, e);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from InviteService
    public MemberInvites getInvitationsStatus ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        MemberInvites result = new MemberInvites();
        result.availableInvitations = _memberRepo.getInvitesGranted(mrec.memberId);
        List<Invitation> pending = Lists.newArrayList();
        for (InvitationRecord iRec : _memberRepo.loadPendingInvites(mrec.memberId)) {
            // we issued these invites so we are the inviter
            pending.add(iRec.toInvitation(mrec.getName()));
        }
        result.pendingInvitations = pending;
        result.serverUrl = ServerConfig.getServerURL() + "#invite-";
        return result;
    }

    // from InviteService
    public InvitationResults sendInvites (List<EmailContact> addresses, String fromName,
                                          String customMessage, boolean anonymous)
        throws ServiceException
    {
        MemberRecord mrec = anonymous ? requireAdminUser() : requireAuthedUser();

// TODO: nix this when we stop caring about retaining the potential to limit growth
//             // make sure this user still has available invites; we already check this value in GWT
//             // land, and deal with it sensibly there
//             int availInvites = _memberRepo.getInvitesGranted(mrec.memberId);
//             if (availInvites < addresses.size()) {
//                 log.warning("Member requested to grant more invites than they have " +
//                             "[who=" + mrec.who() + ", tried=" + addresses.size() +
//                             ", have=" + availInvites + "].");
//                 throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//             }

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        ir.names = new MemberName[addresses.size()];
        List<Invitation> penders = Lists.newArrayList();
        for (int ii = 0; ii < addresses.size(); ii++) {
            EmailContact contact = addresses.get(ii);
            if (contact.name.equals(contact.email)) {
                contact.name = null;
            }
            try {
                penders.add(sendInvite(anonymous ? null : mrec, contact.email, contact.name,
                            fromName, customMessage));
            } catch (NameServiceException nse) {
                ir.results[ii] = nse.getMessage();
                ir.names[ii] = nse.name;
            } catch (ServiceException se) {
                ir.results[ii] = se.getMessage();
            }
        }
        ir.pendingInvitations = penders;
        return ir;
    }

    public InvitationResults sendGameInvites (
        List<EmailContact> addresses, int gameId, String from, String url, String customMessage)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        boolean isDevVersion = Game.isDevelopmentVersion(gameId);
        if (isDevVersion) {
            // TODO: this is not really an end-user exception and could arguably be done in the
            // client. At least let the creator know something is wrong for now
            throw new ServiceException("e.game_not_listed");
        }

        GameDetailRecord gdr = _mgameRepo.loadGameDetail(gameId);
        if (gdr == null) {
            throw new ServiceException(MsoyCodes.INTERNAL_ERROR);
        }

        if (gdr.listedItemId == 0) {
            throw new ServiceException("e.game_not_listed"); // TODO
        }

        String gameName = _gameRepo.loadItem(gdr.listedItemId).name;

        InvitationResults ir = new InvitationResults();
        ir.results = new String[addresses.size()];
        ir.names = new MemberName[addresses.size()];
        for (int ii = 0; ii < addresses.size(); ii++) {
            EmailContact contact = addresses.get(ii);
            if (contact.name.equals(contact.email)) {
                contact.name = null;
            }
            try {
                sendGameInvite(gameName, gameId, mrec, contact.email, contact.name, from,
                    url, customMessage);

            } catch (ServiceException se) {
                ir.results[ii] = se.getMessage();
            }
        }
        return ir;
    }

    public void sendWhirledMailGameInvites (Set<Integer> recipientIds, int gameId, String subject,
        String body, String args)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        GameInvitePayload payload = new GameInvitePayload(args);
        for (int memberId : recipientIds) {
            MemberRecord recip = _memberRepo.loadMember(memberId);
            _mailLogic.startBulkConversation(mrec, recip, subject, body, payload);
            _eventLog.whirledMailGameInviteSent(gameId, mrec.memberId, recip.memberId);
        }
    }

    // from InviteService
    public void removeInvitation (String inviteId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        InvitationRecord invRec = _memberRepo.loadInvite(inviteId, false);
        if (invRec == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        if (invRec.inviterId != mrec.memberId || invRec.inviteeId != 0) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        _memberRepo.deleteInvite(inviteId);
    }

    public int getHomeSceneId ()
        throws ServiceException
    {
        return requireAuthedUser().homeSceneId;
    }

    public List<MemberCard> getFriends (int count)
        throws ServiceException
    {
        MemberRecord memrec = requireAuthedUser();
        IntSet friendsIds = _memberRepo.loadFriendIds(memrec.memberId);
        List<MemberCard> cards = Lists.newArrayList();
        // fill in with dupes up to the requested count in dev deployments for testing
        // TODO: remove duping
        int passes = DeploymentConfig.devDeployment ? 20 : 1;
        for (int ii = 0; ii < passes && cards.size() < count; ++ii) {
            for (MemberCardRecord mcr : _memberRepo.loadMemberCards(
                friendsIds, 0, count - cards.size(), true)) {
                cards.add(mcr.toMemberCard());
            }
        }
        return cards;
    }

    /**
     * Helper function for {@link #sendInvites}.
     */
    protected Invitation sendInvite (MemberRecord inviter, String email, String toName,
                                     String fromName, String customMessage)
        throws ServiceException
    {
        // make sure this address is valid
        if (!MailUtil.isValidAddress(email)) {
            throw new ServiceException(InvitationResults.INVALID_EMAIL);
        }

        // make sure this address isn't already registered
        MemberRecord invitee = _memberRepo.loadMember(email);
        if (invitee != null) {
            if (_memberRepo.getFriendStatus(inviter.memberId, invitee.memberId)) {
                throw new ServiceException(InvitationResults.ALREADY_FRIEND);
            }
            throw new NameServiceException(
                InvitationResults.ALREADY_REGISTERED, invitee.getName());
        }

        // make sure this address isn't on the opt-out list
        if (_memberRepo.hasOptedOut(email)) {
            throw new ServiceException(InvitationResults.OPTED_OUT);
        }

        // make sure this user hasn't already invited this address
        int inviterId = (inviter == null) ? 0 : inviter.memberId;
        if (_memberRepo.loadInvite(email, inviterId) != null) {
            throw new ServiceException(InvitationResults.ALREADY_INVITED);
        }

        String inviteId = _memberRepo.generateInviteId();

        // create and send the invitation email
        MailSender.Parameters params = new MailSender.Parameters();
        if (inviter != null) {
            params.set("friend", fromName);
            params.set("email", inviter.accountName);
        }
        if (!StringUtil.isBlank(toName)) {
            params.set("name", toName);
        }
        if (!StringUtil.isBlank(customMessage)) {
            params.set("custom_message", customMessage);
        }
        params.set("invite_id", inviteId);
        params.set("server_url", ServerConfig.getServerURL());

        String from = (inviter == null) ? ServerConfig.getFromAddress() : inviter.accountName;
        _mailer.sendTemplateEmail(email, from, "memberInvite", params);

        // record the invite and that we sent it
        _memberRepo.addInvite(email, inviterId, inviteId);
        _eventLog.inviteSent(inviteId, inviterId, email);

        Invitation invite = new Invitation();
        invite.inviteId = inviteId;
        invite.inviteeEmail = email;
        // invite.inviter left blank on purpose
        return invite;
    }

    /**
     * Helper function for {@link #sendGameInvites}.
     */
    protected void sendGameInvite (String gameName, int gameId, MemberRecord inviter, String email,
        String toName, String fromName, String url, String customMessage)
        throws ServiceException
    {
        // make sure this address is valid
        if (!MailUtil.isValidAddress(email)) {
            throw new ServiceException(InvitationResults.INVALID_EMAIL);
        }

        // TODO: if a user is trying to invite another registered user, we should just send a
        // whirled mail message instead
        MemberRecord invitee = _memberRepo.loadMember(email);
        if (_memberRepo.loadMember(email) != null) {
            throw new NameServiceException(
                InvitationResults.ALREADY_REGISTERED, invitee.getName());
        }

        // make sure this address isn't on the opt-out list
        if (_memberRepo.hasOptedOut(email)) {
            throw new ServiceException(InvitationResults.OPTED_OUT);
        }

        // we are fine to send multiple game invites, but we must provide an invite id so the
        // recipient can opt out securely
        String inviteId;
        GameInvitationRecord invite = _memberRepo.loadGameInviteByEmail(email);
        if (invite != null) {
            inviteId = invite.inviteId;
        } else {
            inviteId = _memberRepo.generateGameInviteId();
        }

        // create and send the invitation email
        MailSender.Parameters params = new MailSender.Parameters();
        if (inviter != null) {
            params.set("friend", fromName);
            params.set("email", inviter.accountName);
        }
        if (!StringUtil.isBlank(toName)) {
            params.set("name", toName);
        }
        if (!StringUtil.isBlank(customMessage)) {
            params.set("custom_message", customMessage);
        }
        params.set("server_url", ServerConfig.getServerURL());
        params.set("url", url);
        params.set("game", gameName);
        params.set("invite_id", inviteId);

        String from = inviter.accountName;
        _mailer.sendTemplateEmail(email, from, "gameInvite", params);

        // record the invite and that we sent it
        _memberRepo.addGameInvite(email, inviteId);
        _eventLog.gameInviteSent(gameId, inviter.memberId, email);
    }

    protected class NameServiceException extends ServiceException
    {
        public MemberName name;

        public NameServiceException (String message, MemberName name)
        {
            super(message);
            this.name = name;
        }
    }

    protected IntIntMap _webmailAccess = new IntIntMap();
    protected long _webmailCleared = System.currentTimeMillis();

    @Inject protected MailSender _mailer;
    @Inject protected MsoyGameRepository _mgameRepo;
    @Inject protected GameRepository _gameRepo;
    @Inject protected MailLogic _mailLogic;

    protected static final int MAX_WEB_ACCESS_ATTEMPTS = 5;
    protected static final long WEB_ACCESS_CLEAR_INTERVAL = 5L * 60 * 1000;
}
