//
// $Id$

package client.frame;

import com.google.gwt.user.client.History;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.Tabs;

import client.shell.Page;
import client.ui.MsoyUI;

/**
 * Displays a page title and subnavigation at the top of the page content area.
 */
public class TitleBar extends SmartTable
{
    /**
     * Creates a title bar for the specified page.
     */
    public static TitleBar create (Tabs tab, ClickHandler onClose)
    {
        return new TitleBar(tab, Page.getDefaultTitle(tab), new SubNaviPanel(tab), onClose);
    }

    public TitleBar (Tabs tab, String title, SubNaviPanel subnavi, ClickHandler onClose)
    {
        super("pageTitle", 0, 0);

        setWidget(0, 0, createImage(tab), 3, null);

        _tab = tab;
        _titleLabel = new Label(title);
        _titleLabel.setStyleName("Title");

        Widget back = MsoyUI.createImageButton("backButton", new ClickHandler() {
            public void onClick (ClickEvent event) {
                History.back();
            }
        });

        HorizontalPanel panel = new HorizontalPanel();
        panel.add(WidgetUtil.makeShim(10, 1));
        panel.add(back);
        panel.add(WidgetUtil.makeShim(12, 1));
        panel.add(_titleLabel);
        panel.setCellVerticalAlignment(back, HorizontalPanel.ALIGN_MIDDLE);

        setWidget(1, 0, panel);
        setWidget(1, 2, _subnavi = subnavi, 1, "SubNavi");
        getFlexCellFormatter().setVerticalAlignment(1, 1, HasAlignment.ALIGN_BOTTOM);

        _closeBox = MsoyUI.createActionImage("/images/ui/close.png", onClose);
        _closeBox.addStyleName("Close");
        _closeShim = MsoyUI.createHTML("&nbsp;", "Close");
        setCloseVisible(false);
    }

    @Override
    public void setTitle (String title)
    {
        if (title != null) {
            _titleLabel.setText(title);
        }
    }

    public void resetNav ()
    {
        boolean closeWasVisible = _closeBox.isAttached();
        _subnavi.reset(_tab);
        setCloseVisible(closeWasVisible);
    }

    public void addContextLink (String label, Pages page, String args, int position)
    {
        _subnavi.addContextLink(label, page, args, position);
    }

    public void setCloseVisible (boolean visible)
    {
        _subnavi.remove(_closeBox);
        _subnavi.remove(_closeShim);
        if (visible) {
            _subnavi.add(_closeBox);
        } else {
            _subnavi.add(_closeShim);
        }
    }

    protected Image createImage (Tabs tab)
    {
        String id = (tab == null) ? "solid" : tab.toString().toLowerCase();
        return new Image("/images/header/" + id + "_cap.png");
    }

    protected Tabs _tab;
    protected Label _titleLabel;
    protected SubNaviPanel _subnavi;
    protected Widget _closeBox, _closeShim;
}
