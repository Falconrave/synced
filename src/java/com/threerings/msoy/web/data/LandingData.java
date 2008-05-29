//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains the data for the new visitor landing page.
 */
public class LandingData
    implements IsSerializable
{
    /** Currently featured whirleds */
    public GroupCard[] featuredWhirleds;

    /** Top featured game information */
    public FeaturedGameInfo[] topGames;

    /** Top featured avatar information */
    public ListingCard[] topAvatars;
}
