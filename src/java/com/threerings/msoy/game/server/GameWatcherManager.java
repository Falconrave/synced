//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.annotation.EventThread;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.HostedRoom;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import static com.threerings.msoy.Log.log;

/**
 * A registry of watched members whose movements from scene to scene anywhere in the Whirled
 * (i.e. cross server) are relayed to the associated {@link Observer} objects.
 *
 * The sole client of this service is currently AVRGameManager, which needs to keep track of what
 * rooms its various current players are in.
 */
@Singleton @EventThread
public class GameWatcherManager
    implements MsoyPeerManager.MemberObserver
{
    /**
     * Interface for notifying the AVRGameManager of the whereabouts of a member.
     */
    public static interface Observer
    {
        /**
         * Notifies that a member has moved to a new scene and/or logged on.
         */
        void memberMoved (int memberId, int sceneId, String hostname, int port);

        /**
         * Notifies that a member has logged off.
         */
        void memberLoggedOff (int memberId);
    }

    @Inject public GameWatcherManager (MsoyPeerManager peerMan)
    {
        peerMan.memberObs.add(this);
    }

    /**
     * Subscribe to notification of this member's scene-to-scene movements on the world servers.
     * The observer will be immediately notified of the member's current location via a call to
     * {@link Observer#memberMoved} if the member is currently in a location.
     */
    public void addWatch (int memberId, Observer observer)
    {
        Observer old = _observers.put(memberId, observer);
        if (old != null) {
            log.warning("Displaced existing watcher", "memberId", "observer", old);
        }

        int sceneId = _peerMan.getMemberScene(memberId);
        if (sceneId == 0) {
            log.warning("Watched member has no current location", "memberId", memberId);
            return;
        }

        Tuple<String, HostedRoom> room = _peerMan.getSceneHost(sceneId);
        if (room == null) {
            log.warning("Host not found for scene", "scene", sceneId);
            return;
        }

        memberEnteredScene(room.left, memberId, sceneId);
    }

    /**
     * Clear an existing movement watch on the given member.
     */
    public void clearWatch (int memberId)
    {
        if (_observers.remove(memberId) == null) {
            log.warning("Attempt to clear non-existent watch", "memberId", memberId);
        }
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberLoggedOn (String node, MemberName member)
    {
        // nada
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberLoggedOff (String node, MemberName member)
    {
        Observer observer = _observers.get(member.getMemberId());
        if (observer != null) {
            observer.memberLoggedOff(member.getMemberId());
        }
    }

    // from interface MsoyPeerManager.MemberObserver
    public void memberEnteredScene (String node, int memberId, int sceneId)
    {
        Observer observer = _observers.get(memberId);
        if (observer != null) {
            String host = _peerMan.getPeerPublicHostName(node);
            int port = _peerMan.getPeerPort(node);
            observer.memberMoved(memberId, sceneId, host, port);
        }
    }

    /** A map of members to {@link Observer} objects to notify of each member's movements. */
    protected IntMap<Observer> _observers = new HashIntMap<Observer>();

    @Inject protected MsoyPeerManager _peerMan;
}
