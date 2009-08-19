//
// $Id$

package com.threerings.msoy.money.data.all {

import com.threerings.util.ByteEnum;
import com.threerings.util.Enum;
import com.threerings.util.MessageBundle;
import com.threerings.util.StringUtil;

import com.threerings.msoy.client.DeploymentConfig;

public final class Currency extends ByteEnum
{
    /** Coins are awarded from actions in Whirled and can be used to purchase some items. */
    public static const COINS :Currency = new Currency("COINS", 0);

    /**
     * Bars are usually purchased for some real money amount and may be required to purchase some
     * items.
     */
    public static const BARS :Currency = new Currency("BARS", 1);

    /**
     * Bling is awarded when other players purchase or use some content created by a content
     * creator. It can be exchanged for real money.
     */
    public static const BLING :Currency = new Currency("BLING", 2);
    finishedEnumerating(Currency);

    /** @private this is an enum */
    public function Currency (name :String, code :int)
    {
        super(name, code);
    }

    /**
     * Get the complete URL for the "embed header" icon for this currency.
     */
    public function getEmbedHeaderIcon () :String
    {
        return DeploymentConfig.serverURL + "rsrc/" + toString().toLowerCase() + "_embedheader.png";
    }

    /**
     * Format a currency value.
     */
    public function format (value :int) :String
    {
        var postfix :String = "";
        if (this == BLING) {
            const cents :int = Math.abs(value % 100);
            value = int(value / 100);
            postfix = "." + int(cents / 10) + (cents % 10); // always print two decimal places
        }

        return StringUtil.formatNumber(value) + postfix;
    }

    /**
     * Compose an amount of this currency into a translatable String.
     */
    public function compose (value :int) :String
    {
        var key :String = "m." + toString().toLowerCase();
        if (this != BLING) { // bling doesn't pluralize
            switch (value) {
            case 0:
                key += ".0";
                break;

            case 1:
                key += ".1";
                break;

            default:
                key += ".n";
                break;
            }
        }

        return MessageBundle.tcompose(key, format(value));
    }

    /**
     * Get an array of all the Currency values.
     */
    public static function values () :Array
    {
        return Enum.values(Currency);
    }
}
}
