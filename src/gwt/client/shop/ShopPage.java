//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ShopUtil;
import client.remix.ItemRemixer;
import client.remix.RemixerHost;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.util.Link;
import client.util.InfoCallback;
import client.util.ServiceUtil;

/**
 * Handles the MetaSOY inventory application.
 */
public class ShopPage extends Page
{
    public static final String LOAD_LISTING = "l";
    public static final String FAVORITES = "f";
    public static final String SUITE = "s";
    public static final String GAME = "g";
    public static final String REMIX = "r";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (action.equals(LOAD_LISTING)) {
            byte type = getItemType(args, 1, Item.NOT_A_TYPE);
            int catalogId = args.get(2, 0);
            setContent(new ListingDetailPanel(_models, type, catalogId));
            addTypeNavi(type);

        } else if (action.equals(FAVORITES)) {
            // if no member is specified, we use the current member
            int memberId = args.get(1, CShell.getMemberId());
            byte type = getItemType(args, 2, Item.NOT_A_TYPE);
            int page = args.get(3, 0);
            setContent(new FavoritesPanel(_models, memberId, type, page));

        } else if (action.equals(SUITE)) {
            final byte type = getItemType(args, 1, Item.NOT_A_TYPE);
            final int catalogId = args.get(2, 0);
            setContent(new SuiteCatalogPanel(_models, type, catalogId));
            addTypeNavi(type);

        } else if (action.equals(GAME)) {
            final int gameId = args.get(1, 0);
            setContent(new SuiteCatalogPanel(_models, gameId));
            addTypeNavi(Item.GAME);

        } else if (action.equals(REMIX)) {
            final byte type = getItemType(args, 1, Item.AVATAR);
            final int itemId = args.get(2, 0);
            final int catalogId = args.get(3, 0);
            final ItemRemixer remixer = new ItemRemixer();
            _stuffsvc.loadItem(new ItemIdent(type, itemId), new InfoCallback<Item>() {
                public void onSuccess (Item result) {
                    remixer.init(createRemixerHost(remixer, type, catalogId), result, catalogId);
                }
            });
            setContent(remixer);

        } else {
            byte type = getItemType(args, 0, Item.NOT_A_TYPE);
            if (type == Item.NOT_A_TYPE) {
                setContent(_msgs.catalogTitle(), new ShopPanel());
            } else {
                if (!_catalog.isAttached()) {
                    setContent(_catalog);
                }
                _catalog.display(ShopUtil.parseArgs(args), args.get(3, 0));
            }
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.SHOP;
    }

    protected void addTypeNavi (byte type)
    {
        CShell.frame.addNavLink(_dmsgs.xlate("pItemType" + type), Pages.SHOP, ""+type, 1);
    }

    protected RemixerHost createRemixerHost (
        final ItemRemixer remixer, final byte type, final int catalogId)
    {
        // ABTEST: 2009 03 buypanel: switched to loadTestedListing
        return new RemixerHost() {
            public void buyItem () {
                // Request the listing, re-reserving a new price for us
                _catalogsvc.loadTestedListing(
                    CShell.frame.getVisitorInfo(), "2009 03 buypanel", type, catalogId,
                    new InfoCallback<CatalogService.ListingResult>() {
                    public void onSuccess (CatalogService.ListingResult result) {
                        // and display a mini buy dialog.
                        new BuyRemixDialog(
                            result.listing, result.abTestGroup, new AsyncCallback<Item>() {
                            public void onFailure (Throwable cause) { /* not used */ }
                            public void onSuccess (Item item) {
                                remixer.itemPurchased(item);
                            }
                        });
                    }
                });
            }

            // called only when the remixer exits
            public void remixComplete (Item item) {
                if (item != null) {
                    Link.go(Pages.STUFF, "d", item.getType(), item.itemId);
                } else {
                    History.back();
                }
            }
        };
    }

    /**
     * Extracts the item type from the arguments, sanitizing it if necessary.
     */
    protected byte getItemType (Args args, int index, byte deftype)
    {
        byte type = (byte)args.get(index, deftype);
        if (Item.getClassForType(type) == null) {
            CShell.log("Rejecting invalid item type", "type", type, "args", args);
            return deftype;
        }
        return type;
    }

    protected CatalogModels _models = new CatalogModels();
    protected CatalogPanel _catalog = new CatalogPanel(_models);

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
