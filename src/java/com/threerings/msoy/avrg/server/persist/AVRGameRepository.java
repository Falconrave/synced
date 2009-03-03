//
// $Id$

package com.threerings.msoy.avrg.server.persist;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Maintains state for AVR games.
 */
@Singleton @BlockingThread
public class AVRGameRepository extends DepotRepository
{
    @Inject public AVRGameRepository (PersistenceContext context)
    {
        super(context);
    }

    public List<PlayerGameStateRecord> getPlayerGameState (int gameId, int memberId)
    {
        return findAll(PlayerGameStateRecord.class, new Where(
            PlayerGameStateRecord.GAME_ID, gameId,
            PlayerGameStateRecord.MEMBER_ID, memberId));
    }

    public List<GameStateRecord> getGameState (int gameId)
    {
        return findAll(GameStateRecord.class, new Where(GameStateRecord.GAME_ID, gameId));
    }

    /**
     * Stores a particular memory record in the repository.
     */
    public void storeState (GameStateRecord record)
    {
        if (record.datumValue == null) {
            delete(record);

        } else {
            store(record);
        }
    }

    /**
     * Stores a particular memory record in the repository.
     */
    public void storePlayerState (PlayerGameStateRecord record)
    {
        if (record.datumValue == null) {
            delete(record);

        } else {
            store(record);
        }
    }

    public void deleteProperty (int gameId, String key)
    {
        delete(GameStateRecord.class, GameStateRecord.getKey(gameId, key));
    }

    public void deletePlayerProperty (int gameId, int memberId, String key)
    {
        delete(PlayerGameStateRecord.class, PlayerGameStateRecord.getKey(gameId, memberId, key));
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(PlayerGameStateRecord.class,
                  new Where(new Conditionals.In(PlayerGameStateRecord.MEMBER_ID, memberIds)));
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(GameStateRecord.class);
        classes.add(PlayerGameStateRecord.class);
    }
}
