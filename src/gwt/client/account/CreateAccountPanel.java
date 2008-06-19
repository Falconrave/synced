//
// $Id$

package client.account;

import java.util.Date;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.AccountInfo;
import com.threerings.msoy.web.data.CaptchaException;
import com.threerings.msoy.web.data.SessionData;

import client.shell.Application;
import client.shell.CShell;
import client.util.DateFields;
import client.util.MediaUploader;
import client.util.MsoyUI;
import client.util.RoundBox;
import client.util.ThumbBox;

/**
 * Displays an interface for creating a new account.
 */
public class CreateAccountPanel extends VerticalPanel
{
    public interface RegisterListener 
    {
        /** Called when the player is logging on due to a successful registration. */
        public void didRegister ();
    }

    public CreateAccountPanel (RegisterListener regListener)
    {
        _regListener = regListener;
        setStyleName("createAccount");
        setSpacing(10);

        add(MsoyUI.createLabel(CAccount.msgs.createIntro(), "Intro"));

        // create the account information section
        RoundBox box = new RoundBox(RoundBox.DARK_BLUE);
        box.add(new LabeledBox(CAccount.msgs.createEmail(), _email = new TextBox(),
                               CAccount.msgs.createEmailTip()));
        _email.addKeyboardListener(_onType);
        _email.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _password.setFocus(true);
            }
        }));
        if (Application.activeInvite != null &&
            Application.activeInvite.inviteeEmail.matches(MsoyUI.EMAIL_REGEX)) {
            // provide the invitation email as the default
            _email.setText(Application.activeInvite.inviteeEmail);
        }
        _email.setFocus(true);

        box.add(WidgetUtil.makeShim(10, 10));
        box.add(new LabeledBox(CAccount.msgs.createPassword(), _password = new PasswordTextBox(),
                               CAccount.msgs.createPasswordTip(),
                               CAccount.msgs.createConfirm(), _confirm = new PasswordTextBox(),
                               CAccount.msgs.createConfirmTip()));
        _password.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _confirm.setFocus(true);
            }
        }));
        _password.addKeyboardListener(_onType);
        _confirm.addKeyboardListener(new EnterClickAdapter(new ClickListener() {
            public void onClick (Widget sender) {
                _name.setFocus(true);
            }
        }));
        _confirm.addKeyboardListener(_onType);
        add(makeStep(1, box));

        // create the real you section
        box = new RoundBox(RoundBox.DARK_BLUE);
        box.add(new LabeledBox(CAccount.msgs.createRealName(), _rname = new TextBox(),
                               CAccount.msgs.createRealNameTip()));
        _rname.addKeyboardListener(_onType);

        box.add(WidgetUtil.makeShim(10, 10));
        box.add(new LabeledBox(CAccount.msgs.createDateOfBirth(), _dateOfBirth = new DateFields(),
                               CAccount.msgs.createDateOfBirthTip()));
        add(makeStep(2, box));

        // create the Whirled you section
        box = new RoundBox(RoundBox.DARK_BLUE);
        _name = MsoyUI.createTextBox("", Profile.MAX_DISPLAY_NAME_LENGTH, -1);
        _name.addKeyboardListener(_onType);
        box.add(new LabeledBox(CAccount.msgs.createDisplayName(), _name,
                               CAccount.msgs.createDisplayNameTip()));
        box.add(WidgetUtil.makeShim(10, 10));
        box.add(new LabeledBox(CAccount.msgs.createPhoto(), _photo = new PhotoUploader(),
                               CAccount.msgs.createPhotoTip()));
        add(makeStep(3, box));

        // optionally add the recaptcha component
        if (hasRecaptchaKey()) {
            box = new RoundBox(RoundBox.DARK_BLUE);
            box.add(new HTML("<div id=\"recaptchaDiv\"></div>"));
            add(makeStep(4, box));
        }

        // add the TOS agreement checkbox and submit button
        final HorizontalPanel tosBits = new HorizontalPanel();
        tosBits.setVerticalAlignment(HasAlignment.ALIGN_BOTTOM);
        tosBits.addStyleName("TOS");
        tosBits.add(_tosBox = new CheckBox(""));
        tosBits.add(WidgetUtil.makeShim(5, 5));
        tosBits.add(MsoyUI.createHTML(CAccount.msgs.createTOSAgree(), null));
        add(tosBits);

        HorizontalPanel controls = new HorizontalPanel();
        controls.setWidth("400px");
        controls.add(_status = MsoyUI.createLabel("", "Status"));
        controls.add(WidgetUtil.makeShim(10, 10));
        controls.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        ClickListener createGo = new ClickListener() {
            public void onClick (Widget sender) {
                createAccount();
            }
        };
        controls.add(MsoyUI.createButton(MsoyUI.LONG_THICK, CAccount.msgs.createGo(), createGo));
        add(controls);

        Label slurp;
        add(slurp = new Label(""));
        setCellHeight(slurp, "100%");
    }

    @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        if (_email != null) {
            _email.setFocus(true);
        }
        if (hasRecaptchaKey()) {
            RootPanel.get("recaptchaDiv").add(
                    MsoyUI.createLabel(CAccount.msgs.createCaptcha(), "label"));
            initCaptcha();
        }
    }

    protected void initCaptcha ()
    {
        // our JavaScript is loaded asynchrnously, so there's a possibility that it won't be set up
        // by the time we try to initialize ourselves; in that case we have no recourse but to try
        // again in a short while (there's no way to find out when async JS is loaded)
        if (!createRecaptcha("recaptchaDiv")) {
            new Timer() {
                public void run () {
                    initCaptcha();
                }
            }.schedule(500);
        }
    }

    protected boolean validateData ()
    {
        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim(), confirm = _confirm.getText().trim();
        String status;
        FocusWidget toFocus = null;
        if (email.length() == 0) {
            status = CAccount.msgs.createMissingEmail();
            toFocus = _email;
        } else if (password.length() == 0) {
            status = CAccount.msgs.createMissingPassword();
            toFocus = _password;
        } else if (confirm.length() == 0) {
            status = CAccount.msgs.createMissingConfirm();
            toFocus = _confirm;
        } else if (!password.equals(confirm)) {
            status = CAccount.msgs.createPasswordMismatch();
            toFocus = _confirm;
        } else if (_dateOfBirth.getDate() == null) {
            status = CAccount.msgs.createMissingDoB();
            // this is not a FocusWidget so we have to handle it specially
            _dateOfBirth.setFocus(true);
        } else if (name.length() < Profile.MIN_DISPLAY_NAME_LENGTH) {
            status = CAccount.msgs.createNameTooShort(""+Profile.MIN_DISPLAY_NAME_LENGTH);
            toFocus = _name;
        } else if (!_tosBox.isChecked()) {
            status = CAccount.msgs.createMustAgreeTOS();
        } else if (hasRecaptchaKey() && (getRecaptchaResponse() == null ||
                    getRecaptchaResponse().length() == 0)) {
            status = CAccount.msgs.createMustCaptcha();
            focusRecaptcha();
        } else {
            return true;
        }

        if (toFocus != null) {
            toFocus.setFocus(true);
        }
        setStatus(status);
        return false;
    }

    protected void createAccount ()
    {
        if (!validateData()) {
            return;
        }

        String[] today = new Date().toString().split(" ");
        String thirteenYearsAgo = "";
        for (int ii = 0; ii < today.length; ii++) {
            if (today[ii].matches("[0-9]{4}")) {
                int year = Integer.valueOf(today[ii]).intValue();
                today[ii] = "" + (year - 13);
            }
            thirteenYearsAgo += today[ii] + " ";
        }

        Date dob = DateFields.toDate(_dateOfBirth.getDate());
        if (new Date(thirteenYearsAgo).compareTo(dob) < 0) {
            setStatus(CAccount.msgs.createNotThirteen());
            return;
        }

        String email = _email.getText().trim(), name = _name.getText().trim();
        String password = _password.getText().trim();
        String inviteId = (Application.activeInvite == null) ?
            null : Application.activeInvite.inviteId;
        int guestId = CAccount.isGuest() ? CAccount.getMemberId() : 0;
        AccountInfo info = new AccountInfo();
        info.realName = _rname.getText().trim();

        setStatus(CAccount.msgs.creatingAccount());
        String challenge = hasRecaptchaKey() ? getRecaptchaChallenge() : null;
        String response = hasRecaptchaKey() ? getRecaptchaResponse() : null;
        CAccount.usersvc.register(
            DeploymentConfig.version, email, CAccount.md5hex(password), name, 
            _dateOfBirth.getDate(), _photo.getPhoto(), info, 1, inviteId, guestId, challenge, 
            response, new AsyncCallback() {
            public void onSuccess (Object result) {
                // notify our registration listener
                _regListener.didRegister();
                // clear our current token otherwise didLogon() will try to load it
                Application.setCurrentToken(null);
                // pass our credentials into the application (which will trigger a redirect)
                CAccount.app.didLogon((SessionData)result);
            }
            public void onFailure (Throwable caught) {
                if (hasRecaptchaKey()) {
                    reloadRecaptcha();
                    if (caught instanceof CaptchaException) {
                        focusRecaptcha();
                    }
                }
                setStatus(CAccount.serverError(caught));
            }
        });
    }

    protected void setStatus (String text)
    {
        _status.setText(text);
    }

    protected static Widget makeStep (int step, Widget contents)
    {
        SmartTable table = new SmartTable("Step", 0, 0);
        table.setText(0, 0, step + ".", 1, "Number");
        table.setWidget(0, 1, contents, 1, null);
        return table;
    }

    protected class PhotoUploader extends SmartTable
    {
        public PhotoUploader ()
        {
            setWidget(0, 0, new ThumbBox(Profile.DEFAULT_PHOTO, null));
            getFlexCellFormatter().setRowSpan(0, 0, 2);
            setWidget(0, 1, new MediaUploader(Item.THUMB_MEDIA, new MediaUploader.Listener() {
                public void mediaUploaded (String name, MediaDesc desc, int width, int height) {
                    if (!desc.isImage()) {
                        MsoyUI.error(CShell.emsgs.errPhotoNotImage());
                        return;
                    }
                    _media = desc;
                    setWidget(0, 0, new ThumbBox(_media, null));
                }
            }));
            setText(1, 0, CAccount.msgs.createPhotoTip(), 1, "tipLabel");

        }

        public MediaDesc getPhoto ()
        {
            return _media;
        }

        protected MediaDesc _media;
    }

    protected static class LabeledBox extends FlowPanel
    {
        public LabeledBox (String title, Widget contents, String tip)
        {
            setStyleName("Box");
            _tip = new SmartTable("Tip", 0, 0);
            add(title, contents, tip);
        }

        public LabeledBox (String title1, Widget contents1, String tip1,
                           String title2, Widget contents2, String tip2)
        {
            this(title1, contents1, tip1);
            add(WidgetUtil.makeShim(3, 3));
            add(title2, contents2, tip2);
        }

        public void add (String title, final Widget contents, final String tip)
        {
            add(MsoyUI.createLabel(title, "Label"));
            add(contents);
            if (contents instanceof SourcesFocusEvents) {
                ((SourcesFocusEvents)contents).addFocusListener(new FocusListener() {
                    public void onFocus (Widget sender) {
                        // we want contents here not sender because of DateFields
                        showTip(contents, tip);
                    }
                    public void onLostFocus (Widget sender) {
                        if (_tip.isAttached()) {
                            remove(_tip);
                        }
                    }
                });
            }
        }

        protected void showTip (Widget trigger, String tip)
        {
            if (!_tip.isAttached()) {
                DOM.setStyleAttribute(_tip.getElement(), "left",
                                      (trigger.getOffsetWidth()+15) + "px");
                DOM.setStyleAttribute(_tip.getElement(), "top",
                                      (trigger.getAbsoluteTop() - getAbsoluteTop() +
                                       trigger.getOffsetHeight()/2 - 27) + "px");
                _tip.setText(0, 0, tip);
                add(_tip);
            }
        }

        protected SmartTable _tip;
    }

    protected KeyboardListenerAdapter _onType = new KeyboardListenerAdapter() {
        public void onKeyDown (Widget sender, char keyCode, int modifiers) {
            setStatus("");
        }
    };

    protected static native boolean hasRecaptchaKey () /*-{
        return !(typeof $wnd.recaptchaPublicKey == "undefined");
    }-*/;

    protected static native boolean createRecaptcha (String element) /*-{
        try {
            if ($wnd.Recaptcha != null) {
                $wnd.Recaptcha.create($wnd.recaptchaPublicKey, element, { theme: "white" });
                return true;
            }
        } catch (e) {
            // fall through, return false
        }
        return false;
    }-*/;

    protected static native String getRecaptchaChallenge () /*-{
        return $wnd.Recaptcha.get_challenge();
    }-*/;

    protected static native String getRecaptchaResponse () /*-{
        return $wnd.Recaptcha.get_response();
    }-*/;

    protected static native void focusRecaptcha () /*-{
        $wnd.Recaptcha.focus_response_field();
    }-*/;

    protected static native void reloadRecaptcha () /*-{
        $wnd.Recaptcha.reload();
    }-*/;

    protected TextBox _email, _name, _rname;
    protected PasswordTextBox _password, _confirm;
    protected DateFields _dateOfBirth;
    protected PhotoUploader _photo;
    protected CheckBox _tosBox;
    protected Label _status;
    protected RegisterListener _regListener;
}
