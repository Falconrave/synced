//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.CheckBox;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemPack;

/**
 * A class for creating and editing {@link ItemPack} digital items.
 */
public class ItemPackEditor extends IdentGameItemEditor
{
    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new ItemPack();
    }

    @Override // from ItemEditor
    protected void addInfo ()
    {
        super.addInfo();

        addSpacer();
        CheckBox box = new CheckBox();
        box.setValue(true);
        box.setEnabled(false);
        addRow(_emsgs.packPremium(), box);
        addTip(_emsgs.ipackPremiumTip());
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        // item packs' furni media are their primary media
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
        addRow(_emsgs.ipackLabel(), upper, _emsgs.ipackTip());
    }

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
