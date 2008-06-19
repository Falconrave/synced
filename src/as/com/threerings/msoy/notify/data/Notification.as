//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.util.Comparable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet_Entry;

import mx.core.UIComponent;

/**
 * Notification from the server to the client.  Notifications are sent as messages on the 
 * MemberObject.
 */
public /* abstract */ class Notification extends SimpleStreamableObject
{
    /**
     * Get the chat message used to announce this notification, or null.
     * WTF are you doing with a null announcement?
     *
     * All announcements will be translated using the "notify" bundle.
     * You can qualify the string if you want a different bundle.
     */
    public function getAnnouncement () :String
    {
        throw new Error("Abstract");
    }

    // from SimpleStreamableObject
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
    }

    // from SimpleStreamableObject
    override public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }
}
}
