//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.item.gwt.ListingCard;

import client.item.ListingBox;
import client.ui.MiniNowLoadingWidget;
import client.ui.MsoyUI;

/**
 * Displays a paged grid of catalog listings.
 */
public abstract class ListingGrid extends PagedGrid<ListingCard>
{
    public ListingGrid (int headerHeight)
    {
        this(MsoyUI.computeRows(headerHeight+NAV_BAR_ETC, BOX_HEIGHT, 2), COLUMNS);
    }

    public ListingGrid (int rows, int columns)
    {
        super(rows, columns);
        addStyleName("listingGrid");
    }

    @Override // from PagedGrid
    protected Widget createWidget (ListingCard card)
    {
        return ListingBox.newBox(card);
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    @Override// from PagedWidget
    protected Widget getNowLoadingWidget ()
    {
        return new MiniNowLoadingWidget();
    }

    /** The number of columns of items to display. */
    protected static final int COLUMNS = 4;

    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BOX_HEIGHT = MediaDescSize.THUMBNAIL_HEIGHT + 20 /* border */ +
        15 /* name */ + 20 /* creator */ + 20 /* rating/price */;
}
