//
// $Id$

package com.threerings.msoy.web.server;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.servlet.IndiscriminateSiteIdentifier;
import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.NoSuchUserException;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.user.OOOUser;

import com.threerings.underwire.server.GameActionHandler;
import com.threerings.underwire.server.GameInfoProvider;
import com.threerings.underwire.server.persist.SupportRepository;
import com.threerings.underwire.server.persist.UnderwireRepository;
import com.threerings.underwire.web.client.UnderwireException;
import com.threerings.underwire.web.server.UnderwireServlet;

import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MsoyOOOUserRepository;
import com.threerings.msoy.server.persist.OOODatabase;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.data.ServiceException;

import com.threerings.msoy.underwire.server.MsoyGameActionHandler;
import com.threerings.msoy.underwire.server.MsoyGameInfoProvider;

import static com.threerings.msoy.Log.log;

/**
 * An underwire servlet which uses a the msoy connection provider and user manager.
 */
public class MsoyUnderwireServlet extends UnderwireServlet
{
    @Override // from UnderwireServlet
    protected SiteIdentifier createSiteIdentifier ()
    {
        return new IndiscriminateSiteIdentifier();
    }

    @Override // from UnderwireServlet
    protected SupportRepository createSupportRepository ()
    {
        return _authRepo;
    }

    @Override // from UnderwireServlet
    protected UnderwireRepository createUnderwireRepository ()
    {
        return new UnderwireRepository(_userCtx);
    }

    @Override // from UnderwireServlet
    protected Caller userLogin (String username, String password, int expireDays)
        throws PersistenceException, AuthenticationFailedException
    {
        try {
            MemberRecord mrec = _author.authenticateSession(username, password);

            Caller caller = new Caller();
            caller.username = Integer.toString(mrec.memberId);
            caller.email = mrec.accountName;
            caller.authtok = _memberRepo.startOrJoinSession(mrec.memberId, expireDays);
            caller.isSupport = mrec.isSupport();
            return caller;

        } catch (ServiceException se) {
            // convert the exception into the required type
            String message = se.getMessage();
            if (message.equals(MsoyAuthCodes.NO_SUCH_USER)) {
                throw new NoSuchUserException(message);
            } else if (message.equals(MsoyAuthCodes.INVALID_PASSWORD)) {
                throw new InvalidPasswordException(message);
            } else {
                throw new AuthenticationFailedException(message);
            }
        }
    }

    @Override // from UnderwireServlet
    protected boolean allowEmailUpdate ()
    {
        return false;
    }

    @Override // from UnderwireServlet
    protected String getUsername (OOOUser user)
        throws UnderwireException
    {
        return getUsername(user.email);
    }

    @Override // from UnderwireServlet
    protected String getUsername (String username)
        throws UnderwireException
    {
        MemberName name;
        try {
            name = _memberRepo.loadMemberName(username);
        } catch (PersistenceException pe) {
            log.warning("Error looking up member", pe);
            name = null; // handled with the next check
        }
        if (name == null) {
            log.warning("Unable to find member information [email=" + username + "].");
            throw new UnderwireException(InvocationCodes.INTERNAL_ERROR);
        }
        return String.valueOf(name.getMemberId());
    }

    @Override // from UnderwireServlet
    protected int getSiteId ()
    {
        return OOOUser.METASOY_SITE_ID;
    }

    @Override // from UnderwireServlet
    protected GameInfoProvider getInfoProvider ()
    {
        return _infoprov;
    }

    @Override // from UnderwireServlet
    protected GameActionHandler getActionHandler ()
    {
        return _actionHandler;
    }

    @Override // from UnderwireServlet
    protected Caller loadCaller (String authtok)
        throws UnderwireException
    {
        try {
            MemberRecord mrec = _memberRepo.loadMemberForSession(authtok);
            if (mrec == null) {
                return null;
            }

            Caller caller = new Caller();
            caller.authtok = authtok;
            caller.username = String.valueOf(mrec.memberId);
            caller.email = mrec.accountName;
            caller.isSupport = mrec.isSupport();
            return caller;

        } catch (PersistenceException pe) {
            log.warning("Failed to load caller [tok=" + authtok + "].", pe);
            throw new UnderwireException("m.internal_error");
        }
    }

    // our dependencies
    @Inject protected MsoyGameActionHandler _actionHandler;
    @Inject protected MsoyGameInfoProvider _infoprov;
    @Inject protected MsoyAuthenticator _author;
    @Inject protected @OOODatabase PersistenceContext _userCtx;
    @Inject protected MsoyOOOUserRepository _authRepo;
    @Inject protected MemberRepository _memberRepo;
}
