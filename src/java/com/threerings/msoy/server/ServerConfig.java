//
// $Id$

package com.threerings.msoy.server;

import java.io.File;
import java.util.Properties;

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

    /** The root directory of the server installation. */
    public static File serverRoot;

    /** The publicly accessible DNS name of the host on which this server is running. */
    public static String serverHost;

    /** The back-channel DNS name of the host on which this server is running. */
    public static String backChannelHost;

    /** The ports on which we are listening for client connections. */
    public static int[] serverPorts;

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

    /** Provides access to our config properties. <em>Do not</em> modify
     * these properties! */
    public static Config config = new Config("server");

    /**
     * Returns the JDBC configuration.
     */
    public static Properties getJDBCConfig ()
    {
        return config.getSubProperties("db");
    }

    /**
     * Returns the port on which the server should listen for HTTP connections.
     * The default is 8080 to avoid conflict with local web server
     * installations on development servers but the expectation is that in
     * production the server will listen directly on port 80.
     */
    public static int getHttpPort ()
    {
        return config.getValue("http_port", 8080);
    }

    /**
     * Returns a URL that can be used to make HTTP requests from this server.
     */
    public static String getServerURL ()
    {
        return "http://" + serverHost + (getHttpPort() != 80 ? ":" + getHttpPort() : "");
    }

    /**
     * Returns the address from which automated emails are sent.
     */
    public static String getFromAddress ()
    {
        return "peas@whirled.com"; // TODO: move this to the server config
    }

    /**
     * Configures server bits when this class is resolved.
     */
    static {
        // fill in our standard properties
        serverRoot = new File(config.getValue("server_root", "/tmp"));
        mediaDir = new File(config.getValue("media_dir", "/tmp"));
        mediaURL = config.getValue("media_url", "http://localhost:" + getHttpPort() + "/media");
        mediaS3Enable = config.getValue("media_s3enable", false);
        mediaS3Bucket = config.getValue("media_s3bucket", "msoy");
        mediaS3Id = config.getValue("media_s3id", "id");
        mediaS3Key = config.getValue("media_s3key", "key");
        serverHost = config.getValue("server_host", "localhost");
        serverPorts = config.getValue("server_ports", Client.DEFAULT_SERVER_PORTS);

        // if we're a server node (not the webapp or a tool) do some extra stuff
        if (Boolean.getBoolean("is_node")) {
            // our node name and hostname come from system properties passed by our startup scripts
            nodeName = System.getProperty("node");
            if (StringUtil.isBlank(nodeName)) {
                log.warning("Missing 'node' system property. Cannot start.");
            }
            backChannelHost = System.getProperty("hostname");
            if (StringUtil.isBlank(backChannelHost)) {
                log.warning("Missing 'hostname' system property. Cannot start.");
            }
            if (StringUtil.isBlank(nodeName) || StringUtil.isBlank(backChannelHost)) {
                System.exit(-1);
            }
            // fill in our node-specific properties
            serverHost = config.getValue(nodeName + ".server_host", serverHost);
        }
    }
}
