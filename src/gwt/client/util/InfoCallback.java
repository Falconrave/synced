//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

import client.shell.CShell;
import client.ui.MsoyUI;

/**
 * Reports a callback error via {@link MsoyUI#info}.
 */
public abstract class InfoCallback<T> implements AsyncCallback<T>
{
    /** Used for those times when you just don't care enough to look at the response. */
    public static class NOOP<N> extends InfoCallback<N> {
        public void onSuccess (N value) {
            // noop!
        }
    };

    /**
     * Creates a callback that will display its error in the middle of the page.
     */
    public InfoCallback ()
    {
    }

    /**
     * Creates a callback that will display its error near the supplied widget.
     */
    public InfoCallback (Widget errorNear)
    {
        _errorNear = errorNear;
    }

    // from AsyncCallback
    public void onFailure (Throwable cause)
    {
        if (_errorNear == null) {
            MsoyUI.error(CShell.serverError(cause));
        } else {
            MsoyUI.errorNear(CShell.serverError(cause), _errorNear);
        }
        CShell.log("Service request failed", cause);

        // TODO: It seems 3 possible things happen on failure:
        // 1) The failure comes back and this displays a message and everything is fine.
        // 2) The URL was changed in the browser, then this failure occurs retrieving
        // content for the new page and the user is left viewing their old page but with
        // an incorrect URL.
        // 3) Somtimes, the user tries to load a URL fresh, this failure occurs, and the user
        // sees the nice error message but is left on a totally blank page.
        //
        // 2 & 3 should be handled, and for #3 the user should be directed to the home page or
        // given a 404-type page that has a link to the home page.
    }

    protected Widget _errorNear;
}
