//
// $Id$

package com.threerings.msoy.money.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information about a user's current bling status.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class BlingInfo
    implements IsSerializable
{
    /** An amount of centibling. */
    public int bling;

    /** The worth in US pennies. */
    public int worthPerBling;

    /** The minimum amount of centibling required to cash out. */
    public int minCashOutBling;

    /** Information about the cash out request currently in progress, or null if no request is
     * active. */
    public CashOutInfo cashOut;

    /** If the most recent cashout was too recently requested, indicates when the next request may
     * be submitted (in milliseconds). */
    public long timeToNextRequest;

    public BlingInfo (
        int bling, int worthPerBling, int minCashOutBling, long waitTime, CashOutInfo cashOut)
    {
        this.bling = bling;
        this.worthPerBling = worthPerBling;
        this.minCashOutBling = minCashOutBling;
        this.cashOut = cashOut;
        this.timeToNextRequest = waitTime;
    }

    /** For serialization purposes. */
    public BlingInfo () { }

    public static final int CASHOUT_DAYS = 30;
}
