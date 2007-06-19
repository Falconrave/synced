//
// $Id$

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.FlashClients;

public class LevelUpdateEvent extends FlashEvent
{
    // constants for the different types of level updates we can receive.  Defined in BaseClient.as
    public static final int LEVEL = 1;
    public static final int FLOW = 2;
    public static final int GOLD = 3; 
    public static final int MAIL = 4;

    // @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args) 
    {
        _type = FlashClients.getIntElement(args, 0);
        _value = FlashClients.getIntElement(args, 1);
        _oldValue = FlashClients.getIntElement(args, 2);
    }

    // @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof LevelsListener) {
            ((LevelsListener) listener).levelUpdated(this);
        }
    }

    public int getType ()
    {
        return _type;
    }

    public int getValue ()
    {
        return _value;
    }

    public int getOldValue ()
    {
        return _oldValue;
    }

    protected int _type;
    protected int _value;
    protected int _oldValue;
}
