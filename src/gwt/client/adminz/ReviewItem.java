//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.BorderedDialog;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.ClickCallback;
import client.util.Link;

/**
 * Displays an item to be reviewed.
 * TODO: i18n
 */
public class ReviewItem extends FlowPanel
{
    public ReviewItem (ReviewPanel parent, ItemDetail detail, ItemFlag flag)
    {
        _parent = parent;
        _item = detail != null ? detail.item : null;
        _ident = flag.itemIdent;

        byte type = flag.itemIdent.type;
        int id = flag.itemIdent.itemId;

        // the name displays an item inspector and the creator name links to profile
        HorizontalPanel nameCreator = new HorizontalPanel();
        nameCreator.setSpacing(4);
        String itemName = _item != null ? _item.name : "Unknown";
        nameCreator.add(Link.create(itemName, Pages.STUFF, "d", type, id));
        nameCreator.add(MsoyUI.createLabel(" by ", null));
        if (detail != null) {
            nameCreator.add(Link.memberView(detail.creator));
        }
        add(nameCreator);

        // say what flags are set on it
        FlowPanel flaggedAs = new FlowPanel();
        flaggedAs.add(new InlineLabel("Flagged as:"));
        flaggedAs.add(new InlineLabel(flag.kind.toString(), false, true, false));
        add(flaggedAs);

        // transactions link
        add(Link.create("Transactions", Pages.ADMINZ, "review", type, id));

        // then a row of action buttons
        RowPanel line = new RowPanel();

//             // TODO: Let's nix 'delist' for a bit and see if we need it later.
//             if (item.ownerId == 0) {
//                 Button button = new Button("Delist");
//                 new ClickCallback<Integer>(button) {
//                     @Override protected boolean callService () {
//                         _catalogsvc.listItem(item.getIdent(), false, this);
//                         return true;
//                     }
//                     @Override protected boolean gotResult (Integer result) {
//                         if (result != null) {
//                             MsoyUI.info(_msgs.reviewDelisted());
//                             return false; // don't reenable delist
//                         }
//                         MsoyUI.error(_msgs.errListingNotFound());
//                         return true;
//                     }
//                 };
//                 line.add(button);
//             }

        // a button to mark someting as mature
        if (flag.kind == ItemFlag.Kind.MATURE) {
            _mark = new Button(_msgs.reviewMark());
            new ClickCallback<Void>(_mark) {
                @Override protected boolean callService () {
                    if (_item == null) {
                        // should not happen, but let's be careful
                        return false;
                    }
                    _itemsvc.setMature(_item.getIdent(), true, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.reviewMarked());
                    return false; // don't reenable button
                }
            };
            line.add(_mark);
        }

        // a button to delete an item and possibly all its clones
        if (_item != null) {
            _delete = new Button(_item.ownerId != 0 ?
                                 _msgs.reviewDelete() : _msgs.reviewDeleteAll());
            _delete.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    if (_item == null) {
                        // should not happen, but let's be careful
                        return;
                    }
                    new DeleteDialog().show();
                }
            });
            line.add(_delete);
        }

        // a button to signal we're done
        _done = new Button(_msgs.reviewDone());
        new ClickCallback<Void>(_done) {
            @Override protected boolean callService () {
                if (_ident == null) {
                    _parent.refresh();
                    return false;
                }
                _itemsvc.removeAllFlags(_ident, this);
                return true;
            }
            @Override protected boolean gotResult (Void result) {
                // the flags are set: refresh the UI
                _parent.refresh();
                // keep the button disabled until the UI refreshes
                return false;
            }
        };
        line.add(_done);
        add(line);
    }

    /**
     * Handle the deletion message and prompt.
     */
    protected class DeleteDialog extends BorderedDialog
        implements KeyUpHandler
    {
        public DeleteDialog ()
        {
            setHeaderTitle(_msgs.reviewDeletionTitle());

            VerticalPanel contents = new VerticalPanel();
            contents.setSpacing(10);
            contents.setWidth("500px");
            contents.add(MsoyUI.createLabel(_msgs.reviewDeletionPrompt(), null));
            contents.add(_area = MsoyUI.createTextArea("", 50, 4));
            _area.addKeyUpHandler(this);
            setContents(contents);

            addButton(_yesButton = new Button(_msgs.reviewDeletionDo(), new ClickHandler () {
                public void onClick (ClickEvent event) {
                    doDelete();
                    hide();
                }
            }));
            _yesButton.setEnabled(false);

            addButton(new Button(_msgs.reviewDeletionDont(), new ClickHandler () {
                public void onClick (ClickEvent event) {
                    hide();
                }
            }));

            show();
        }

        public void onKeyUp (KeyUpEvent event)
        {
            _yesButton.setEnabled(_area.getText().trim().length() > 0);
        }

        protected void doDelete ()
        {
            if (!_yesButton.isEnabled()) {
                return; // you just never know
            }

            _adminsvc.deleteItemAdmin(
                _item.getIdent(), _msgs.reviewDeletionMailHeader(),
                _msgs.reviewDeletionMailMessage(_item.name, _area.getText().trim()),
                new AsyncCallback<AdminService.ItemDeletionResult>() {
                    public void onSuccess (AdminService.ItemDeletionResult result) {
                        MsoyUI.info(_msgs.reviewDeletionSuccess(
                            String.valueOf(result.deletionCount), String.valueOf(result.refunds),
                            String.valueOf(result.reclaimCount),
                            String.valueOf(result.reclaimErrors)));
                        if (_mark != null) {
                            _mark.setEnabled(false);
                        }
                        _delete.setEnabled(false);
                        _item = null;
                        _ident = null;
                        hide();
                    }
                    public void onFailure (Throwable caught) {
                        MsoyUI.error(_msgs.reviewErrDeletionFailed(caught.getMessage()));
                        if (_mark != null) {
                            _mark.setEnabled(true);
                        }
                        _delete.setEnabled(true);
                        hide();
                    }
                });
        }

        protected TextArea _area;
        protected Button _yesButton;
    }

    protected ReviewPanel _parent;
    protected Item _item;
    protected ItemIdent _ident;
    protected Button _mark, _delete, _done;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
    protected static final ItemServiceAsync _itemsvc = GWT.create(ItemService.class);
    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
}
