//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains all the information needed to launch a particular game.
 */
public class LaunchConfig
    implements IsSerializable
{
    /** A constant used to denote in-world Flash games. */
    public static final int FLASH_IN_WORLD = 0;
    
    /** A constant used to denote lobbied Flash games. */
    public static final int FLASH_LOBBIED = 1;

    /** A constant used to denote single player Flash games. */
    public static final int FLASH_SOLO = 2;

    /** A constant used to denote Java games lobbied in Flash. */
    public static final int JAVA_FLASH_LOBBIED = 3;

    /** A constant used to denote Java games lobbied themselves (in Java). */
    public static final int JAVA_SELF_LOBBIED = 4;

    /** A constant used to denote single player Java games. */
    public static final int JAVA_SOLO = 5;

    /** The unique identifier for the game in question. */
    public int gameId;

    /** The type of this game (see above constants). */
    public int type;

    /** The display name of this game. */
    public String name;

    /** The URL relative to which game resources should be downloaded. */
    public String resourceURL;

    /** The path (relative to the reosurce URL) for the game client media (SWF
     * or JAR). */
    public String gameMediaPath;

    /** The server to which the game should connect (if this is a multiplayer
     * game). */
    public String server;

    /** The port on which the game should connect to the server (if this is a
     * multiplayer game). */
    public int port;
}
