//
// $Id$

package client.item;

import client.util.Stars;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

public class ItemRating extends SimplePanel
{
    /**
     * Construct a new display for the given item with member's previous rating of the item,
     * automatically figuring out read-only or read/write display mode.
     */
    public ItemRating (Item item, byte memberRating)
    {
        this(item, memberRating, item.isRatable() ? Stars.MODE_BOTH : Stars.MODE_READ, false);
    }

    /**
     * Construct a new display for the given item with member's previous rating of the item and a
     * specified display mode.
     */
    public ItemRating (Item item, byte memberRating, int mode, boolean halfSize)
    {
        super();
        // sanity check
        if (mode != Stars.MODE_READ && !item.isRatable()) {
            throw new IllegalArgumentException("Can only rate clones and listed items " + _item);
        }
        setStyleName("itemRating");

        _item = item;
        _memberRating = memberRating;
        _itemId = new ItemIdent(_item.getType(), _item.getPrototypeId());

        // if we're not logged in, force MODE_READ
        setWidget(new ItemStars((CItem.ident == null) ? Stars.MODE_READ : mode, halfSize));
    }

    /** The subclass of {#link Stars} that implements most of the item rating. */
    protected class ItemStars extends Stars
    {
        protected ItemStars (int mode, boolean halfSize)
        {
            super(mode, halfSize);
        }

        // @Override
        protected void update ()
        {
            // show average rating unless we're in write-only mode
            if (_mode == MODE_WRITE) {
                updateStarImages(_memberRating, true);
            } else {
                updateStarImages(_item.rating, false);
            }
        }

        // called when we are over the widget
        // @Override
        protected void update (double rating)
        {
            // show the changing user rating as user hovers over widget, unless we're read-only
            if (_mode == MODE_READ) {
                updateStarImages(_item.rating, true);
            } else {
                updateStarImages(rating, true);
            }
        }

        // @Override
        protected void starsClicked (byte newRating)
        {
            rateItem(newRating);
        }
    
        /**
         * Performs a server call to give an item a new rating by this member.
         */
        protected void rateItem (byte newRating)
        {
            _memberRating = newRating;
            CItem.itemsvc.rateItem(CItem.ident, _itemId, newRating, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _item.rating = ((Float)result).floatValue();
                    _mode = MODE_READ;
                    update();
                }
                public void onFailure (Throwable caught) {
                    CItem.log("rateItem failed", caught);
                    // TODO: Error image?
                }
            });
        }
    }

    protected Item _item;
    protected ItemIdent _itemId;
    protected byte _memberRating;
}
