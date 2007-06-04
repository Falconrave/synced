//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

public abstract class FlashEvent
{
    /**
     * Pull the expected values for this event out of the JavaScriptObject, using the utility
     * functions in FlashClients.
     */
    public abstract void readFlashArgs (JavaScriptObject args);

    /**
     * Events with the associated listener interface should implement this function and 
     * notify the supplied listener if it implements their event listening interface.
     */
    public abstract void notifyListener (FlashEventListener listener);
}
