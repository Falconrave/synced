//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.web.client.DeploymentConfig;

import client.shell.Args;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override from Page
    public void onHistoryChanged (Args args)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CShop.ident == null) {
            setContent(MsoyUI.createLabel(CShop.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        String action = args.get(0, "");
        if (action.equals("l")) {
            byte type = (byte)args.get(1, Item.NOT_A_TYPE);
            int catalogId = args.get(2, 0);
            CShop.catalogsvc.loadListing(CShop.ident, type, catalogId, new MsoyCallback() {
                public void onSuccess (Object result) {
                    setContent(new ListingDetailPanel(_models, (CatalogListing)result));
                }
            });

        } else {
            byte type = (byte)args.get(0, Item.NOT_A_TYPE);
            if (type == Item.NOT_A_TYPE) {
                setContent(CShop.msgs.catalogTitle(), new ShopPanel());

            } else {
                if (!_catalog.isAttached()) {
                    setContent(_catalog);
                }
                _catalog.display(CShop.parseArgs(args), args.get(3, 0));
            }
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return SHOP;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CShop.msgs = (ShopMessages)GWT.create(ShopMessages.class);
    }

    protected CatalogModels _models = new CatalogModels();
    protected CatalogPanel _catalog = new CatalogPanel(_models);
}
