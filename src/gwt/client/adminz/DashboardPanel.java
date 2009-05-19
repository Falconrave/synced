//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.Link;
import client.util.InfoCallback;
import client.util.ServiceUtil;

/**
 * Displays the various services available to support and admin personnel.
 */
public class DashboardPanel extends SmartTable
{
    public DashboardPanel ()
    {
        super("dashboardPanel", 0, 10);
        int row = 0, col = 0;

        // display some infoez
        setHTML(row, 0, _msgs.dashVersion(DeploymentConfig.version));
        setHTML(row++, 1, _msgs.dashBuilt(DeploymentConfig.buildTime));

        // admin-only controls
        if (CShell.isAdmin()) {
            FlowPanel admin = new FlowPanel();
            admin.add(MsoyUI.createLabel(_msgs.adminControls(), "Title"));
            final SimplePanel ddash = new SimplePanel();
            ddash.setWidget(MsoyUI.createActionLabel(_msgs.displayDashboard(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    ddash.setWidget(new Label(_msgs.displayDashboard()));
                    displayDashboard();
                }
            }));
            admin.add(ddash);
            admin.add(makeLink(_msgs.viewExchange(), "exchange"));
            admin.add(makeLink(_msgs.cashOutButton(), "cashout"));
            admin.add(makeLink(_msgs.statsButton(), "stats"));
            admin.add(makeLink(_msgs.viewABTests(), "testlist"));
            admin.add(makeLink(_msgs.viewBureaus(), "bureaus"));
            admin.add(makeLink(_msgs.panopticonStatus(), "panopticonStatus"));
            admin.add(makeLink(_msgs.viewSurveys(), "survey_e"));
            setWidget(row, col, admin);
            getFlexCellFormatter().setVerticalAlignment(row, col++, HasAlignment.ALIGN_TOP);

            FlowPanel reboot = new FlowPanel();
            reboot.add(MsoyUI.createLabel(_msgs.adminReboot(), "Title"));
            reboot.add(MsoyUI.createLabel(_msgs.adminRebootMessage(), null));
            TextArea message = MsoyUI.createTextArea("", 30, 4);
            reboot.add(message);
            reboot.add(makeReboot(_msgs.rebootInTwo(), 2, message));
            reboot.add(makeReboot(_msgs.rebootInFive(), 5, message));
            reboot.add(makeReboot(_msgs.rebootInFifteen(), 15, message));
            reboot.add(makeReboot(_msgs.rebootInThirty(), 30, message));
            // TODO: support reboot cancellation
            //reboot.add(makeReboot(_msgs.rebootCancel(), -1));
            setWidget(row, col, reboot);
            getFlexCellFormatter().setVerticalAlignment(row, col++, HasAlignment.ALIGN_TOP);
        }

        // support controls
        FlowPanel support = new FlowPanel();
        support.add(MsoyUI.createLabel(_msgs.supportControls(), "Title"));
        support.add(makeLink(_msgs.reviewButton(), "review"));
        support.add(makeLink(_msgs.promosButton(), "promos"));
        support.add(makeLink(_msgs.contestsButton(), "contests"));
        support.add(makeLink(_msgs.broadcastButton(), "broadcasts"));
        setWidget(row, col, support);
        getFlexCellFormatter().setVerticalAlignment(row++, col++, HasAlignment.ALIGN_TOP);
    }

    protected void displayDashboard ()
    {
        // load up the information needed to display the dashboard applet
        _usersvc.getConnectConfig(new InfoCallback<ConnectConfig>() {
            public void onSuccess (ConnectConfig config) {
                finishDisplayDashboard(config);
            }
        });
    }

    protected Widget makeLink (String title, String args)
    {
        Widget link = Link.create(title, Pages.ADMINZ, args);
        link.removeStyleName("inline");
        return link;
    }

    protected Widget makeReboot (String title, final int minutes, final TextArea messageWidget)
    {
        Button reboot = new Button(title);
        new ClickCallback<Void>(reboot) {
            protected boolean callService () {
                _adminsvc.scheduleReboot(minutes, messageWidget.getText(), this);
                return true;
            }
            protected boolean gotResult (Void result) {
                MsoyUI.info(minutes < 0 ? _msgs.rebootCancelled() :
                            _msgs.rebootScheduled(""+minutes));
                return true;
            }
        };
        return reboot;
    }

    protected void finishDisplayDashboard (ConnectConfig config)
    {
        CShell.frame.closeClient();

        // we have to serve admin-client.jar from the server to which it will connect back due to
        // security restrictions and proxy the game jar through there as well
        String appletURL = config.getURL(
            "/clients/" + DeploymentConfig.version + "/admin-client.jar");

        int row = getRowCount();
        getFlexCellFormatter().setStyleName(row, 0, "Applet");
        setWidget(row, 0, WidgetUtil.createApplet(
                      "admin", appletURL,
                      "com.threerings.msoy.admin.client.AdminApplet", 680, 400, false,
                      new String[] { "server", config.server,
                                     "port", "" + config.port,
                                     "authtoken", CShell.getAuthToken() }),
                  getCellCount(row-1), null);
    }

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
