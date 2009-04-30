//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ItemActivator;
import client.item.ItemUtil;
import client.money.BuyPanel;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * An interface for buying a CatalogListing. Doesn't display anything but functional buy
 * buttons.
 */
public class ItemBuyPanel extends BuyPanel<Item>
{
    /**
     * @param callback optional. Notified only on success.
     */
    // ABTEST: 2009 03 buypanel: addition of abTestGroup param
    public ItemBuyPanel (CatalogListing listing, int abTestGroup, AsyncCallback<Item> callback)
    {
        _listing = listing;
        init(listing.quote, abTestGroup, callback);
    }

    @Override
    protected boolean makePurchase (
        Currency currency, int amount, AsyncCallback<PurchaseResult<Item>> listener)
    {
        if (CShell.isGuest()) {
            MsoyUI.infoAction(_msgs.msgMustRegister(), _msgs.msgRegister(),
                Link.createListener(Pages.ACCOUNT, "create"));
            return false;

        } else {
            _catalogsvc.purchaseItem(_listing.detail.item.getType(), _listing.catalogId,
                currency, amount, ItemUtil.getMemories(), listener);
            return true;
        }
    }

    @Override
    protected void addPurchasedUI (Item item, FlowPanel boughtPanel)
    {
        byte itype = item.getType();

        // change the buy button into a "you bought it" display
        String type = _dmsgs.xlate("itemType" + itype);
        boughtPanel.add(MsoyUI.createLabel(_msgs.boughtTitle(type), "Title"));

        if (FlashClients.clientExists()) {
            if (item instanceof SubItem) {
                boughtPanel.add(WidgetUtil.makeShim(10, 10));
                boughtPanel.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.boughtBackTo(),
                    new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        CShell.frame.closeContent();
                    }
                }));
            } else {
                boughtPanel.add(new ItemActivator(item, true));
                boughtPanel.add(new Label(getUsageMessage(itype)));
            }

        } else {
            boughtPanel.add(new Label(_msgs.boughtViewStuff(type)));
            String ptype = _dmsgs.xlate("pItemType" + itype);
            boughtPanel.add(Link.create(_msgs.boughtGoNow(ptype), Pages.STUFF, ""+itype));
        }
    }

    protected static String getUsageMessage (byte itemType)
    {
        if (itemType == Item.AVATAR) {
            return _msgs.boughtAvatarUsage();
        } else if (itemType == Item.DECOR) {
            return _msgs.boughtDecorUsage();
        } else if (itemType == Item.AUDIO) {
            return _msgs.boughtAudioUsage();
        } else if (itemType == Item.PET) {
            return _msgs.boughtPetUsage();
        } else {
            return _msgs.boughtOtherUsage();
        }
    }

    protected CatalogListing _listing;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
