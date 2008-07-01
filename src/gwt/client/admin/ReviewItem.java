//
// $Id$

package client.admin;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.BorderedDialog;
import client.util.ClickCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays an item to be reviewed.
 */
public class ReviewItem extends FlowPanel
{
    public ReviewItem (ReviewPanel parent, ItemDetail detail)
    {
        _parent = parent;
        _item = detail.item;

        // say what flags are set on it
        FlowPanel flaggedAs = new FlowPanel();
        flaggedAs.add(new InlineLabel("Flagged as:"));
        if (_item.isFlagSet(Item.FLAG_FLAGGED_MATURE)) {
            flaggedAs.add(new InlineLabel("Mature", false, true, false));
        }
        if (_item.isFlagSet(Item.FLAG_FLAGGED_COPYRIGHT)) {
            flaggedAs.add(new InlineLabel("Copyright Violation", false, true, false));
        }
        add(flaggedAs);

        // the name displays an item inspector
        String name = _item.name + " - " + detail.creator.toString();
        String args = Args.compose("d", ""+_item.getType(), ""+_item.itemId);
        add(Application.createLink(name, Page.STUFF, args));

        add(MsoyUI.createLabel(_item.description, null));

        // then a row of action buttons
        RowPanel line = new RowPanel();

//             // TODO: Let's nix 'delist' for a bit and see if we need it later.
//             if (item.ownerId == 0) {
//                 Button button = new Button("Delist");
//                 new ClickCallback<Integer>(button) {
//                     public boolean callService () {
//                         CAdmin.catalogsvc.listItem(CAdmin.ident, item.getIdent(), false, this);
//                         return true;
//                     }
//                     public boolean gotResult (Integer result) {
//                         if (result != null) {
//                             MsoyUI.info(CAdmin.msgs.reviewDelisted());
//                             return false; // don't reenable delist
//                         }
//                         MsoyUI.error(CAdmin.msgs.errListingNotFound());
//                         return true;
//                     }
//                 };
//                 line.add(button);
//             }

        // a button to mark someting as mature
        if (_item.isFlagSet(Item.FLAG_FLAGGED_MATURE)) {
            _mark = new Button(CAdmin.msgs.reviewMark());
            new ClickCallback<Void>(_mark) {
                public boolean callService () {
                    if (_item == null) {
                        // should not happen, but let's be careful
                        return false;
                    }
                    CAdmin.itemsvc.setMature(CAdmin.ident, _item.getIdent(), true, this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(CAdmin.msgs.reviewMarked());
                    return false; // don't reenable button
                }
            };
            line.add(_mark);
        }

        // a button to delete an item and possibly all its clones
        _delete = new Button(_item.ownerId != 0 ?
                             CAdmin.msgs.reviewDelete() : CAdmin.msgs.reviewDeleteAll());
        _delete.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                if (_item == null) {
                    // should not happen, but let's be careful
                    return;
                }
                new DeleteDialog().show();
            }
        });
        line.add(_delete);

        // a button to signal we're done
        _done = new Button(CAdmin.msgs.reviewDone());
        new ClickCallback<Void>(_done) {
            public boolean callService () {
                if (_item == null) {
                    _parent.refresh();
                    return false;
                }
                byte flags = (byte) (Item.FLAG_FLAGGED_COPYRIGHT | Item.FLAG_FLAGGED_MATURE);
                CAdmin.itemsvc.setFlags(CAdmin.ident, _item.getIdent(), flags, (byte) 0, this);
                return true;
            }
            public boolean gotResult (Void result) {
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
        implements KeyboardListener
    {
        public DeleteDialog ()
        {
            setHeaderTitle(CAdmin.msgs.reviewDeletionTitle());

            VerticalPanel contents = new VerticalPanel();
            contents.setSpacing(10);
            contents.setWidth("500px");
            contents.add(MsoyUI.createLabel(CAdmin.msgs.reviewDeletionPrompt(), null));
            contents.add(_area = MsoyUI.createTextArea("", 50, 4));
            _area.addKeyboardListener(this);
            setContents(contents);

            addButton(_yesButton = new Button(CAdmin.msgs.reviewDeletionDo(), new ClickListener () {
                public void onClick (Widget sender) {
                    doDelete();
                    hide();
                }
            }));
            _yesButton.setEnabled(false);

            addButton(new Button(CAdmin.msgs.reviewDeletionDont(), new ClickListener () {
                public void onClick (Widget sender) {
                    hide();
                }
            }));

            show();
        }

        public void onKeyDown (Widget sender, char keyCode, int modifiers) { /* empty*/ }
        public void onKeyPress (Widget sender, char keyCode, int modifiers) { /* empty */ }
        public void onKeyUp (Widget sender, char keyCode, int modifiers)
        {
            _yesButton.setEnabled(_area.getText().trim().length() > 0);
        }

        protected void doDelete ()
        {
            if (!_yesButton.isEnabled()) {
                return; // you just never know
            }

            CAdmin.itemsvc.deleteItemAdmin(
                CAdmin.ident, _item.getIdent(), CAdmin.msgs.reviewDeletionMailHeader(),
                CAdmin.msgs.reviewDeletionMailMessage(_item.name, _area.getText().trim()),
                new AsyncCallback<Integer>() {
                    public void onSuccess (Integer result) {
                        MsoyUI.info(CAdmin.msgs.reviewDeletionSuccess(result.toString()));
                        if (_mark != null) {
                            _mark.setEnabled(false);
                        }
                        _delete.setEnabled(false);
                        _item = null;
                        hide();
                    }
                    public void onFailure (Throwable caught) {
                        MsoyUI.error(CAdmin.msgs.reviewErrDeletionFailed(caught.getMessage()));
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
    protected Button _mark, _delete, _done;
}
