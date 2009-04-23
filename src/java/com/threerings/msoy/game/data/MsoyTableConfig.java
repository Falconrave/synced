//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.parlor.data.TableConfig;

/**
 * A table configuration with Msoy-specific extras.
 */
public class MsoyTableConfig extends TableConfig
{
    /** The display text for this table. */
    public String title;

    /** The required partyId for joining this table, or 0. */
    public int partyId;
}
