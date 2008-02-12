//
// $Id$

package client.stuff;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;

import client.util.MsoyCallback;

/**
 * Displays a set of sub-items on an item's detail page.
 */
public class SubItemPanel extends PagedGrid
{
    public SubItemPanel (InventoryModels models, byte type, Item parent, final ItemPanel panel)
    {
        super(ROWS, ItemPanel.COLUMNS, PagedGrid.NAV_ON_BOTTOM);
        addStyleName("subInventoryContents");

        _models = models;
        _type = type;
        _parent = parent;

        // if our parent is an original item, allow creation of subitems
        _create.setVisible(_parent.sourceId == 0);
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || hasModel()) {
            return;
        }

        _models.loadModel(_type, _parent.getSuiteId(), new MsoyCallback() {
            public void onSuccess (Object result) {
                setModel((DataModel)result, 0);
            }
        });
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new SubItemEntry((Item)item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CStuff.msgs.panelNoItems(CStuff.dmsgs.getString("itemType" + _type));
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    // @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        controls.setWidget(0, 0, _create = new Button(CStuff.msgs.panelCreateNew()));
        _create.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                CStuff.createItem(_type, _parent.getType(), _parent.itemId);
            }
        });
    }

    protected InventoryModels _models;
    protected byte _type;
    protected Item _parent;
    protected Button _create;

    protected static final int ROWS = 5;
}
