//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.money.data.all.ExchangeData;
import com.threerings.msoy.money.data.all.ExchangeStatusData;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.ui.MsoyUI;

import client.util.MsoyPagedServiceDataModel;

public class ExchangePanel extends SmartTable
{
    public ExchangePanel ()
    {
        super("exchangePanel", 0, 10);

        int row = 0;
        setText(row, 0, "Current rate:", 1, "rightLabel");
        setText(row++, 2, "Target rate:", 1, "rightLabel");
        setText(row, 0, "Bar pool balance:", 1, "rightLabel");
        setText(row++, 2, "Target bar pool:", 1, "rightLabel");
        setText(row, 0, "Coin balance:", 1, "rightLabel");

        addWidget(new RecentExchanges(new ExchangeDataDataModel()), 4);
    }

    protected class ExchangeDataDataModel
        extends MsoyPagedServiceDataModel<ExchangeData, ExchangeStatusData>
    {
        @Override
        protected void callFetchService (
            int start, int count, boolean needCount, AsyncCallback<ExchangeStatusData> callback)
        {
            _moneysvc.getExchangeStatus(start, count, callback);
        }

        @Override
        protected void onSuccess (
            ExchangeStatusData result, AsyncCallback<List<ExchangeData>> callback)
        {
            super.onSuccess(result, callback);

            int row = 0;
            setText(row, 1, _rateFormat.format(result.rate));
            setText(row++, 3, _rateFormat.format(result.targetRate));
            setText(row, 1, String.valueOf(result.barPool));
            setText(row++, 3, String.valueOf(result.targetBarPool));
            setText(row, 1, String.valueOf(result.coinBalance));
        }
    }

    protected class RecentExchanges extends PagedTable<ExchangeData>
    {
        public RecentExchanges (ExchangeDataDataModel model)
        {
            super(20);
            setModel(model, 0);
        }

        @Override
        public List<Widget> createRow (ExchangeData data)
        {
            List<Widget> row = new ArrayList<Widget>();

            Label time = MsoyUI.createLabel(DateUtil.formatDateTime(data.timestamp), "Time");
            time.setWordWrap(false);
            row.add(time);

            row.add(MsoyUI.createLabel(String.valueOf(data.bars), "rightLabel"));
            row.add(MsoyUI.createLabel(String.valueOf(data.coins), "rightLabel"));
            row.add(MsoyUI.createLabel(_rateFormat.format(data.rate), "rightLabel"));
            // TODO: reference tx

            return row;
        }

        @Override
        public List<Widget> createHeader ()
        {
            List<Widget> header = new ArrayList<Widget>();

            header.add(MsoyUI.createLabel("When", null));
            header.add(MsoyUI.createLabel("Bars", "rightLabel"));
            header.add(MsoyUI.createLabel("Coins", "rightLabel"));
            header.add(MsoyUI.createLabel("Rate", "rightLabel"));
            // TODO: reference tx

            return header;
        }

        @Override
        public String getEmptyMessage ()
        {
            return ""; // not gonna happen
        }
    }

    protected NumberFormat _rateFormat = NumberFormat.getFormat("0.00");

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
