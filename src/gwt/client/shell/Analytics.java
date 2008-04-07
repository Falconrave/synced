//
// $Id$

package client.shell;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Timer;

/**
 * Handles communication with Google Anayltics.
 */
public class Analytics
{
    /**
     * Initializes our analytics instance.
     */
    public void init ()
    {
        // our GA javascript is loaded asynchronously, so it might not be loaded when we're
        // initialized; annoyingly there is no cross-browser way to find out when dynamically
        // loaded JavaScript (that you don't control) is ready, so we have poll
        if (!(_initialized = initJS())) {
            new Timer() {
                public void run () {
                    Analytics.this.init();
                }
            }.schedule(500);

        } else {
            for (int ii = 0; ii < _pending.size(); ii++) {
                report((String)_pending.get(ii));
            }
            _pending.clear();
        }
    }

    /**
     * Reports a page view to Google Analytics.
     */
    public void report (String path)
    {
        if (!_initialized) {
            _pending.add(path);
        } else if (!reportJS(path)) {
            CShell.log("Failed to report to GA [path=" + path + "].");
        }
    }

    protected native boolean initJS () /*-{
        try {
            var pageTracker = $wnd._gat._getTracker("UA-169037-5");
            pageTracker._initData();
            return true;
        } catch (e) {
            return false;
        }
    }-*/;

    protected native boolean reportJS (String page) /*-{
        try {
            var pageTracker = $wnd._gat._getTracker("UA-169037-5");
            pageTracker._trackPageview(page);
            return true;
        } catch (e) {
            return false;
        }
    }-*/;

    protected boolean _initialized;
    protected List _pending = new ArrayList();
}
