//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.admin.data.ConfigObject;

/**
 * Contains runtime configurable costs.
 *
 * To interpret the value of a cost, use RuntimeConfig.getCoinCost();
 * (Each cost is specified in coins, 0 for free, or a negative number to peg it to the value
 * of the magnitude of bars.)
 */
public class CostsConfigObject extends ConfigObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>newRoom</code> field. */
    public static final String NEW_ROOM = "newRoom";

    /** The field name of the <code>newGroup</code> field. */
    public static final String NEW_GROUP = "newGroup";

    /** The field name of the <code>newTheme</code> field. */
    public static final String NEW_THEME = "newTheme";

    /** The field name of the <code>broadcastBase</code> field. */
    public static final String BROADCAST_BASE = "broadcastBase";

    /** The field name of the <code>broadcastIncrement</code> field. */
    public static final String BROADCAST_INCREMENT = "broadcastIncrement";

    /** The field name of the <code>startParty</code> field. */
    public static final String START_PARTY = "startParty";
    // AUTO-GENERATED: FIELDS END

    /** The cost of a new room. */
    public int newRoom = -1;

    /** The cost of new group. */
    public int newGroup = -3;

    /** The cost of new theme. */
    public int newTheme = -100;

    /** The base cost of a paid broadcast. */
    public int broadcastBase = -10;

    /** The increment for each recent broadcast. */
    public int broadcastIncrement = -4;

    /** The cost to start a party. */
    public int startParty = 2000;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>newRoom</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNewRoom (int value)
    {
        int ovalue = this.newRoom;
        requestAttributeChange(
            NEW_ROOM, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newRoom = value;
    }

    /**
     * Requests that the <code>newGroup</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNewGroup (int value)
    {
        int ovalue = this.newGroup;
        requestAttributeChange(
            NEW_GROUP, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newGroup = value;
    }

    /**
     * Requests that the <code>newTheme</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setNewTheme (int value)
    {
        int ovalue = this.newTheme;
        requestAttributeChange(
            NEW_THEME, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.newTheme = value;
    }

    /**
     * Requests that the <code>broadcastBase</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBroadcastBase (int value)
    {
        int ovalue = this.broadcastBase;
        requestAttributeChange(
            BROADCAST_BASE, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.broadcastBase = value;
    }

    /**
     * Requests that the <code>broadcastIncrement</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBroadcastIncrement (int value)
    {
        int ovalue = this.broadcastIncrement;
        requestAttributeChange(
            BROADCAST_INCREMENT, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.broadcastIncrement = value;
    }

    /**
     * Requests that the <code>startParty</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setStartParty (int value)
    {
        int ovalue = this.startParty;
        requestAttributeChange(
            START_PARTY, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.startParty = value;
    }
    // AUTO-GENERATED: METHODS END
}
