//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.MultiKey;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.ForUpdate;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.threerings.msoy.web.data.MailFolder;

/**
 * Manages the persistent store of mail and mailboxes.
 */
public class MailRepository extends DepotRepository
{
    public MailRepository (ConnectionProvider conprov)
    {
        super(conprov);
    }

    /**
     * Fetch and return a single folder record for a given member, or null.
     */
    public MailFolderRecord getFolder (int memberId, int folderId)
        throws PersistenceException
    {
        return load(MailFolderRecord.class,
                    MailFolderRecord.OWNER_ID, memberId,
                    MailFolderRecord.FOLDER_ID, folderId);
    }

    /**
     * Count the number of read/unread messages in a given folder and return
     * a Tuple<Integer, Integer> with the number of read messages in the left
     * spot and the unread ones in the right.
     */
    public Tuple<Integer, Integer> getMessageCount (final int memberId, final int folderId)
        throws PersistenceException
    {
        int read = 0, unread = 0;
        Collection<MailCountRecord> records = findAll(
            MailCountRecord.class,
            new Where(MailMessageRecord.OWNER_ID_C, memberId,
                MailMessageRecord.FOLDER_ID_C, folderId),
                new FromOverride(MailMessageRecord.class),
                new FieldOverride(MailCountRecord.UNREAD, MailMessageRecord.UNREAD_C),
                new FieldOverride(MailCountRecord.COUNT, "count(*)"),
                new GroupBy(MailMessageRecord.UNREAD_C));
        for (MailCountRecord record : records) {
            if (record.unread) {
                unread = record.count;
            } else {
                read = record.count;
            }
        }
        return new Tuple<Integer, Integer>(read, unread);
    }

    /**
     * Fetch and return all folder records for a given member.
     */
    public Collection<MailFolderRecord> getFolders (int memberId)
        throws PersistenceException
    {
        testFolders(memberId);

        return findAll(MailFolderRecord.class, new Where(MailFolderRecord.OWNER_ID, memberId));
    }

    /**
     * Fetch and return a single message record in a given folder of a given member.
     */
    public MailMessageRecord getMessage (int memberId, int folderId, int messageId)
        throws PersistenceException
    {
        return load(MailMessageRecord.class,
                    MailMessageRecord.OWNER_ID, memberId,
                    MailMessageRecord.FOLDER_ID, folderId,
                    MailMessageRecord.MESSAGE_ID, messageId);
    }

    /**
     * Fetch and return all message records in a given folder of a given member.
     *
     * TODO: If messages end up being non-trivial in size, separate into own table.
     */
     public Collection<MailMessageRecord> getMessages (int memberId, int folderId)
         throws PersistenceException
     {
         return findAll(MailMessageRecord.class,
                        new Where(MailMessageRecord.OWNER_ID_C, memberId,
                                  MailMessageRecord.FOLDER_ID_C, folderId),
                        OrderBy.descending(MailMessageRecord.SENT_C));
     }

     /**
      * Insert a new folder record into the database.
      */
     public MailFolderRecord createFolder (MailFolderRecord record)
         throws PersistenceException
     {
         insert(record);
         return record;
     }

     /**
      * Insert a message into the database, for a given member and folder. This method
      * fills in the messageId field with a new value that's unique within the folder.
      */
     public MailMessageRecord fileMessage (MailMessageRecord record)
         throws PersistenceException
     {
         testFolders(record.ownerId);
         
         record.messageId = claimMessageId(record.ownerId, record.folderId, 1);
         insert(record);
         return record;
     }

     /**
      * Move a message from one folder to another.
      */
     public void moveMessage (int ownerId, int folderId, int newFolderId, int[] messageIds)
         throws PersistenceException
     {
         Comparable[] idArr = IntListUtil.box(messageIds);
         int newId = claimMessageId(ownerId, newFolderId, 1);
         MultiKey<MailMessageRecord> key = new MultiKey<MailMessageRecord>(
                 MailMessageRecord.class,
                 MailMessageRecord.OWNER_ID, ownerId,
                 MailMessageRecord.FOLDER_ID, folderId,
                 MailMessageRecord.MESSAGE_ID, idArr);
         updatePartial(MailMessageRecord.class, key, key,
                       MailMessageRecord.FOLDER_ID, newFolderId,
                       MailMessageRecord.MESSAGE_ID, newId);
     }

