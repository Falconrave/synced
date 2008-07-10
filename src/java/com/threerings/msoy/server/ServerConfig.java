//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.StaticConnectionProvider;
import com.samskivert.jdbc.depot.CacheAdapter;
import com.samskivert.util.Config;
import com.samskivert.util.StringUtil;

import com.threerings.presents.client.Client;

import static com.threerings.msoy.Log.log;

/**
 * Provides access to installation specific configuration. Properties that
 * are specific to a particular Bang! server installation are accessed via
 * this class.
 */
public class ServerConfig
{
    /** The name assigned to this server node. */
    public static String nodeName;

    /** The unique id associated with this node. */
    public static int nodeId;

    /** The root directory of the server installation. */
    public static File serverRoot;

    /** The publicly accessible DNS name of the host on which this server is running. */
    public static String serverHost;

    /** The back-channel DNS name of the host on which this server is running. */
    public static String backChannelHost;

    /** The ports on which we are listening for client connections. */
    public static int[] serverPorts;

    /** The port on which we are listening for HTTP connections. */
    public static int httpPort;

    /** The port on which game servers start listening. */
    public static int gameServerPort;

    /** The secret used to authenticate other servers in our cluster. */
    public static String sharedSecret;

    /** The local directory where dictionary files are stored. */
    public static File dictionaryDir;

    /** The local directory into which uploaded media is stored. */
    public static File mediaDir;

    /** The URL from which we instruct clients to load their media. */
    public static String mediaURL;

    /** Enables S3 media storage. */
    public static boolean mediaS3Enable;

    /** The remote S3 bucket in which media is stored. */
    public static String mediaS3Bucket;

    /** The user ID used for S3 authentication. */
    public static String mediaS3Id;

    /** The secret key used for S3 authentication. */
    public static String mediaS3Key;

    /** Event logging server host name. */
    public static String eventLogHostname;

    /** Event logging server host name. */
    public static int eventLogPort;

    /** Event logging server host name. */
    public static String eventLogUsername;

    /** Event logging server host name. */
    public static String eventLogPassword;

    /** Event logging server spooling directory. */
    public static String eventLogSpoolDir;

    /** The port on which we listen to socket policy requests. */
    public static int socketPolicyPort;

    /** The ReCaptcha public key. */
    public static String recaptchaPublicKey;

    /** The ReCaptcha private key. */
    public static String recaptchaPrivateKey;

    /** Provides access to our config properties. */
    public static Config config = new Config("server");

    /** The secret used to authenticate the bureau launching client. */
    public static String bureauSharedSecret;

    /** Comma-separated game server list the bureau launcher will connect to */
    public static String bureauGameServers;

    /** True if the game server kicks off local proceses for bureaus. */
    public static boolean localBureaus;

    /** True if the server should restart when code changes. */
    public static boolean autoRestart;

    /**
     * Returns a provider of JDBC connections.
     */
    public static ConnectionProvider createConnectionProvider ()
        throws Exception
    {
        return new StaticConnectionProvider(config.getSubProperties("db"));
    }

    /**
     * Creates the Depot cache that should be used by this server and its tools. May return null,
     * in which case no caching is used.
     */
    public static CacheAdapter createCacheAdapter ()
        throws Exception
    {
        CacheAdapter cacheAdapter = null;
        String adapterName = config.getValue("depot.cache.adapter", "");
        if (adapterName.length() > 0) {
            Class<?> adapterClass = Class.forName(adapterName);
            if (adapterClass != null) {
                if (CacheAdapter.class.isAssignableFrom(adapterClass)) {
                    log.info("Using cache manager: " + adapterClass);
                    cacheAdapter = (CacheAdapter) adapterClass.newInstance();
                } else {
                    log.warning("Configured with invalid CacheAdapter: " + adapterName + ".");
                }
            }
        }
        return cacheAdapter;
    }

    /**
     * Returns a URL that can be used to make HTTP requests from this server.
     */
    public static String getServerURL ()
    {
        String defurl = "http://" + serverHost + (httpPort != 80 ? ":" + httpPort : "");
        return config.getValue("server_url", defurl);
    }

    /**
     * Returns the address from which automated emails are sent.
     */
    public static String getFromAddress ()
    {
        return "\"Whirled Mailbot\" <peas@whirled.com>"; // TODO: move this to the server config
    }

    /**
     * Returns the HTTP port on which the specified node should be listening.
     */
    public static int getHttpPort (String nodeName)
    {
        return config.getValue(nodeName + ".http_port", httpPort);
    }

    /**
     * Returns the group id of the announcement group, or 0 if none is configured.
     */
    public static int getAnnounceGroupId ()
    {
        return config.getValue("announce_group_id", 0);
    }

    /**
     * Returns the group id of the bug tracking group, or 0 if none is configured.
     */
    public static int getIssueGroupId ()
    {
        return config.getValue("issue_group_id", 0);
    }

