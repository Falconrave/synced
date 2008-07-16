//
// $Id$

package com.threerings.msoy.client {

import com.threerings.util.Config;
import com.threerings.util.Log;

import com.threerings.msoy.data.all.ReferralInfo;  

/**
 * Wrapper that stores and loads up tracking information: parameters from the referral,
 * and the unique tracking number.
 */
public class TrackingCookie
{
    /**
     * Does the Flash cookie already contains referral information?
     */
    public static function contains () :Boolean
    {
        return (config.getValue(AFFILIATE_ID, null) != null);
    }

    /**
     * Retrieves referral information. Returns null if one has not been set.
     */
    public static function get () :ReferralInfo
    {
        if (! contains()) {
            return null;
        }
        
        var aff :String = config.getValue(AFFILIATE_ID, null) as String;
        var vec :String = config.getValue(VECTOR_ID, null) as String;
        var cre :String = config.getValue(CREATIVE_ID, null) as String;
        var tra :String = config.getValue(TRACKER_ID, null) as String;
        var ref :ReferralInfo = ReferralInfo.makeInstance(aff, vec, cre, tra);

        log.debug("Loaded referral info: " + ref);
        return ref;
    }

    /**
     * Saves referral information in the Flash cookie.
     *
     * This function will only overwrite old data if the /overwrite/ flag is set to true.
     * Referral infos should only be saved if they don't already exist, or if there's
     * an authoritative version coming from the server.
     */
    public static function save (referral :ReferralInfo, overwrite :Boolean) :void
    {
        if (contains() && !overwrite) {
            return; 
        }

        config.setValue(AFFILIATE_ID, referral.affiliate);
        config.setValue(VECTOR_ID, referral.vector);
        config.setValue(CREATIVE_ID, referral.creative);
        config.setValue(TRACKER_ID, referral.tracker);

        log.debug("Saved referral info: " + referral);
    }

    /**
     * Completely clears the Flash cookie. Used when a registered player is logging off.
     */
    public static function clear () :void
    {
        const ids :Array = [ AFFILIATE_ID, VECTOR_ID, CREATIVE_ID, TRACKER_ID ];
        for each (var id :String in ids) {
            config.setValue(id, null);
        }

        log.debug("Cleared referral info.");
    }

    /** The underlying config object used to store tracking info. */
    protected static const config :Config = new Config("rsrc/config/msoy/affiliate");

    /** Logging logger. */
    protected static const log :Log = Log.getLog(TrackingCookie);

    protected static const AFFILIATE_ID :String = "aff";
    protected static const VECTOR_ID :String = "vec";
    protected static const CREATIVE_ID :String = "cre";
    protected static const TRACKER_ID :String = "grp";
}
}