     /**
      * Delete one or more message records.
      */
     public void deleteMessage (int ownerId, int folderId, int... messageIds)
         throws PersistenceException
     {
         if (messageIds.length == 0) {
             return;
         }
         Comparable[] idArr = IntListUtil.box(messageIds);
         MultiKey<MailMessageRecord> key = new MultiKey<MailMessageRecord>(
                 MailMessageRecord.class,
                 MailMessageRecord.OWNER_ID, ownerId,
                 MailMessageRecord.FOLDER_ID, folderId,
                 MailMessageRecord.MESSAGE_ID, idArr);
         deleteAll(MailMessageRecord.class, key, key);
     }

     /**
      * Set the payload state of a message in the persistent store.
      */
     public void setPayloadState (int ownerId, int folderId, int messageId, byte[] state)
         throws PersistenceException
     {
         Key<MailMessageRecord> key =
             new Key<MailMessageRecord>(MailMessageRecord.class,
                     MailMessageRecord.OWNER_ID, ownerId,
                     MailMessageRecord.FOLDER_ID, folderId,
                     MailMessageRecord.MESSAGE_ID, messageId);
         updatePartial(MailMessageRecord.class, key, key, MailMessageRecord.PAYLOAD_STATE, state);
     }

    /**
     * Flag a message as being unread (or not).
     */
     public void setUnread (int ownerId, int folderId, int messageId, boolean unread)
         throws PersistenceException
     {
         Key<MailMessageRecord> key =
             new Key<MailMessageRecord>(MailMessageRecord.class,
                     MailMessageRecord.OWNER_ID, ownerId,
                     MailMessageRecord.FOLDER_ID, folderId,
                     MailMessageRecord.MESSAGE_ID, messageId);
         updatePartial(MailMessageRecord.class, key, key, MailMessageRecord.UNREAD, unread);
     }


     // claim space in a folder to deliver idCount messages; returns the first usable id
     protected int claimMessageId (int memberId, int folderId, int idCount)
         throws PersistenceException
     {
         MailFolderRecord record = load(MailFolderRecord.class,
                                        MailFolderRecord.OWNER_ID, memberId,
                                        MailFolderRecord.FOLDER_ID, folderId,
                                        new ForUpdate());
         int firstId = record.nextMessageId;
         record.nextMessageId += idCount;
         update(record, MailFolderRecord.NEXT_MESSAGE_ID);
         return firstId;
     }

    // initialize a member's folder structure, if necessary
    protected void testFolders (int memberId)
        throws PersistenceException
    {
        if (getFolders(memberId).size() == 0) {
            MailFolderRecord record = new MailFolderRecord();
            record.ownerId = memberId;
            record.nextMessageId = 1;
    
            record.folderId = MailFolder.INBOX_FOLDER_ID;
            record.name = "Inbox";
            createFolder(record);
    
            record.folderId = MailFolder.TRASH_FOLDER_ID;
            record.name = "Trash";
            createFolder(record);
    
            record.folderId = MailFolder.SENT_FOLDER_ID;
            record.name = "Sent";
            createFolder(record);
    
            MailMessageRecord welcome = new MailMessageRecord();
            welcome.ownerId = memberId;
            welcome.folderId = MailFolder.INBOX_FOLDER_ID;
            welcome.recipientId = memberId;
            // TODO: We need to be able to send system messages somehow.
            welcome.senderId = memberId;
            welcome.subject = "Welcome to Whirled!";
            welcome.sent = new Timestamp(System.currentTimeMillis());
            welcome.unread = true;
            welcome.bodyText = "Welcome to the Whirled mail system!\n";
            fileMessage(welcome);
        }
    }

}
