//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.peer.data.CrowdNodeObject;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.web.data.MemberCard;

/**
 * Maintains information on an MSOY peer server.
 */
public class MsoyNodeObject extends CrowdNodeObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>hostedScenes</code> field. */
    public static final String HOSTED_SCENES = "hostedScenes";

    /** The field name of the <code>hostedGames</code> field. */
    public static final String HOSTED_GAMES = "hostedGames";

    /** The field name of the <code>hostedProjects</code> field. */
    public static final String HOSTED_PROJECTS = "hostedProjects";

    /** The field name of the <code>hostedChannels</code> field. */
    public static final String HOSTED_CHANNELS = "hostedChannels";

    /** The field name of the <code>memberLocs</code> field. */
    public static final String MEMBER_LOCS = "memberLocs";

    /** The field name of the <code>msoyPeerService</code> field. */
    public static final String MSOY_PEER_SERVICE = "msoyPeerService";

    /** The field name of the <code>peerChatService</code> field. */
    public static final String PEER_CHAT_SERVICE = "peerChatService";

    /** The field name of the <code>peerGameService</code> field. */
    public static final String PEER_GAME_SERVICE = "peerGameService";

    /** The field name of the <code>peerProjectService</code> field. */
    public static final String PEER_PROJECT_SERVICE = "peerProjectService";
    // AUTO-GENERATED: FIELDS END

    /** Contains info on all scenes hosted by this server. */
    public DSet<HostedRoom> hostedScenes = new DSet<HostedRoom>();

    /** Contains info on all games hosted by this server. */
    public DSet<HostedGame> hostedGames = new DSet<HostedGame>();

    /** Contains info on all projects hosted by this server. */
    public DSet<HostedProject> hostedProjects = new DSet<HostedProject>();

    /** Contains info on all chat channels hosted by this server. */
    public DSet<HostedChannel> hostedChannels = new DSet<HostedChannel>();

    /** Contains the current location of all members on this server. */
    public DSet<MemberLocation> memberLocs = new DSet<MemberLocation>();

    /** Handles special communication between MSOY peers. */
    public MsoyPeerMarshaller msoyPeerService;

    /** Dispatches chat channel modifications between proxies and hosts. */
    public PeerChatMarshaller peerChatService;

    /** Dispatches game-related peer notifications. */
    public PeerGameMarshaller peerGameService;

    /** Dispatches Swiftly project room peer notifications. */
    public PeerProjectMarshaller peerProjectService;

    /**
     * If the specified member is in a room on this server, creates and populates a status card
     * with their information. If they are in a game, the name of the game will not be filled in
     * (we don't know it) so the caller will have to obtain that from the popular places snapshot.
     */
    public MemberCard.Status getMemberStatus (int memberId)
    {
        MemberLocation mloc = memberLocs.get(memberId);
        if (mloc == null || (mloc.sceneId == 0 && mloc.gameId == 0)) {
            return null;
        }

        // don't show developer versions of games
        if (mloc.gameId != 0 && !Game.isDeveloperVersion(mloc.gameId) &&
            mloc.gameId != Game.TUTORIAL_GAME_ID) {
            MemberCard.InGame status = new MemberCard.InGame();
            status.gameId = mloc.gameId;
            return status;
        }

        if (mloc.sceneId != 0) {
            MemberCard.InScene status = new MemberCard.InScene();
            status.sceneId = mloc.sceneId;
            HostedRoom room = hostedScenes.get(mloc.sceneId);
            if (room != null) {
                status.sceneName = room.name;
            }
            return status;
        }

        return null;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the specified entry be added to the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedScenes (HostedRoom elem)
    {
        requestEntryAdd(HOSTED_SCENES, hostedScenes, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedScenes</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedScenes (Comparable key)
    {
        requestEntryRemove(HOSTED_SCENES, hostedScenes, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedScenes</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedScenes (HostedRoom elem)
    {
        requestEntryUpdate(HOSTED_SCENES, hostedScenes, elem);
    }

    /**
     * Requests that the <code>hostedScenes</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setHostedScenes (DSet<com.threerings.msoy.peer.data.HostedRoom> value)
    {
        requestAttributeChange(HOSTED_SCENES, value, this.hostedScenes);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.peer.data.HostedRoom> clone =
            (value == null) ? null : value.typedClone();
        this.hostedScenes = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedGames (HostedGame elem)
    {
        requestEntryAdd(HOSTED_GAMES, hostedGames, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedGames</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedGames (Comparable key)
    {
        requestEntryRemove(HOSTED_GAMES, hostedGames, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedGames</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedGames (HostedGame elem)
    {
        requestEntryUpdate(HOSTED_GAMES, hostedGames, elem);
    }

    /**
     * Requests that the <code>hostedGames</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setHostedGames (DSet<com.threerings.msoy.peer.data.HostedGame> value)
    {
        requestAttributeChange(HOSTED_GAMES, value, this.hostedGames);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.peer.data.HostedGame> clone =
            (value == null) ? null : value.typedClone();
        this.hostedGames = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedProjects</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedProjects (HostedProject elem)
    {
        requestEntryAdd(HOSTED_PROJECTS, hostedProjects, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedProjects</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedProjects (Comparable key)
    {
        requestEntryRemove(HOSTED_PROJECTS, hostedProjects, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedProjects</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedProjects (HostedProject elem)
    {
        requestEntryUpdate(HOSTED_PROJECTS, hostedProjects, elem);
    }

    /**
     * Requests that the <code>hostedProjects</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setHostedProjects (DSet<com.threerings.msoy.peer.data.HostedProject> value)
    {
        requestAttributeChange(HOSTED_PROJECTS, value, this.hostedProjects);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.peer.data.HostedProject> clone =
            (value == null) ? null : value.typedClone();
        this.hostedProjects = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>hostedChannels</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToHostedChannels (HostedChannel elem)
    {
        requestEntryAdd(HOSTED_CHANNELS, hostedChannels, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>hostedChannels</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromHostedChannels (Comparable key)
    {
        requestEntryRemove(HOSTED_CHANNELS, hostedChannels, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>hostedChannels</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateHostedChannels (HostedChannel elem)
    {
        requestEntryUpdate(HOSTED_CHANNELS, hostedChannels, elem);
    }

    /**
     * Requests that the <code>hostedChannels</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setHostedChannels (DSet<com.threerings.msoy.peer.data.HostedChannel> value)
    {
        requestAttributeChange(HOSTED_CHANNELS, value, this.hostedChannels);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.peer.data.HostedChannel> clone =
            (value == null) ? null : value.typedClone();
        this.hostedChannels = clone;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>memberLocs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToMemberLocs (MemberLocation elem)
    {
        requestEntryAdd(MEMBER_LOCS, memberLocs, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>memberLocs</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromMemberLocs (Comparable key)
    {
        requestEntryRemove(MEMBER_LOCS, memberLocs, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>memberLocs</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateMemberLocs (MemberLocation elem)
    {
        requestEntryUpdate(MEMBER_LOCS, memberLocs, elem);
    }

    /**
     * Requests that the <code>memberLocs</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setMemberLocs (DSet<com.threerings.msoy.data.MemberLocation> value)
    {
        requestAttributeChange(MEMBER_LOCS, value, this.memberLocs);
        @SuppressWarnings("unchecked") DSet<com.threerings.msoy.data.MemberLocation> clone =
            (value == null) ? null : value.typedClone();
        this.memberLocs = clone;
    }

    /**
     * Requests that the <code>msoyPeerService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setMsoyPeerService (MsoyPeerMarshaller value)
    {
        MsoyPeerMarshaller ovalue = this.msoyPeerService;
        requestAttributeChange(
            MSOY_PEER_SERVICE, value, ovalue);
        this.msoyPeerService = value;
    }

    /**
     * Requests that the <code>peerChatService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPeerChatService (PeerChatMarshaller value)
    {
        PeerChatMarshaller ovalue = this.peerChatService;
        requestAttributeChange(
            PEER_CHAT_SERVICE, value, ovalue);
        this.peerChatService = value;
    }

    /**
     * Requests that the <code>peerGameService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPeerGameService (PeerGameMarshaller value)
    {
        PeerGameMarshaller ovalue = this.peerGameService;
        requestAttributeChange(
            PEER_GAME_SERVICE, value, ovalue);
        this.peerGameService = value;
    }

    /**
     * Requests that the <code>peerProjectService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPeerProjectService (PeerProjectMarshaller value)
    {
        PeerProjectMarshaller ovalue = this.peerProjectService;
        requestAttributeChange(
            PEER_PROJECT_SERVICE, value, ovalue);
        this.peerProjectService = value;
    }
    // AUTO-GENERATED: METHODS END
}
