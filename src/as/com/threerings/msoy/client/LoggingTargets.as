//
// $Id$

package com.threerings.msoy.client {

import flash.external.ExternalInterface;

import com.threerings.util.LogTarget;

import com.threerings.msoy.data.MemberObject;

// TODO: stop listening at the end?
public class LoggingTargets
{
    public static function configureLogging (ctx :BaseContext) :void
    {
        var userObj :MemberObject = ctx.getClientObject();

        // for now, everything logs to the FireBug console
        try {
            if (_bugTarget == null && ExternalInterface.available) {
                ExternalInterface.call("console.debug", "Msoy console logging enabled");
                _bugTarget = new FireBugTarget();
                Log.addTarget(_bugTarget);
            }
        } catch (err :Error) {
            // oh well!
        }

        if ((null != ctx.getStage().loaderInfo.parameters["test"]) || Prefs.getLogToChat()) {
            if (_chatTarget == null) {
                _chatTarget = new ChatTarget(ctx);
                Log.addTarget(_chatTarget);
            }
        } else if (_chatTarget != null) {
            Log.removeTarget(_chatTarget);
            _chatTarget = null;
        }
    }

    protected static var _bugTarget :LogTarget;
    protected static var _chatTarget :LogTarget;
}
}

import flash.external.ExternalInterface;

import com.threerings.util.LogTarget;
import com.threerings.util.MessageBundle;

import com.threerings.msoy.client.BaseContext;

// TODO: stop listening at the end?
class ChatTarget
    implements LogTarget
{
    public function ChatTarget (ctx :BaseContext)
    {
        _ctx = ctx;
    }

    // from LogTarget
    public function log (msg :String) :void
    {
        _ctx.displayInfo(null, MessageBundle.taint(msg));
    }

    protected var _ctx :BaseContext;
}

/**
 * A logging target that goes to firebug's console.
 */
class FireBugTarget
    implements LogTarget
{
    // from LogTarget
    public function log (msg :String) :void
    {
/*
        // TEMP: needed because of bug in ExternalInterface
        // (long lines are unterminated)
        var max_length :int = 79;
        while (msg.length > max_length) {
            ExternalInterface.call("console.debug",
                msg.substring(0, max_length));
            msg = msg.substring(max_length);
        }
        // END: temp
*/

        ExternalInterface.call("console.debug", msg);
    }
}
