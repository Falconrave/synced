//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableLobbyObject;
import com.threerings.parlor.data.TableMarshaller;

import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents a lobby for a particular game.
 */
public class LobbyObject extends DObject implements TableLobbyObject
{
    /** Used on the server to listen to subscriber count changes to a lobby object. */
    public interface SubscriberListener
    {
        /** Called when the number of subscribers has changed. */
        void subscriberCountChanged (LobbyObject target);
    }

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>game</code> field. */
    public static final String GAME = "game";

    /** The field name of the <code>gameDef</code> field. */
    public static final String GAME_DEF = "gameDef";

    /** The field name of the <code>tables</code> field. */
    public static final String TABLES = "tables";

    /** The field name of the <code>tableService</code> field. */
    public static final String TABLE_SERVICE = "tableService";

    /** The field name of the <code>groupId</code> field. */
    public static final String GROUP_ID = "groupId";

    /** The field name of the <code>splashMedia</code> field. */
    public static final String SPLASH_MEDIA = "splashMedia";
    // AUTO-GENERATED: FIELDS END

    /** The game that we're matchmaking for. */
    public GameSummary game;

    /** The parsed configuration info for this game. */
    public GameDefinition gameDef;

    /** The tables. */
    public DSet<Table> tables = new DSet<Table>();

    /** Used to communicate to the table manager. */
    public TableMarshaller tableService;

    /** The group to load up behind the lobby if not already in a room. */
    public int groupId;

    /** The splash media for our game, or null. */
    public MediaDesc splashMedia;

    /** If set on the server, will be called with subscriber updates. */
    public transient SubscriberListener subscriberListener;

    // from TableLobbyObject
    public DSet<Table> getTables ()
    {
        return tables;
    }

    // from TableLobbyObject
    public TableMarshaller getTableService ()
    {
        return tableService;
    }

    /**
     * Expose our subscriber count, for our lobby.
     */
    public int getSubscriberCount ()
    {
        return _scount;
    }

    @Override // from DObject
    public void addSubscriber (Subscriber<?> sub)
    {
        super.addSubscriber(sub);
        if (subscriberListener != null) {
            subscriberListener.subscriberCountChanged(this);
        }
    }

    @Override // from DObject
    public void removeSubscriber (Subscriber<?> sub)
    {
        super.removeSubscriber(sub);
        if (subscriberListener != null) {
            subscriberListener.subscriberCountChanged(this);
        }
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>game</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGame (GameSummary value)
    {
        GameSummary ovalue = this.game;
        requestAttributeChange(
            GAME, value, ovalue);
        this.game = value;
    }

    /**
     * Requests that the <code>gameDef</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGameDef (GameDefinition value)
    {
        GameDefinition ovalue = this.gameDef;
        requestAttributeChange(
            GAME_DEF, value, ovalue);
        this.gameDef = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>tables</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToTables (Table elem)
    {
        requestEntryAdd(TABLES, tables, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>tables</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromTables (Comparable<?> key)
    {
        requestEntryRemove(TABLES, tables, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>tables</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateTables (Table elem)
    {
        requestEntryUpdate(TABLES, tables, elem);
    }

    /**
     * Requests that the <code>tables</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setTables (DSet<Table> value)
    {
        requestAttributeChange(TABLES, value, this.tables);
        DSet<Table> clone = (value == null) ? null : value.typedClone();
        this.tables = clone;
    }

    /**
     * Requests that the <code>tableService</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTableService (TableMarshaller value)
    {
        TableMarshaller ovalue = this.tableService;
        requestAttributeChange(
            TABLE_SERVICE, value, ovalue);
        this.tableService = value;
    }

    /**
     * Requests that the <code>groupId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setGroupId (int value)
    {
        int ovalue = this.groupId;
        requestAttributeChange(
            GROUP_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.groupId = value;
    }

    /**
     * Requests that the <code>splashMedia</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setSplashMedia (MediaDesc value)
    {
        MediaDesc ovalue = this.splashMedia;
        requestAttributeChange(
            SPLASH_MEDIA, value, ovalue);
        this.splashMedia = value;
    }
    // AUTO-GENERATED: METHODS END
}
