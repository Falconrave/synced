//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import client.util.BorderedDialog;
import client.util.MsoyUI;

/**
 * Handles resetting of a user's password.
 */
public class ResetPasswordDialog extends BorderedDialog
{
    public static void display (String args)
    {
        try {
            int uidx = args.indexOf("_");
            if (uidx != -1) {
                new ResetPasswordDialog(Integer.parseInt(args.substring(0, uidx)),
                                        args.substring(uidx+1)).show();
                return;
            }
        } catch (Exception e) {
            // fall through and fail
        }
        MsoyUI.error(CShell.cmsgs.resetInvalid());
    }

    // @Override // from BorderedDialog
    public Widget createContents ()
    {
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(10);
        contents.setStyleName("formDialog");
        return contents;
    }

    protected ResetPasswordDialog (int memberId, String code) 
    {
        _memberId = memberId;
        _code = code;
        _header.add(createTitleLabel(CShell.cmsgs.resetTitle(), null));

        FlexTable contents = (FlexTable)_contents;

        int row = 0;
        contents.getFlexCellFormatter().setColSpan(row, 0, 2);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Intro");
        contents.setText(row++, 0, CShell.cmsgs.resetIntro());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.resetPassword());
        contents.setWidget(row++, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_validator);

        ClickListener submit = new ClickListener() {
            public void onClick (Widget sender) {
                sendResetRequest();
            }
        };

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.resetConfirm());
        contents.setWidget(row++, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(new EnterClickAdapter(submit));
        _confirm.addKeyboardListener(_validator);

        contents.getFlexCellFormatter().setColSpan(row, 0, 2);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Status");
        contents.setWidget(row++, 0, _status = new Label(""));
        _status.setText(CShell.cmsgs.createMissingPassword());

        _footer.add(_submit = new Button(CShell.cmsgs.resetSubmit(), submit));
        _footer.add(new Button(CShell.cmsgs.dismiss(), new ClickListener() {
            public void onClick (Widget widget) {
                ResetPasswordDialog.this.hide();
            }   
        }));
    }

    protected void validateData ()
    {
        boolean valid = false;
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (password.length() == 0) {
            _status.setText(CShell.cmsgs.resetMissingPassword());
        } else if (confirm.length() == 0) {
            _status.setText(CShell.cmsgs.resetMissingConfirm());
        } else if (!password.equals(confirm)) {
            _status.setText(CShell.cmsgs.resetPasswordMismatch());
        } else {
            _status.setText(CShell.cmsgs.resetReady());
            valid = true;
        }
        _submit.setEnabled(valid);
    }

    protected void sendResetRequest ()
    {
        String password = CShell.md5hex(_password.getText().trim());
        CShell.usersvc.resetPassword(_memberId, _code, password, new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
                if (((Boolean)result).booleanValue()) {
                    History.newItem("world"); // send them to the world
                    MsoyUI.info(CShell.cmsgs.resetReset());
                } else {
                    MsoyUI.error(CShell.cmsgs.resetInvalid());
                }
            }
            public void onFailure (Throwable caught) {
                _status.setText(CShell.serverError(caught));
            }
        });
    }

    protected KeyboardListener _validator = new KeyboardListenerAdapter() {
        public void onKeyPress (Widget sender, char keyCode, int modifiers) {
            // let the keypress go through, then validate our data
            DeferredCommand.add(new Command() {
                public void execute () {
                    validateData();
                }
            });
        }
    };

    protected int _memberId;
    protected String _code;
    protected PasswordTextBox _password, _confirm;
    protected Button _submit;
    protected Label _status;
}
