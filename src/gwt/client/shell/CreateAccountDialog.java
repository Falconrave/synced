//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.msoy.web.data.WebCreds;

import client.util.BorderedDialog;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountDialog extends BorderedDialog
{
    public CreateAccountDialog (StatusPanel parent)
    {
        _parent = parent;
        _header.add(createTitleLabel(CShell.cmsgs.createTitle(), null));
        _footer.add(_go = new Button(CShell.cmsgs.createCreate(), new ClickListener() {
            public void onClick (Widget sender) {
                createAccount();
            }
        }));
        _go.setEnabled(false);

        FlexTable contents = (FlexTable)_contents;
        int row = 0;
        contents.getFlexCellFormatter().setColSpan(row, 0, 2);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Intro");
        contents.setText(row++, 0, CShell.cmsgs.createIntro());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createEmail());
        contents.setWidget(row++, 1, _email = new TextBox());
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));
        _email.addKeyboardListener(_validator);
        contents.getFlexCellFormatter().setColSpan(row, 0, 2);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Tip");
        contents.setText(row++, 0, CShell.cmsgs.createEmailTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createPassword());
        contents.setWidget(row++, 1, _password = new PasswordTextBox());
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_validator);

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createConfirm());
        contents.setWidget(row++, 1, _confirm = new PasswordTextBox());
        _confirm.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _name.setFocus(true);
            }
        }));
        _confirm.addKeyboardListener(_validator);
        contents.getFlexCellFormatter().setColSpan(row, 0, 2);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Tip");
        contents.setText(row++, 0, CShell.cmsgs.createPasswordTip());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CShell.cmsgs.createDisplayName());
        contents.setWidget(row++, 1, _name = new TextBox());
        _name.addKeyboardListener(_validator);
        contents.getFlexCellFormatter().setColSpan(row, 0, 2);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Tip");
        contents.setText(row++, 0, CShell.cmsgs.createDisplayNameTip());

        contents.getFlexCellFormatter().setColSpan(row, 0, 2);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Status");
        contents.setWidget(row++, 0, _status = new Label(""));
        _status.setText(CShell.cmsgs.createMissingEmail());
    }

    // @Override // from PopupPanel
    public void show ()
    {
        super.show();
        _email.setFocus(true);
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(10);
        contents.setStyleName("createAccount");
        return contents;
    }

    protected void validateData ()
    {
        boolean valid = false;
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        if (email.length() == 0) {
            _status.setText(CShell.cmsgs.createMissingEmail());
        } else if (password.length() == 0) {
            _status.setText(CShell.cmsgs.createMissingPassword());
        } else if (confirm.length() == 0) {
            _status.setText(CShell.cmsgs.createMissingConfirm());
        } else if (!password.equals(confirm)) {
            _status.setText(CShell.cmsgs.createPasswordMismatch());
        } else if (name.length() == 0) {
            _status.setText(CShell.cmsgs.createMissingName());
        } else {
            _status.setText(CShell.cmsgs.createReady());
            valid = true;
        }
        _go.setEnabled(valid);
    }

    protected void createAccount ()
    {
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim();
        _status.setText(CShell.cmsgs.creatingAccount());
        CShell.usersvc.register(email, md5hex(password), name, 1, new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
                // TODO: display some sort of welcome to whirled business
                _parent.didLogon((WebCreds)result);
            }
            public void onFailure (Throwable caught) {
                _status.setText(CShell.serverError(caught));
            }
        });
    }

    protected native String md5hex (String text) /*-{
       return $wnd.hex_md5(text);
    }-*/;

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

    protected StatusPanel _parent;
    protected TextBox _email, _name;
    protected PasswordTextBox _password, _confirm;
    protected Button _go;
    protected Label _status;
}
