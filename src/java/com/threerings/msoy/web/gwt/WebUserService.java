//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.data.all.LaunchConfig;
import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Defines general user services available to the GWT/AJAX web client.
 */
public interface WebUserService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/usersvc";

    /** The default length of a session. */
    public static final int SESSION_DAYS = 3;

    /** Used by {@link #register}. */
    public static class RegisterData extends SessionData
    {
        /** The entry vector associated with this client. */
        public String entryVector;
    }

    /**
     * Requests that the client be logged on as the specified user with the supplied (MD5-encoded)
     * password.
     *
     * @return a set of credentials including a session cookie that should be provided to
     * subsequent remote service calls that require authentication.
     */
    SessionData logon (String clientVersion, String email, String password, int expireDays)
        throws ServiceException;

    /**
     * Requests that the client be logged on to the account associated with the supplied external
     * credentials.
     */
    SessionData externalLogon (String clientVersion, ExternalCreds creds, VisitorInfo vinfo,
                               int expireDays)
        throws ServiceException;

    /**
     * Requests that an account be created for the specified user. The user will be logged in after
     * the account is created.
     * @param forceValidation experimental flag to send a validation email instead of a welcome
     * email. The link in the email will then take the user on the the next step of registration.
     * @return a set of credentials including a session cookie that should be provided to
     * subsequent remote service calls that require authentication.
     */
    RegisterData register (String clientVersion, RegisterInfo info, boolean forceValidation)
        throws ServiceException;

    /**
     * Validates that the supplied session token is still active and refreshes its expiration time
     * if so.
     */
    SessionData validateSession (String clientVersion, String authtok, int expireDays)
        throws ServiceException;

    /**
     * Links the requesting user's account to the supplied external authentication source account.
     *
     * @return true if the account was linked, false if the account was not linked because the
     * specified external id is already linked with another Whirled account.
     */
    boolean linkExternalAccount (ExternalCreds creds, boolean override)
        throws ServiceException;

    /**
     * Returns the connection information for this server's World services.
     */
    ConnectConfig getConnectConfig ()
        throws ServiceException;

    /**
     * Loads the configuration needed to play (launch) the specified game.
     */
    LaunchConfig loadLaunchConfig (int gameId)
        throws ServiceException;

    /**
     * Sends a "forgot my password" email to the account registered with the supplied address.
     */
    void sendForgotPasswordEmail (String email)
        throws ServiceException;

    /**
     * Updates the email address on file for this account.
     */
    void updateEmail (String newEmail)
        throws ServiceException;

    /**
     * Updates the email preferences for this account.
     */
    void updateEmailPrefs (boolean emailOnWhirledMail, boolean emailAnnouncements)
        throws ServiceException;

    /**
     * Updates the password on file for this account.
     */
    void updatePassword (String newPassword)
        throws ServiceException;

    /**
     * Resets the password on file for the specified account to the new value.
     */
    boolean resetPassword (int memberId, String code, String newPassword)
        throws ServiceException;

    /**
     * Configures the permaname for this account.
     */
    void configurePermaName (String permaName)
        throws ServiceException;

    /**
     * fetches the user's account info.
     */
    AccountInfo getAccountInfo ()
        throws ServiceException;

    /**
     * Updates the user's account info to match the AccountInfo object.
     */
    void updateAccountInfo (AccountInfo info)
        throws ServiceException;

    /**
     * Sets the user's charity to the given member ID.
     */
    void updateCharity (int selectedCharityId)
        throws ServiceException;

    /**
     * Requests to resend the validation email for the calling account.
     */
    void resendValidationEmail ()
        throws ServiceException;

    /**
     * Marks the specified account valid if the supplied validation code checks out as valid.
     *
     * @return true if the code is valid, false if not.
     */
    boolean validateEmail (int memberId, String code)
        throws ServiceException;

    /**
     * Requests to delete the calling user's account. This will email a link to the user's account,
     * which when followed will present the user with warnings and require their password.
     */
    void requestAccountDeletion ()
        throws ServiceException;

    /**
     * Requests to delete the calling user's account. The supplied MD5-encoded password must match
     * the one on file for the account. The supplied code must match the one sent via email using
     * {@link #requestAccountDeletion()}.
     */
    void deleteAccount (String password, String code)
        throws ServiceException;
}
