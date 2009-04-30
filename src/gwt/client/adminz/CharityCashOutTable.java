//
// $Id$

package client.adminz;

import java.util.List;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.MoneyUtil;
import client.util.InfoCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.money.data.all.CharityBlingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

public class CharityCashOutTable extends PagedGrid<CharityBlingInfo>
{
    public CharityCashOutTable ()
    {
        super(10, 1, NAV_ON_BOTTOM);
        init();
    }

    @Override // from PagedGrid
    protected Widget createWidget (CharityBlingInfo entry)
    {
        return new CharityCashOutWidget(entry);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _msgs.coEmptyMessage();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }
    
    protected void init ()
    {
        addStyleName("dottedGrid");
        reload();
    }
    
    protected void reload ()
    {
        _moneysvc.getCharityBlingInfo(new InfoCallback<List<CharityBlingInfo>>() {
            public void onSuccess (List<CharityBlingInfo> result) {
                setModel(new SimpleDataModel<CharityBlingInfo>(result), 0);
            }
        });
    }

    protected class CharityCashOutWidget extends SmartTable
    {
        public CharityCashOutWidget (final CharityBlingInfo item)
        {
            super("cashOutWidget", 0, 3);
            addStyleName("charityCashOutWidget");
            
            this.entry = item;
            
            setWidget(0, 0, Link.create(item.displayName, Pages.PEOPLE,
                         Integer.toString(item.memberId)), 1, "Name");
            
            setWidget(1, 0, new Anchor("mailto:" + item.emailAddress, item.emailAddress));
            setText(0, 1, _msgs.coEntryAmount(Currency.BLING.format(item.blingAmount)));
            setText(1, 1, _msgs.coEntryWorth(MoneyUtil.formatUSD(item.blingWorth)));
            
            SmartTable extras = new SmartTable("Extras", 0, 5);
            Button btn = new Button(_msgs.coEntryTransactionsButton());
            btn.addStyleName("sideButton");
            btn.addClickHandler(Link.createListener(Pages.ME, Args.compose("transactions", "3", 
                String.valueOf(item.memberId))));
            extras.addWidget(btn, 0, null);
            btn = new Button(_msgs.coEntryCashOutButton());
            btn.addStyleName("sideButton");
            btn.addClickHandler(new ClickHandler() {
                public void onClick (ClickEvent event) {
                    showPanel(new CashOutPanel(item.blingAmount));
                }
            });
            extras.addWidget(btn, 0, null);
            
            setWidget(0, 2, extras);
            getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
            getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        }
        
        protected void showPanel (Panel panel)
        {
            setWidget(3, 0, panel, 3, "PopupPanel");
        }
        
        protected class CashOutPanel extends FlowPanel
        {
            public CashOutPanel (int initialAmount)
            {
                add(new InlineLabel(_msgs.coEntryCashOutPanelAmount()));
                
                _amountBox = new TextBox();
                _amountBox.setText(Currency.BLING.format(initialAmount));
                add(_amountBox);
                
                Button btn = new Button(_msgs.coEntryCashOutPanelButton());
                btn.addStyleName("PopupButton");
                btn.addClickHandler(new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        doCashOut();
                    }
                });
                add(btn);
                
                add(_status = new InlineLabel(""));
            }
            
            @Override
            protected void onLoad ()
            {
                super.onLoad();
                _amountBox.setSelectionRange(0, _amountBox.getText().length());
                _amountBox.setFocus(true);
            }
            
            protected void doCashOut()
            {
                int blingAmount = Currency.BLING.parse(_amountBox.getText());
                _moneysvc.charityCashOutBling(entry.memberId, blingAmount,
                    new InfoCallback<Void>() {
                    public void onSuccess (Void result) {
                        reload();
                        MsoyUI.info(_msgs.coEntryCashOutSuccess());
                    }
                });
            }
            
            protected final TextBox _amountBox;
            protected final InlineLabel _status;
        }
        
        protected final CharityBlingInfo entry;
    }
    
    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
