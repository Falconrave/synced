//
// $Id$

package com.threerings.msoy.web.client;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.MailFolder;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Defines mail services available to the GWT/AJAX web client.
 */
public interface MailService extends RemoteService
{
    public MailFolder getFolder (WebIdent ident, int folderId)
        throws ServiceException;

    /**
     * Returns all folders for the specified member.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.MailFolder>
     */
    public List getFolders (WebIdent ident)
        throws ServiceException;

    public MailMessage getMessage (WebIdent ident, int folderId, int messageId)
        throws ServiceException;

    /**
     * Returns all message headers in the specified folder.
     *
     * @gwt.typeArgs <com.threerings.msoy.web.data.MailHeaders>
     */
    public List getHeaders (WebIdent ident, int folderId)
        throws ServiceException;

    public void deliverMessage (WebIdent ident, int recipientId, String subject, String text,
                                MailPayload object)
        throws ServiceException;

    public void updatePayload (WebIdent ident, int folderId, int messageId, MailPayload payload)
        throws ServiceException;

    public void deleteMessages (WebIdent ident, int folderId, int[] msgIdArr)
        throws ServiceException;
}
