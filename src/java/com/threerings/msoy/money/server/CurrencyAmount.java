//
// $Id$

package com.threerings.msoy.money.server;

import com.threerings.msoy.money.data.all.Currency;

/**
 * Tracks a Currency and an amount.
 */
public class CurrencyAmount
{
    /** The currency. */
    public Currency currency;

    /** The amount of the currency. */
    public int amount;

    /**
     * Construct a CurrencyAmount.
     */
    public CurrencyAmount (Currency currency, int amount)
    {
        this.currency = currency;
        this.amount = amount;
    }

    // from Object
    public String toString ()
    {
        return String.valueOf(amount) + currency;
    }
}
