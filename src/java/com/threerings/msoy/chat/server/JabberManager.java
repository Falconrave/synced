//
// $Id$

package com.threerings.msoy.chat.server;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.util.Interval;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.util.Name;

import com.threerings.crowd.chat.data.UserMessage;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.data.all.ContactEntry;
import com.threerings.msoy.data.all.GatewayEntry;
import com.threerings.msoy.data.all.JabberName;

import com.threerings.msoy.chat.client.JabberService;

import com.threerings.msoy.web.client.DeploymentConfig;

import com.threerings.presents.server.ClientManager;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.server.PresentsClient;

import com.threerings.presents.data.ClientObject;

import com.threerings.presents.dobj.DSet;

import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketResponder;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.TimedPacketResponder;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.ToContainsFilter;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.RosterPacket;

import static com.threerings.msoy.Log.log;

/**
 * Manages the connection to a jabber server providing gateway access to external IM networks.
 */
public class JabberManager
    implements MsoyServer.Shutdowner, ClientManager.ClientObserver, JabberProvider,
               ConnectionListener
{

    public static boolean DEBUG = false;

    /**
     * Initializes this manager during server startup.
     */
    public void init (InvocationManager invmgr)
    {
        // register our jabber service
        invmgr.registerDispatcher(new JabberDispatcher(this), MsoyCodes.WORLD_GROUP);

        MsoyServer.registerShutdowner(this);
        MsoyServer.clmgr.addClientObserver(this);

        String host = ServerConfig.config.getValue("jabber.host", "localhost");
        int port = ServerConfig.config.getValue("jabber.port", 5275);
        String gatewayList = ServerConfig.config.getValue("jabber.gateways", "");

        ConnectionConfiguration config = new ConnectionConfiguration(host, port);
        _conn = new XMPPConnection(config, MsoyServer.omgr);

        // For now, only try and connect to the Jabber server if we're on a development server
        // and have at least one gateway configured
        if (!DeploymentConfig.devDeployment || StringUtil.isBlank(gatewayList)) {
            return;
        }

        try {
            _conn.connect(ServerConfig.nodeName);
        } catch (XMPPException e) {
            log.warning("Unable to connect to jabber server [host=" + host + ", error=" +
                    e + ", cause=" + e.getCause() + "].");
            attemptReconnect();
            return;
        }
        handshake();
    }

    // from interface MsoyServer.Shutdowner
    public void shutdown ()
    {
        if (_reconnectInterval != null) {
            _reconnectInterval.cancel();
            _reconnectInterval = null;
        }
        if (_conn.isConnected()) {
            _conn.removeConnectionListener(this);
        }
        cleanup();
        if (_conn.isConnected()) {
            _conn.disconnect();
        }
    }

    // from interface ConnectionListener
    public void connectionClosed ()
    {
        // we should never be closing the connection willingly unless we're shutting down
    }

    // from interface ConnectionListener
    public void connectionClosedOnError (Exception e)
    {
        log.info("Jabber server connection failed [error=" + e + ", cause=" + e.getCause() + "].");
        cleanup();
        attemptReconnect();
    }

    // from interface ConnectionListener
    public void reconnectingIn (int seconds)
    {
        // ignored
    }

    // from interface ConnectionListener
    public void reconnectionSuccessful ()
    {
    }

    // from interface ConnectionListener
    public void reconnectionFailed (Exception e)
    {
        attemptReconnect();
    }

    // from interface ClientManager.ClientObserver
    public void clientSessionDidStart (PresentsClient client)
    {
        if (!_conn.isConnected() || !(client.getClientObject() instanceof MemberObject)) {
            return;
        }
        MemberObject user = (MemberObject)client.getClientObject();
        user.startTransaction();
        try {
            for (String gateway : _gateways) {
                user.addToGateways(new GatewayEntry(gateway));
            }
        } finally {
            user.commitTransaction();
        }
    }

    // from interface ClientManager.ClientObserver
    public void clientSessionDidEnd (PresentsClient client)
    {
        if (!_conn.isConnected() || !(client.getClientObject() instanceof MemberObject)) {
            return;
        }
        MemberObject user = (MemberObject)client.getClientObject();
        logoffUser(user.username);
        removePacketResponder(user);
        _users.remove(user.username);
    }

    // from interface JabberProvider
    public void registerIM (ClientObject caller, final String gateway, final String username,
            String password, final JabberService.InvocationListener listener)
    {
        if (!_conn.isConnected() || !_conn.isAuthenticated() || !_gateways.contains(gateway)) {
            listener.requestFailed("IM service not currently available");
            return;
        }
        final MemberObject user = (MemberObject)caller;
        Registration reg = new Registration();
        reg.setTo(gateway + "." + _conn.getServiceName());
        String uJID = getJID(user);
        reg.setFrom(uJID);
        reg.setType(IQ.Type.SET);
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("username", username);
        map.put("password", password);
        log.info("Jabber register [uJID=" + uJID +
                ", to=" + gateway + "." + _conn.getServiceName() + "].");
        reg.setAttributes(map);
        TimedPacketResponder responder = new TimedPacketResponder(_conn) {
            public void handlePacket (Packet packet) {
                IQ iq = (IQ)packet;
                if (iq == null) {
                    listener.requestFailed("No response from IM service");
                } else if (iq.getType() == IQ.Type.ERROR) {
                    listener.requestFailed("Failed to register IM account: " +
                            iq.getError().toString());
                } else {
                    loginUser(user, gateway, username);
                }
            }
        };
        responder.init(new PacketIDFilter(reg.getPacketID()));
        _conn.sendPacket(reg);
        responder.nextResult(SmackConfiguration.getPacketReplyTimeout());
    }

    // from interface JabberProvider
    public void unregisterIM (ClientObject caller, String gateway,
            JabberService.InvocationListener listener)
    {
        if (!_conn.isConnected() || !_conn.isAuthenticated() || !_gateways.contains(gateway)) {
            listener.requestFailed("IM service not currently available");
            return;
        }
        MemberObject user = (MemberObject)caller;
        if (!user.gateways.containsKey(gateway)) {
            listener.requestFailed("You are not currently logged into this IM service");
            return;
        }
        logoffUser(user, gateway);
    }

    // from interface JabberProvider
    public void sendMessage (ClientObject caller, JabberName name, String message,
            final JabberService.ResultListener listener)
    {
        if (!_conn.isConnected() || !_conn.isAuthenticated()) {
            listener.requestFailed("IM service not currently available");
            return;
        }
        final MemberObject user = (MemberObject)caller;
        Chat chat = getChat(user, name);
        chat.sendMessage(message);
        listener.requestProcessed(null);
    }

    /**
     * Attempts to reconnect (if allowed) if we lose connection unexpectedly.
     */
    protected void attemptReconnect ()
    {
        if (_conn.isConnected() || !_conn.getConfiguration().isReconnectionAllowed()) {
            return;
        }
        _reconnectInterval = new Interval(MsoyServer.omgr) {
            public void expired () {
                try {
                    _conn.reconnect();
                } catch (XMPPException e) {
                    log.warning("Unable to reconnect to jabber server [host=" +
                            _conn.getConfiguration().getHost() + ", error=" +
                            e + ", cause=" + e.getCause() + "].");
                    attemptReconnect();
                    return;
                }
                _reconnectInterval.cancel();
                _reconnectInterval = null;
                handshake();
            }
        };
        _reconnectInterval.schedule(RECONNECT_TIMEOUT);
    }

    /**
     * Handshakes with the server after connection.
     */
    protected void handshake ()
    {
        _conn.addConnectionListener(this);
        String secret = ServerConfig.config.getValue("jabber.secret", "");
        _conn.handshake(secret, new ResultListener<Object>() {
            public void requestCompleted (Object result) {
                onAuthorization();
            }

            public void requestFailed (Exception cause) {
                log.warning("Failed handshake with jabber server [error=" + cause + ", cause=" +
                    cause.getCause() + "].");
                _conn.disconnect();
            }
        });
    }

    protected void onAuthorization ()
    {
        log.info("Successfully authorized on Jabber Server [host=" +
                _conn.getConfiguration().getHost() + "].");
        String gatewayList = ServerConfig.config.getValue("jabber.gateways", "");
        _gateways.clear();
        for (String gateway : StringUtil.split(gatewayList, ",")) {
            _gateways.add(gateway);
        }
        if (_chatListener != null) {
            return;
        }
        _chatListener = new ChatManagerListener() {
            public void chatCreated (Chat chat, boolean createdLocally) {
                if (createdLocally) {
                    return;
                }
                if (DEBUG) {
                    log.info("Remote chat creation [from=" + chat.getParticipant() + ", to=" +
                            chat.getUser() + "].");
                }
                JabberUser juser = _users.get(fromJID(chat.getUser()));
                if (juser == null) {
                    log.warning("No user found for incoming chat");
                    return;
                }
                if (juser.chats == null) {
                    juser.chats = new HashMap<JabberName, Chat>();
                }
                if (juser.messageListener == null) {
                    juser.messageListener = new UserMessageListener(juser);
                }
                chat.addMessageListener(juser.messageListener);
                juser.chats.put(new JabberName(
                            StringUtils.parseBareAddress(chat.getParticipant())), chat);
            }
        };
        _conn.getChatManager().addChatListener(_chatListener);
    }

    /**
     * Cleans up user information on disconnection.
     */
    protected void cleanup ()
    {
        for (Name name : _users.keySet()) {
            logoffUser(name);
        }
    }

    protected void loginUser (MemberObject user, String gateway, String username)
    {
        String ujid = getJID(user);
        createPacketResponder(user);
        Presence presence = new Presence(Presence.Type.available);
        presence.setFrom(ujid);
        presence.setTo(gateway + "." + _conn.getServiceName());
        _conn.packetWriter.sendPacket(presence);
        if (DEBUG) {
            log.info("Jabber login [uJID=" + ujid +
                    ", to=" + gateway + "." + _conn.getServiceName() + "].");
        }

        GatewayEntry gent = user.gateways.get(gateway);
        if (gent == null) {
            gent = new GatewayEntry(gateway, true, username);
            user.addToGateways(gent);
        } else {
            gent.online = true;
            gent.username = username;
            user.updateGateways(gent);
        }
    }

    protected void logoffUser (Name name)
    {
        JabberUser juser = _users.get(name);
        if (juser == null || juser.user == null) {
            return;
        }
        juser.user.startTransaction();
        try {
            GatewayEntry[] gents = juser.user.gateways.toArray(
                    new GatewayEntry[juser.user.gateways.size()]);
            for (GatewayEntry gent : gents) {
                if (!gent.online) {
                    continue;
                }
                logoffUser(juser.user, gent.gateway);
            }
        } finally {
            juser.user.commitTransaction();
        }
    }

    protected void logoffUser (MemberObject user, String gateway)
    {
        String uJID = getJID(user);
        if (DEBUG) {
            log.info("Jabber logoff [uJID=" + uJID +
                    ", to=" + gateway + "." + _conn.getServiceName() + "].");
        }
        if (_conn.isConnected()) {
            Registration reg = new Registration();
            reg.setTo(gateway + "." + _conn.getServiceName());
            reg.setFrom(uJID);
            reg.setType(IQ.Type.SET);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("remove", "");
            reg.setAttributes(map);
            _conn.packetWriter.sendPacket(reg);
        }
        GatewayEntry gent = user.gateways.get(gateway);
        if (gent != null) {
            gent.online = false;
            gent.username = null;
            user.updateGateways(gent);
        }
    }

    protected String getJID (MemberObject user)
    {
        return getJID(user.username);
    }

    protected String getJID (Name name)
    {
        return StringUtils.escapeNode(name.getNormal()) + "@" + ServerConfig.nodeName +
                "." + _conn.getServiceName() + "/" + ServerConfig.nodeName;
    }

    protected Name fromJID (String jid)
    {
        return new Name(StringUtils.unescapeNode(StringUtils.parseName(jid)));
    }

    protected void createPacketResponder (MemberObject user)
    {
        JabberUser juser = getJabberUser(user, true);
        if (juser.packetResponder == null) {
            juser.packetResponder = new UserPacketResponder(user);
            _conn.addPacketListener(juser.packetResponder,
                    new ToContainsFilter(StringUtils.parseBareAddress(getJID(user))));
        }
    }

    protected void removePacketResponder (MemberObject user)
    {
        JabberUser juser = getJabberUser(user, false);
        if (juser != null && juser.packetResponder != null) {
            _conn.removePacketListener(juser.packetResponder);
            juser.packetResponder = null;
        }
    }

    protected Chat getChat (MemberObject user, JabberName name)
    {
        JabberUser juser = getJabberUser(user, true);
        if (juser.chats == null) {
            juser.chats = new HashMap<JabberName, Chat>();
        }
        Chat chat = juser.chats.get(name);
        if (chat == null) {
            if (juser.messageListener == null) {
                juser.messageListener = new UserMessageListener(juser);
            }
            chat = _conn.getChatManager().createChat(
                    getJID(user), name.toJID(), juser.messageListener);
            juser.chats.put(name, chat);
        }
        return chat;
    }

    protected JabberUser getJabberUser (MemberObject user, boolean create)
    {
        JabberUser juser = _users.get(user.username);
        if (juser == null && create) {
            juser = new JabberUser(user);
            _users.put(user.username, juser);
        }
        return juser;
    }

    protected class UserPacketResponder extends PacketResponder
    {
        public UserPacketResponder (MemberObject user)
        {
            super(_conn.queue);
            _user = user;
            _user.setImContacts(new DSet<ContactEntry>());
        }

        @Override // from PacketResponder
        public void handlePacket (Packet packet)
        {
            if (DEBUG) {
                log.info("incoming jabber packet [user=" + _user.username +
                        ", xml=" + packet.toXML() + "].");
            }
            String from = packet.getFrom();
            if (!from.endsWith("." + _conn.getServiceName())) {
                return;
            }
            String gateway = StringUtils.parseServer(from);
            gateway = gateway.substring(0, gateway.indexOf("."));
            if (!_user.gateways.containsKey(gateway)) {
                return;
            }
            if (packet instanceof Presence) {
                handlePresence((Presence)packet, gateway);
            } else if (packet instanceof RosterPacket) {
                handleRoster((RosterPacket)packet, gateway);
            }
        }

        protected void handlePresence (Presence presence, String gateway)
        {
            if (StringUtil.isBlank(StringUtils.parseName(presence.getFrom()))) {
                return;
            }

            JabberName jname = new JabberName(presence.getFrom());
            ContactEntry entry = _user.imContacts.get(jname);
            if (presence.getType() == Presence.Type.available ||
                    presence.getType() == Presence.Type.unavailable) {
                boolean online = presence.getType() == Presence.Type.available;
                if (entry == null) {
                    entry = new ContactEntry(jname, online);
                    _user.addToImContacts(entry);
                } else if (online != entry.online) {
                    entry.online = online;
                    _user.updateImContacts(entry);
                }
            }
        }

        protected void handleRoster (RosterPacket roster, String gateway)
        {
            _user.startTransaction();
            try {
                for (RosterPacket.Item item : roster.getRosterItems()) {
                    if (StringUtil.isBlank(StringUtils.parseName(item.getUser()))) {
                        continue;
                    }
                    JabberName jname = new JabberName(item.getUser(),
                            StringUtil.isBlank(item.getName()) ? null : item.getName());
                    ContactEntry entry = _user.imContacts.get(jname);
                    if (entry == null) {
                        entry = new ContactEntry(jname, false);
                        _user.addToImContacts(entry);
                    } else if (item.getName() != null &&
                            !item.getName().equals(entry.name.getDisplayName())) {
                        entry.name = jname;
                        _user.updateImContacts(entry);
                    }
                }
            } finally {
                _user.commitTransaction();
            }
        }

        protected MemberObject _user;
    }

    protected class UserMessageListener
        implements MessageListener
    {
        public UserMessageListener (JabberUser juser)
        {
            _user = juser.user;
        }

        // from MessageListener
        public void processMessage (Chat chat, Message message)
        {
            JabberName jname = new JabberName(chat.getParticipant());
            ContactEntry entry = _user.imContacts.get(jname);
            if (entry != null) {
                jname = entry.name;
            }
            MsoyServer.chatprov.deliverTell(_user, new UserMessage(jname, message.getBody()));
        }

        protected MemberObject _user;
    }

    protected class JabberUser
    {
        public MemberObject user;
        public UserPacketResponder packetResponder;
        public UserMessageListener messageListener;
        public HashMap<JabberName, Chat> chats;

        public JabberUser (MemberObject user)
        {
            this.user = user;
        }
    }

    /** Reference to our XMPP connection. */
    protected XMPPConnection _conn;

    /** Our chat listener. */
    protected ChatManagerListener _chatListener;

    /** Our interval for reconnecting to the jabber server. */
    protected Interval _reconnectInterval;

    /** Mapping of username to runtime jabber information for that user. */
    protected HashMap<Name, JabberUser> _users = new HashMap<Name, JabberUser>();

    /** The available gateways. */
    protected ArrayList<String> _gateways = new ArrayList<String>();

    /** The time between reconnection attempts. */
    protected static final long RECONNECT_TIMEOUT = 60 * 1000L;
}
