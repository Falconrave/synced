//
// $Id$

package client.item;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;

import com.samskivert.depot.util.ByteEnumUtil;

import com.threerings.msoy.item.data.all.Item;

import client.item.ItemMessages;
import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.events.FlashEvents;
import client.util.events.ItemUsageEvent;
import client.util.events.ItemUsageListener;

/**
 * Displays an interface for activating an item (wearing an avatar, adding furni to a room, etc.).
 * This should only be added if the Flash client is known to exist: {@link
 * FlashClients#clientExists}.
 */
public class ItemActivator extends FlowPanel
    implements ItemUsageListener
{
    public ItemActivator (Item item, boolean bigAss)
    {
        setStyleName("itemActivator");
        _bigAss = bigAss;
        setItem(item);
    }

    public void setItem (Item item)
    {
        _item = item;
        update(_item.used, _item.location);
    }

    // from ItemUsageListener
    public void itemUsageChanged (ItemUsageEvent event)
    {
        if ((_item != null) && (_item.getType() == event.getItemType()) &&
                (_item.itemId == event.getItemId())) {
            update(ByteEnumUtil.fromByte(Item.UsedAs.class, event.getUsage()), event.getLocation());
        }
    }

    @Override // from Panel
    protected void onAttach ()
    {
        super.onAttach();

        FlashEvents.addListener(this);
        update(_item.used, _item.location);
    }

    @Override // from Panel
    protected void onDetach ()
    {
        super.onDetach();

        FlashEvents.removeListener(this);
    }

    protected void update (Item.UsedAs usedAs, int location)
    {
        // TODO: do this in one place?
        boolean hasClient = FlashClients.clientExists();
        boolean isUsed = usedAs.forAnything();
        boolean usedHere;

        switch (usedAs) {
        default:
            usedHere = false;
            break;

        case AVATAR:
            usedHere = isUsed;
            break;

        case FURNITURE:
        case PET:
        case BACKGROUND:
            // TODO: getSceneId out so it's retrieved in one place and shared?
            usedHere = hasClient && (location == FlashClients.getSceneId());
            break;
        }

        clear();

        String suff = isUsed ? "used.png" : "unused.png";
        String tip = null;
        String path;
        ClickHandler onClick = null;

        if (!hasClient) {
            tip = isUsed ? _imsgs.inUse() : _imsgs.notInUse();
        }

        byte type = _item.getType();
        final boolean fUsedHere = usedHere;
        if (type == Item.AVATAR) {
            if (hasClient) {
                tip = usedHere ? _imsgs.removeAvatar() : _imsgs.wearAvatar();
                onClick = new ClickHandler () {
                    public void onClick (ClickEvent event) {
                        if (fUsedHere) {
                            FlashClients.useAvatar(0);
                        } else {
                            FlashClients.useAvatar(_item.itemId);
                        }
                    }
                };
            }
            path = "/images/ui/checkbox_avatar_" + suff;

        } else {
            if (hasClient) {
                if (usedHere) {
                    tip = _imsgs.removeFromRoom();
                    suff = "usedhere.png";
                } else if (isUsed) {
                    tip = _imsgs.moveToRoom();
                } else {
                    tip = _imsgs.addToRoom();
                }
                onClick = new ClickHandler () {
                    public void onClick (ClickEvent event) {
                        if (fUsedHere) {
                            FlashClients.clearItem(_item.getType(), _item.itemId);
                        } else {
                            FlashClients.useItem(_item.getType(), _item.itemId);
                            // CItem.frame.closeContent();
                        }
                    }
                };
            }
            path = "/images/ui/checkbox_room_" + suff;
        }

        if (_bigAss) {
            add(MsoyUI.createButton(MsoyUI.LONG_THIN, tip, onClick));
        } else {
            add(MsoyUI.createActionImage(path, onClick));
            add(MsoyUI.createActionLabel(tip, "Tip", onClick));
        }
    }

    protected boolean _bigAss;
    protected Item _item;

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
}
