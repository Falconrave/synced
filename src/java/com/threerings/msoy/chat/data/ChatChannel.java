//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.Name;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.ChannelName;

/**
 * Defines a particular chat channel.
 */
public class ChatChannel extends SimpleStreamableObject
{
    /** A chat channel between a player and their friend. Implemented using tells. */
    public static final int FRIEND_CHANNEL = 1;

    /** A chat channel open to all members of a group. */
    public static final int GROUP_CHANNEL = 2;

    /** A chat channel created by a player into whom they invite other players. */
    public static final int PRIVATE_CHANNEL = 3;

    /** The type of this chat channel. */
    public int type;

    /** The name that identifies this channel (either a {@link MemberName}, {@link GroupName} or
     * {@link ChannelName}. */
    public Name ident;

    /**
     * Creates a channel identifier for a channel communicating with the specified friend.
     */
    public static ChatChannel makeFriendChannel (MemberName friend)
    {
        return new ChatChannel(FRIEND_CHANNEL, friend);
    }

    /**
     * Creates a channel identifier for the specified group's channel.
     */
    public static ChatChannel makeGroupChannel (GroupName group)
    {
        return new ChatChannel(GROUP_CHANNEL, group);
    }

    /**
     * Creates a channel identifier for the specified named private channel.
     */
    public static ChatChannel makePrivateChannel (ChannelName channel)
    {
        return new ChatChannel(PRIVATE_CHANNEL, channel);
    }

    /** Used for unserialization. */
    public ChatChannel ()
    {
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (!(other instanceof ChatChannel)) {
            return false;
        }
        ChatChannel oc = (ChatChannel)other;
        return type == oc.type && ident.equals(oc.ident);
    }

    @Override // from Object
    public int hashCode ()
    {
        return type ^ ident.hashCode();
    }

    protected ChatChannel (int type, Name ident)
    {
        this.type = type;
        this.ident = ident;
    }
}
