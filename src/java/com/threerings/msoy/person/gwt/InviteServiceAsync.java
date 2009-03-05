//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.MemberCard;

/**
 * The asynchronous (client-side) version of {@link InviteService}.
 */
public interface InviteServiceAsync
{
    /**
     * The asynchronous version of {@link InviteService#getWebMailAddresses}.
     */
    void getWebMailAddresses (
        String email, String password, boolean skipFriends,
        AsyncCallback<List<EmailContact>> callback);

    /**
     * The asynchronous version of {@link InviteService#getInvitationsStatus}.
     */
    void getInvitationsStatus (AsyncCallback<MemberInvites> callback);

    /**
     * The asynchronous version of {@link InviteService#sendInvites}.
     */
    void sendInvites (
        List<EmailContact> addresses, String fromName, String subject, String customMessage,
        boolean anonymous, AsyncCallback<InvitationResults> callback);

    /**
     * The asynchronous version of {@link InviteService#sendGameInvites}.
     */
    void sendGameInvites (
        List<EmailContact> addresses, int gameId, String from, String url, String customMessage,
        AsyncCallback<InvitationResults> callback);

    /**
     * The asynchronous version of {@link InviteService#sendWhirledMailGameInvites}.
     */
    void sendWhirledMailGameInvites (
        Set<Integer> recipientIds, int gameId, String subject, String body, String args,
        AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link InviteService#removeInvitation}.
     */
    void removeInvitation (String inviteId, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link InviteService#getHomeSceneId}.
     */
    void getHomeSceneId (AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link InviteService#getFriends}.
     */
    void getFriends (int count, AsyncCallback<List<MemberCard>> callback);
}
