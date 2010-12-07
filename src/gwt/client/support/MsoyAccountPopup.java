//
// $Id$

package client.support;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.underwire.gwt.client.AccountPopup;
import com.threerings.underwire.gwt.client.WebContext;
import com.threerings.underwire.web.data.Account;

import com.threerings.msoy.underwire.gwt.MsoyAccount;
import com.threerings.msoy.underwire.gwt.SupportService;
import com.threerings.msoy.underwire.gwt.SupportServiceAsync;
import com.threerings.msoy.underwire.gwt.MsoyAccount.SocialStatus;

import client.util.ClickCallback;

/**
 * Extends the underwire account popup to display additional msoy data.
 */
public class MsoyAccountPopup extends AccountPopup
{
    /**
     * Creates a new msoy account popup.
     */
    public MsoyAccountPopup (WebContext ctx, Account account, boolean autoShowRelated)
    {
        super(ctx, account, autoShowRelated);
    }

    /**
     * Adds in the extra controls for the msoy data.
     */
    @Override // from AccountPopup
    protected int addMoreControls (int row)
    {
        row = super.addMoreControls(row);

        // greeter flag
        setText(row, 0, CSupport.msgs.adminSocialStatus());
        setWidget(row++, 1, _status = new ListBox());
        _status.addItem(CSupport.msgs.adminSocialStatus0());
        _status.addItem(CSupport.msgs.adminSocialStatus1());
        _status.addItem(CSupport.msgs.adminSocialStatus2());
        _status.setSelectedIndex(((MsoyAccount)_account).status.ordinal());
        new ClickCallback<Void> (_status) {
            SocialStatus value;
            @Override protected boolean callService () {
                value = SocialStatus.values()[_status.getSelectedIndex()];
                if (value != ((MsoyAccount)_account).status) {
                    _supportService.setSocialStatus(Integer.valueOf(_account.name.accountName),
                        value, this);
                    return true;
                }
                return false;
            }
            @Override protected boolean gotResult (Void result) {
                ((MsoyAccount)_account).status = value;
                return true;
            }
        };
        
        return row;
    }
    
    protected ListBox _status;

    protected static final SupportServiceAsync _supportService = GWT.create(SupportService.class);
}
