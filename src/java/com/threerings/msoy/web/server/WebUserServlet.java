//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Calendar;
import java.util.Date;

import java.util.logging.Level;

import org.apache.velocity.VelocityContext;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.net.MailUtil;
import com.samskivert.util.ResultListener;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;
import com.threerings.msoy.web.data.WebIdent;
import com.threerings.msoy.web.data.Invitation;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link WebUserService}.
 */
public class WebUserServlet extends MsoyServiceServlet
    implements WebUserService
{
    // from interface WebUserService
    public WebCreds login (long clientVersion, String username, String password, int expireDays)
        throws ServiceException
    {
        checkClientVersion(clientVersion, username);
        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
        return startSession(auth.authenticateSession(username, password), expireDays);
    }

    // from interface WebUserService
    public WebCreds register (long clientVersion, String username, String password, 
                              final String displayName, Date birthday, int expireDays, 
                              final Invitation invite)
        throws ServiceException
    {
        checkClientVersion(clientVersion, username);

        // check age restriction
        Calendar thirteenYearsAgo = Calendar.getInstance();
        thirteenYearsAgo.add(Calendar.YEAR, -13);
        if (birthday.compareTo(thirteenYearsAgo.getTime()) > 0) {
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // check invitation validity
        boolean ignoreRestrict = false;
        if (invite != null) {
            try {
                if (MsoyServer.memberRepo.inviteAvailable(invite.inviteId)) {
                    ignoreRestrict = true;
                } else {
                    // this is likely due to an attempt to gain access through a trying random
                    // invites.
                    throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
                }
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "Checking invite availability failed [inviteId=" +
                        invite.inviteId + "]", pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }
        }

        // we are running on a servlet thread at this point and can thus talk to the authenticator
        // directly as it is thread safe (and it blocks) and we are allowed to block
        MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
        final MemberRecord newAccount = auth.createAccount(username, password, displayName, 
            ignoreRestrict, invite != null ? invite.inviter.getMemberId() : 0);
        try {
            MsoyServer.profileRepo.setBirthday(newAccount.memberId, birthday);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "failed to set birthday on new account's profile [memberId=" +
                newAccount.memberId + ", birthday=" + birthday + "]", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }

        // if we were invited by another player, wire that all up
        if (invite != null) {
            try {
                MsoyServer.memberRepo.linkInvite(invite, newAccount);
            } catch (PersistenceException pe) {
                log.log(Level.WARNING, "linking invites failed [inviteId=" + invite.inviteId + 
                        ", memberId=" + newAccount.memberId + "]", pe);
                throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
            }

            // send a notification email that the friend has accepted his invite
            MsoyServer.omgr.postRunnable(new Runnable() {
                public void run () {
                    // TODO: this should be a custom mail message type (perhaps just one that
                    // displays a translatable string from the server)
                    String body = "The invitation that you sent to " + invite.inviteeEmail +
                        " has been accepted.  Your friend has chosen the display name \"" +
                        displayName + "\", and has been added to your friend's list.";
                    MsoyServer.mailMan.deliverMessage(
                        newAccount.memberId, invite.inviter.getMemberId(), "Invitation Accepted!",
                        body, null, new ResultListener.NOOP<Void>());
                }
            });
        }

        return startSession(newAccount, expireDays);
    }

    // from interface WebUserService
    public WebCreds validateSession (long clientVersion, String authtok, int expireDays)
        throws ServiceException
    {
        checkClientVersion(clientVersion, authtok);

        // refresh the token associated with their authentication session
        try {
            MemberRecord mrec = MsoyServer.memberRepo.refreshSession(authtok, expireDays);
            if (mrec == null) {
                return null;
            }

            WebCreds creds = mrec.toCreds(authtok);
            mapUser(creds, mrec);
            return creds;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to refresh session [tok=" + authtok + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    // from interface WebUserService
    public void sendForgotPasswordEmail (String email)
        throws ServiceException
    {
        try {
            MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
            String code = auth.generatePasswordResetCode(email);
            if (code == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }

            MemberRecord mrec = MsoyServer.memberRepo.loadMember(email);
            if (mrec == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }

            // create and send a forgot password email
            VelocityContext ctx = new VelocityContext();
            ctx.put("server_url", ServerConfig.getServerURL());
            ctx.put("email", mrec.accountName);
            ctx.put("memberId", mrec.memberId);
            ctx.put("code", code);
            try {
                sendEmail(email, ServerConfig.getFromAddress(), "forgotPassword", ctx);
            } catch (Exception e) {
                throw new ServiceException(e.getMessage());
            }

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to lookup account [email=" + email + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    // from interface WebUserService
    public void updateEmail (WebIdent ident, String newEmail)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        if (!MailUtil.isValidAddress(newEmail)) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        }

        try {
            MsoyServer.memberRepo.configureAccountName(mrec.memberId, newEmail);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to set email [who=" + mrec.memberId +
                    ", email=" + newEmail + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // let the authenticator know that we updated our account name
        MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
        auth.updateAccount(mrec.accountName, newEmail, null, null);
    }

    // from interface WebUserService
    public void updatePassword (WebIdent ident, String newPassword)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
        auth.updateAccount(mrec.accountName, null, null, newPassword);
    }

    // from interface WebUserService
    public boolean resetPassword (int memberId, String code, String newPassword)
        throws ServiceException
    {
        try {
            MemberRecord mrec = MsoyServer.memberRepo.loadMember(memberId);
            if (mrec == null) {
                log.info("No such member for password reset " + memberId + ".");
                return false;
            }

            MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
            if (!auth.validatePasswordResetCode(mrec.accountName, code)) {
                log.info("Code mismatch for password reset [id=" + memberId + ", code=" + code +
                         ", actual=" + auth.generatePasswordResetCode(mrec.accountName) + "].");
                return false;
            }

            auth.updateAccount(mrec.accountName, null, null, newPassword);
            return true;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to reset password [who=" + memberId +
                    ", code=" + code + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }
    }

    // from interface WebUserService
    public void configurePermaName (WebIdent ident, String permaName)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);
        if (mrec.permaName != null) {
            log.warning("Rejecting attempt to reassing permaname [who=" + mrec.accountName +
                        ", oname=" + mrec.permaName + ", nname=" + permaName + "].");
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        if (permaName.length() < MemberName.MINIMUM_PERMANAME_LENGTH ||
            permaName.length() > MemberName.MAXIMUM_PERMANAME_LENGTH ||
            !permaName.matches(PERMANAME_REGEX)) {
            throw new ServiceException("e.invalid_permaname");
        }

        try {
            MsoyServer.memberRepo.configurePermaName(mrec.memberId, permaName);
        } catch (DuplicateKeyException dke) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_PERMANAME);
        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to set permaname [who=" + mrec.memberId +
                    ", pname=" + permaName + "].", pe);
            throw new ServiceException(ServiceException.INTERNAL_ERROR);
        }

        // let the authenticator know that we updated our permaname
        MsoyAuthenticator auth = (MsoyAuthenticator)MsoyServer.conmgr.getAuthenticator();
        auth.updateAccount(mrec.accountName, null, permaName, null);
    }

    protected void checkClientVersion (long clientVersion, String who)
        throws ServiceException
    {
        if (clientVersion != DeploymentConfig.version) {
            log.info("Refusing wrong version [who=" + who + ", cvers=" + clientVersion +
                     ", svers=" + DeploymentConfig.version + "].");
            throw new ServiceException(MsoyAuthCodes.VERSION_MISMATCH);
        }
    }

    protected WebCreds startSession (MemberRecord mrec, int expireDays)
        throws ServiceException
    {
        try {
            // if they made it through that gauntlet, create or update their session token
            WebCreds creds = mrec.toCreds(
                MsoyServer.memberRepo.startOrJoinSession(mrec.memberId, expireDays));
            mapUser(creds, mrec);
            return creds;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to start session [for=" + mrec.accountName + "].", pe);
            throw new ServiceException(MsoyAuthCodes.SERVER_UNAVAILABLE);
        }
    }

    /** The regular expression defining valid permanames. */
    protected static final String PERMANAME_REGEX = "^[A-Za-z][_A-Za-z0-9]*$";
}
