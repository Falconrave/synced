//
// $Id$

package com.threerings.msoy.chat.server;

import com.samskivert.util.Interval;

import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelObject;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.msoy.peer.data.HostedChannel;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;

import static com.threerings.msoy.Log.log;

/**
 * Wrapper for channels hosted on this peer server.
 */
public class HostedWrapper extends ChannelWrapper
{
    public HostedWrapper (ChatChannelManager mgr, ChatChannel channel)
    {
        super(mgr, channel);
    }

    // from abstract class ChannelWrapper 
    public void initialize (final ChannelCreationContinuation cccont)
    {
        log.info("Creating chat channel " + _channel + ".");

        // create and initialize a new chat channel object
        _ccobj = MsoyServer.omgr.registerObject(new ChatChannelObject());
        _ccobj.channel = _channel;

        // and advertise to other peers that we're hosting this channel
        HostedChannel hosted = new HostedChannel(_channel, _ccobj.getOid());
        ((MsoyNodeObject) MsoyServer.peerMan.getNodeObject()).addToHostedChannels(hosted);

        // initialize speak service
        SpeakDispatcher sd = new SpeakDispatcher(new HostedSpeakHandler(this, _mgr));
        _ccobj.setSpeakService((SpeakMarshaller)MsoyServer.invmgr.registerDispatcher(sd));
        _ccobj.addListener(this);
        
        cccont.creationSucceeded(this);
    }

    // from abstract class ChannelWrapper 
    public void shutdown ()
    {
        _ccobj.removeListener(this);
        MsoyServer.invmgr.clearDispatcher(_ccobj.speakService);

        log.info("Shutting down hosted chat channel: " + _channel + ".");
        MsoyNodeObject host = (MsoyNodeObject) MsoyServer.peerMan.getNodeObject();
        host.removeFromHostedChannels(HostedChannel.getKey(_channel));

        // clean up the hosted dobject
        MsoyServer.omgr.destroyObject(_ccobj.getOid());
    }

    // from abstract class ChannelWrapper 
    public void addChatter (VizMemberName chatter)
    {
        try {
            removeStaleMessagesFromHistory();
            _mgr.addUser(null, chatter, _channel, new ChatterListener(chatter));
            cancelShutdowner();
        } catch (Exception ex) {
            log.warning("Host failed to add a new user [user=" + chatter +
                        ", channel=" + _channel + ", error=" + ex.getMessage() + "].");
        }
    }

    // from abstract class ChannelWrapper 
    public void removeChatter (VizMemberName chatter)
    {
        try {
            _mgr.removeUser(null, chatter, _channel, new ChatterListener(chatter));
        } catch (Exception ex) {
            log.warning("Host failed to remove a user [user=" + chatter +
                        ", channel=" + _channel + ", error=" + ex.getMessage() + "].");
        }
    }

    /**
     * Wrapper around the distributed channel object, to let chat manager add or remove
     * users from the chatter list. It is assumed the caller already checked whether
     * this modification is valid.
     *
     * @param chatter user to be added or removed from this channel
     * @param addAction if true, the user will be added, otherwise they will be removed
     */
    protected void updateDistributedObject (VizMemberName chatter, boolean addAction)
    {
        if (addAction) {
            _ccobj.addToChatters(chatter);
        } else {
            if (_ccobj.chatters.contains(chatter)) {
                _ccobj.removeFromChatters(chatter.getKey());
                if (_ccobj.chatters.size() == 0) {
                    checkShutdownInterval();
                }
            } else {
                log.warning("Requested to remove non-member of chatter set [channel=" + _channel +
                            ", chatter=" + chatter + "].");
            }
        }
    }

    protected void checkShutdownInterval ()
    {
        // queue up a shutdown interval, unless we've already got one.
        if (_shutdownInterval == null) {
            _shutdownInterval = new Interval((PresentsDObjectMgr)_ccobj.getManager()) {
                public void expired () {
                    _mgr.removeWrapper(HostedWrapper.this);
                    shutdown();
                }
            };
            _shutdownInterval.schedule(SHUTDOWN_PERIOD);
        }
    }

    /**
     * Cancels any registered shutdown interval.
     */
    protected void cancelShutdowner ()
    {
        if (_shutdownInterval != null) {
            _shutdownInterval.cancel();
            _shutdownInterval = null;
        }
    }

    /**
     * Called when chatter list is changed, deletes the channel if nobody is
     * participating in it anymore.
     */
    protected class ChatterListener implements PeerChatService.ConfirmListener
    {
        public ChatterListener (VizMemberName chatter) {
            _chatter = chatter;
        }
        public void requestProcessed () {
        }
        public void requestFailed (String cause) {
            log.info("Hosted channel: chatter action failed [channel=" + _channel +
                     ", user=" + _chatter + ", chatterCount=" +
                     _ccobj.chatters.size() + ", cause = " + cause + "].");
        }
        protected VizMemberName _chatter;
    };

    protected static final long SHUTDOWN_PERIOD = 5 * 60 * 1000L;

    protected Interval _shutdownInterval;
}
