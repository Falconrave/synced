//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.LevelPack;

/**
 * A class for creating and editing {@link LevelPack} digital items.
 */
public class LevelPackEditor extends IdentGameItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _pack = (LevelPack)item;
        _premium.setValue(_pack.premium);
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new LevelPack();
    }

    @Override // from ItemEditor
    protected void addInfo ()
    {
        super.addInfo();

        addSpacer();
        addRow(_emsgs.packPremium(), _premium = new CheckBox());
        addTip(_emsgs.lpackPremiumTip());
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        // level packs' furni media are their primary media
        addSpacer();
        ItemMediaUploader upper = createUploader(
            Item.FURNI_MEDIA, TYPE_ANY, ItemMediaUploader.MODE_NORMAL, new MediaUpdater() {
            public String updateMedia (String name, MediaDesc desc, int width, int height) {
                // TODO: validate media type
                _item.setFurniMedia(desc);
                return null;
            }
            public void clearMedia () {
                _item.setFurniMedia(null);
            }
        });
        addRow(_emsgs.lpackLabel(), upper, _emsgs.lpackTip());
    }

    @Override // from ItemEditor
    protected void prepareItem ()
        throws Exception
    {
        super.prepareItem();
        _pack.premium = _premium.getValue();
    }

    protected LevelPack _pack;
    protected CheckBox _premium;

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
