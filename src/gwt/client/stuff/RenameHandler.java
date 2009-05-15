//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.util.ClickCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays a dialog that handles renaming of a cloned item.
 */
public class RenameHandler extends ClickCallback<String>
{
    public RenameHandler (HasClickHandlers trigger, Item item, InventoryModels models)
    {
        super(trigger, "");

        _item = item;
        _models = models;
        _name = new TextBox();
        _name.setMaxLength(Item.MAX_NAME_LENGTH);
        _name.setVisibleLength(Item.MAX_NAME_LENGTH);
        _name.setText(_item.name);
    }

    // from ClickCallback
    @Override protected boolean callService () {
        _stuffsvc.renameClone(_item.getIdent(), _name.getText(), this);
        return true;
    }

    // from ClickCallback
    @Override protected boolean gotResult (String result) {
        _item.name = result;
        _models.itemUpdated(_item);
        // just force a reload of the detail page
        Link.replace(Pages.STUFF, "d", _item.getType(), _item.itemId,
                     _item.name.replaceAll(" ", "-"));
        return true;
    }

    // from ClickCallback
    protected void displayPopup ()
    {
        BorderedDialog dialog = new BorderedDialog(false) {
            protected void onClosed (boolean autoClosed) {
                setEnabled(true);
            }
        };

        dialog.setHeaderTitle(_msgs.renameTitle());

        SmartTable content = new SmartTable(0, 10);
        content.setWidth("300px");
        content.setText(0, 0, _msgs.renameTip());
        content.setWidget(1, 0, _name);
        dialog.setContents(content);

        dialog.addButton(new Button(_cmsgs.cancel(), dialog.onCancel()));
        dialog.addButton(new Button(_msgs.renameRevert(), dialog.onAction(new Command() {
            public void execute () {
                _name.setText("");
                takeAction(true);
            }
        })));
        dialog.addButton(new Button(_cmsgs.change(), dialog.onAction(new Command() {
            public void execute () {
                takeAction(true);
            }
        })));

        dialog.show();
    }

    protected Item _item;
    protected InventoryModels _models;
    protected TextBox _name;

    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
