//
// $Id$

package com.threerings.msoy.avrg.data;

import com.google.common.primitives.Ints;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Helps an AVRG keep track of which scene a player is in.
 */
@com.threerings.util.ActionScript(omit=true)
public class PlayerLocation extends SimpleStreamableObject
    implements DSet.Entry, Comparable<PlayerLocation>
{
    /** The member id of the player in question. */
    public int playerId;

    /** The id of the scene currently occupied by this member. */
    public int sceneId;

    public PlayerLocation ()
    {
    }

    public PlayerLocation (int playerId, int sceneId)
    {
        this.playerId = playerId;
        this.sceneId = sceneId;
    }

    // from interface DSet.Entry
    public Comparable<?> getKey ()
    {
        return playerId;
    }

    // from interface Comparable<PlayerLocation>
    public int compareTo (PlayerLocation other)
    {
        return Ints.compare(playerId, other.playerId);
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return other != null && other.getClass().equals(getClass()) &&
            playerId == ((PlayerLocation) other).playerId &&
            sceneId == ((PlayerLocation)other).sceneId;
    }

    @Override // from Object
    public int hashCode ()
    {
        return playerId;
    }
}
