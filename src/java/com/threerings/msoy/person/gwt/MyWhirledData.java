//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.person.data.all.FeedMessage;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Promotion;

/**
 * Contains the data that we need for the My Whirled views.
 */
public class MyWhirledData
    implements IsSerializable
{
    /** Contains summary information on a particular game genre. */
    public static class FeedCategory
        implements IsSerializable
    {
        /** How many feed messages to list by default in each category */
        public static final int DEFAULT_COUNT = 3;

        /** How many feed messages to list in a category when "show more" is clicked */
        public static final int FULL_COUNT = 50;

        /** The category of feed item - see FeedMessageType.Category. */
        public int category;

        /** The highlighted games in this genre. */
        public FeedMessage[] messages;
    }

    /** The total number of people online. */
    public int whirledPopulation;

    /** This member's total friend count (on and offline). */
    public int friendCount;

    /** Promotions to display on the My Whirled page. */
    public List<Promotion> promos;

    /**
     * This member's online friends.
     */
    public List<MemberCard> friends;

    /**
     * Online greeters.
     */
    public List<MemberCard> greeters;

    /**
     * This member's recent feed messages broken up by category.
     */
    public List<FeedCategory> feed;
}