    /** The pattern via which we obtain our node id from our name. */
    protected static final Pattern NODE_ID_PATTERN = Pattern.compile("msoy([0-9]+)");

    /**
     * Configures server bits when this class is resolved.
     */
    static {
        // these will be overridden if we're running in cluster mode
        serverHost = config.getValue("server_host", "localhost");
        serverPorts = config.getValue("server_ports", Client.DEFAULT_SERVER_PORTS);
        httpPort = config.getValue("http_port", 8080);
        gameServerPort = config.getValue("game_server_port", 47625);
        socketPolicyPort = config.getValue("socket_policy_port", 47623);

        // if we're a server node (not the webapp or a tool) do some extra stuff
        if (Boolean.getBoolean("is_node")) {
            List<String> errors = Lists.newArrayList();
            String hostname = System.getProperty("hostname");

            // if we have a host node pattern, use that to determine our node id and name
            String hostNodePattern = config.getValue("host_node_pattern", "");
            if (!StringUtil.isBlank(hostNodePattern)) {
                try {
                    Matcher m = Pattern.compile(hostNodePattern).matcher(hostname);
                    if (!m.matches()) {
                        errors.add("Unable to determine node from hostname [hostname=" + hostname +
                                   ", pattern=" + hostNodePattern + "].");
                    } else {
                        nodeId = Integer.parseInt(m.group(1));
                        nodeName = "msoy" + nodeId;
                    }
                } catch (Exception e) {
                    errors.add("Unable to determine node from hostname [hostname=" + hostname +
                               ", pattern=" + hostNodePattern + "].");
                    errors.add(e.getMessage());
                }

            } else {
                // otherwise we should get our node name as a system property
                nodeName = System.getProperty("node", "");
                // ...or we'll determine it from our hostname
                if (StringUtil.isBlank(nodeName)) {
                    errors.add("Must have either 'node' sysprop or 'host_node_pattern' config.");
                }

                // obtain our node id from the node name
                Matcher m = NODE_ID_PATTERN.matcher(nodeName);
                if (m.matches()) {
                    nodeId = Integer.parseInt(m.group(1));
                } else {
                    errors.add("Unable to determine node id from name [name=" + nodeName + "]. " +
                               "Node name must match pattern '" + NODE_ID_PATTERN + "'.");
                }
            }

            // we tell other nodes to connect to us using our back channel hostname
            backChannelHost = hostname;
            if (StringUtil.isBlank(backChannelHost)) {
                errors.add("Missing 'hostname' system property.");
            }

            // configure our server hostname based on our server_host_pattern
            String hostPattern = config.getValue("server_host_pattern", "");
            if (!StringUtil.isBlank(hostPattern)) {
                try {
                    serverHost = String.format(hostPattern, nodeId);
                } catch (Exception e) {
                    errors.add("Invalid 'server_host_pattern' supplied: " + hostPattern);
                }
            }

            // if node_port_offset is specified, adjust our various ports
            String nodePortOffset = config.getValue("node_port_offset", "");
            if (!StringUtil.isBlank(nodePortOffset)) {
                try {
                    int offset = Integer.valueOf(nodePortOffset);
                    for (int ii = 0; ii < serverPorts.length; ii++) {
                        serverPorts[ii] = serverPorts[ii] + offset + nodeId;
                    }
                    httpPort = httpPort + offset + nodeId;
                    gameServerPort = gameServerPort + offset + nodeId;
                } catch (Exception e) {
                    errors.add("Invalid 'node_port_offset' supplied: " + nodePortOffset);
                }
            }

            // if we have any errors, report them and exit
            if (errors.size() > 0) {
                for (String error : errors) {
                    log.warning(error);
                }
                System.exit(255);
            }
        }

        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        mediaDir = new File(config.getValue("media_dir", "/tmp"));
        mediaURL = config.getValue("media_url", "http://localhost:" + httpPort + "/media");
        mediaS3Enable = config.getValue("media_s3enable", false);
        mediaS3Bucket = config.getValue("media_s3bucket", "msoy");
        mediaS3Id = config.getValue("media_s3id", "id");
        mediaS3Key = config.getValue("media_s3key", "key");
        sharedSecret = config.getValue("server_secret", "");
        eventLogHostname = config.getValue("event_log_host", "");
        eventLogPort = config.getValue("event_log_port", 0);
        eventLogUsername = config.getValue("event_log_username", "");
        eventLogPassword = config.getValue("event_log_password", "");
        eventLogSpoolDir = config.getValue("event_log_spool_dir", "");
        recaptchaPublicKey = config.getValue("recaptcha_public", "");
        recaptchaPrivateKey = config.getValue("recaptcha_private", "");
        bureauSharedSecret = config.getValue("bureau_secret", "");
        bureauGameServers = config.getValue("bureau_game_servers", "localhost:47625");
        localBureaus = config.getValue("local_bureaus", true);
        autoRestart = config.getValue("auto_restart", false);
    }
}
