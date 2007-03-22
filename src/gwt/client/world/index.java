//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.Page;
import client.shell.WorldClient;
import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY main page.
 */
public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (String token)
    {
        _entryCounter++;

        // cancel our refresher interval as we'll restart it if needed below
        if (_refresher != null) {
            _refresher.cancel();
            _refresher = null;
        }

        // don't show the flash client in the GWT shell
        if (!GWT.isScript()) {
            return;
        }

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CWorld.creds == null) {
            setContent(MsoyUI.createLabel(CWorld.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        try {
            if (token.startsWith("s")) {
                // go to a specific scene
                WorldClient.display("world", token, "sceneId=" + id(token, 1));

            } else if (token.startsWith("g")) {
                // go to a specific group's scene group
                WorldClient.display("world", token, "groupHome=" + id(token, 1));

            } else if (token.startsWith("m")) {
                // go to a specific member's home
                WorldClient.display("world", token, "memberHome=" + id(token, 1));

            } else if (token.startsWith("l")) {
                // go to a specific member's home
                WorldClient.display("world", token, "location=" + id(token, 1));

            } else if (token.startsWith("ng")) {
                // go to the neighborhood around the specified group
                displayNeighborhood(_entryCounter, id(token, 2), true);

            } else if (token.startsWith("nm")) {
                // go to the neighborhood around the specified member
                displayNeighborhood(_entryCounter, id(token, 2), false);

            } else if (token.startsWith("p")) {
                // display popular places by request
                displayHotSpots(_entryCounter);

            } else if (CWorld.creds != null) {
                // we're logged in, go to our home
                WorldClient.display("world", token, null);

            } else {
                // we're not logged in, show popular places
                displayHotSpots(_entryCounter);
            }

        } catch (NumberFormatException e) {
            // if all else fails, display popular places
            displayHotSpots(_entryCounter);
        }
    }

    // @Override // from Page
    public void onPageUnload ()
    {
        super.onPageUnload();

        if (_refresher != null) {
            _refresher.cancel();
            _refresher = null;
        }
    }

    // @Override // from Page
    protected void didLogoff ()
    {
        super.didLogoff();
        onHistoryChanged("p");
    }

    // @Override // from Page
    protected boolean needsHeaderClient ()
    {
        String token = getPageArgs();
        return (token.startsWith("ng") || token.startsWith("nm") || token.startsWith("p") ||
                CWorld.creds == null);
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CWorld.msgs = (WorldMessages)GWT.create(WorldMessages.class);
    }

    protected void displayNeighborhood (final int requestEntryCount, int entityId, boolean isGroup)
    {
        CWorld.membersvc.serializeNeighborhood(
            CWorld.creds, entityId, isGroup, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (requestEntryCount == _entryCounter) {
                    neighborhood((String) result);
                }
            }
            public void onFailure (Throwable caught) {
                if (requestEntryCount == _entryCounter) {
                    setContent(new Label(CWorld.serverError(caught)));
                }
            }
        });
        scheduleReload();
    }

    protected void displayHotSpots (final int requestEntryCount)
    {
        CWorld.membersvc.serializePopularPlaces(CWorld.creds, 20, new AsyncCallback() {
            public void onSuccess (Object result) {
                if (requestEntryCount == _entryCounter) {
                    hotSpots((String) result);
                }
            }
            public void onFailure (Throwable caught) {
                if (requestEntryCount == _entryCounter) {
                    setContent(new Label(CWorld.serverError(caught)));
                }
            }
        });
        scheduleReload();
    }

    protected void scheduleReload ()
    {
        _refresher = new Timer() {
            public void run() {
                onHistoryChanged(getPageArgs());
            }
        };
        _refresher.schedule(NEIGHBORHOOD_REFRESH_TIME * 1000);
    }

    protected void neighborhood (String hood)
    {
        if (hood == null) {
            setContent(new Label(CWorld.msgs.noSuchMember()));
        } else {
            setContent(FlashClients.createNeighborhood(hood), true, false);
        }
    }

    protected void hotSpots (String hotSpots)
    {
        setContent(FlashClients.createPopularPlaces(hotSpots), true, false);
    }

    protected int id (String token, int index)
    {
        return Integer.valueOf(token.substring(index)).intValue();
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "world";
    }

    /** A counter to help asynchronous callbacks to figure out if they've been obsoleted. */
    protected int _entryCounter;

    /** Handles periodic refresh of the popular places view. */
    protected Timer _refresher;

    protected static final int NEIGHBORHOOD_REFRESH_TIME = 60;
}
