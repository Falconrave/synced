//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.util.Assert;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.ResultAdapter;
import com.threerings.util.ResultListener;

import com.threerings.bureau.client.BureauDirector;

import com.threerings.msoy.bureau.data.WindowAuthName;
import com.threerings.msoy.bureau.data.WindowClientObject;

/**
 * Manages a set of {@link Window} objects, keyed by host name and port.
 */
public class WindowDirector
{
    /**
     * Creates a new window director.
     */
    public function WindowDirector (bureauId :String, token :String, bureauDir :BureauDirector)
    {
        _bureauDir = bureauDir;
        _bureauId = bureauId;
        _token = token;
    }

    /**
     * Adds a service group that will be requested by all subsequent windows when they are
     * opened.
     */
    public function addServiceGroup (code :String) :void
    {
        Assert.isTrue(_windows.isEmpty());
        _serviceGroups.push(code);
    }

    /**
     * Attempts to logon to a world server. If successful, a {@link Window} object will be
     * returned via the supplied listener. This window will be open and ready to serve requests.
     * <p>NOTE: Since windows are ref-coutned, it is VERY important that the {@link #closeWindow}
     * function is also called, regardless of whether the agent still requires it.</p>
     */
    public function openWindow (host :String, port :int, listener :ResultListener) :void
    {
        var key :String = host + ":" + port;
        var window :WindowImpl = _windows.get(key) as WindowImpl;

        if (window == null) {
            Assert.isTrue(_windows.get(key) === undefined);
            window = new WindowImpl(_bureauDir, host, port, _bureauId, _token, _serviceGroups);
            _windows.put(key, window);
        }

        window.addRef();

        if (window.isLoggedOn()) {
            listener.requestCompleted(window);
            return;
        }

        window.addListener(new ResultAdapter(null,
            function (cause :Error) :void {
                if (window.releaseRef()) {
                    _windows.remove(key);
                }
            })); // either arg may be null in a util.ResultAdapter
        window.addListener(listener);
    }

    /**
     * Closes a successfully opened window.
     */
    public function closeWindow (window :Window) :void
    {
        var impl :WindowImpl = window as WindowImpl;
        Assert.isTrue(impl != null);
        Assert.isTrue(impl.isLoggedOn());
        if (impl.releaseRef()) {
            var key :String = impl.getHost() + ":" + impl.getPort();
            Assert.isTrue(_windows.get(key) !== undefined);
            _windows.remove(key);
            impl.close();
        }
    }

    protected var _windows :Map = Maps.newMapOf(String);
    protected var _bureauDir :BureauDirector;
    protected var _serviceGroups :Array = [];
    protected var _bureauId :String;
    protected var _token :String;

    // classes that need to get linekd in for serialization
    WindowAuthName;
    WindowClientObject;
}
}

import com.threerings.util.Assert;
import com.threerings.util.Log;
import com.threerings.util.ResultListener;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.bureau.client.BureauDirector;

import com.threerings.msoy.bureau.client.Window;
import com.threerings.msoy.bureau.data.WindowCredentials;

/**
 * Local implementation of Window
 */
class WindowImpl implements Window
{
    /**
     * Creates a new implementation that immediately connects the the given server and port and
     * requests the given service groups.
     */
    public function WindowImpl (
        director :BureauDirector, host :String, port :int, bureauId :String, token :String,
        serviceGroups :Array)
    {
        _director = director;

        _client = new Client(new WindowCredentials(bureauId, token));
        _client.setServer(host, [port]);
        _client.addEventListener(ClientEvent.CLIENT_DID_LOGON, clientDidLogon);
        _client.addEventListener(ClientEvent.CLIENT_FAILED_TO_LOGON, clientFailedToLogon);
        _client.addEventListener(ClientEvent.CLIENT_CONNECTION_FAILED, clientConnectionFailed);
        _client.addEventListener(ClientEvent.CLIENT_DID_LOGOFF, clientLoggedOff);

        var debug :Boolean = false;
        if (debug) {
            _client.addEventListener(ClientEvent.CLIENT_WILL_LOGON, logEvt);
            _client.addEventListener(ClientEvent.CLIENT_DID_LOGON, logEvt);
            _client.addEventListener(ClientEvent.CLIENT_FAILED_TO_LOGON, logEvt);
            _client.addEventListener(ClientEvent.CLIENT_OBJECT_CHANGED, logEvt);
            _client.addEventListener(ClientEvent.CLIENT_CONNECTION_FAILED, logEvt);
            _client.addEventListener(ClientEvent.CLIENT_WILL_LOGOFF, logEvt);
            _client.addEventListener(ClientEvent.CLIENT_DID_LOGOFF, logEvt);
            _client.addEventListener(ClientEvent.CLIENT_DID_CLEAR, logEvt);
        }

        for each (var grp :String in serviceGroups) {
            _client.addServiceGroup(grp);
        }

        _client.logon();
    }

    /** @inheritDoc */
    // from Window
    public function requireService (sclass: Class) :InvocationService
    {
        Assert.isTrue(isLoggedOn());
        return _client.requireService(sclass);
    }

    /** @inheritDoc */
    // from Window
    public function getClient () :Client
    {
        return _client;
    }

    /** @inheritDoc */
    // from Window
    public function getDObjectManager () :DObjectManager
    {
        Assert.isTrue(isLoggedOn());
        return _client.getDObjectManager();
    }

    /**
     * Adds a listener to the list that will be notified when the window succeeds in opening.
     */
    public function addListener (listener :ResultListener) :void
    {
        _listeners.push(listener);
    }

    /**
     * Tests if the window is currently open.
     */
    public function isLoggedOn () :Boolean
    {
        return _client.isLoggedOn();
    }

    /**
     * Closes the window. No further communication will be possible.
     */
    public function close () :void
    {
        _client.logoff(false);
    }

    /**
     * Retrieves the host this window is connected to.
     */
    public function getHost () :String
    {
        return _client.getHostname();
    }

    /**
     * Retrieves the port this window is connected to.
     */
    public function getPort () :int
    {
        return _client.getPorts()[0];
    }

    /**
     * Increments the reference counter.
     */
    public function addRef () :void
    {
        _refCount++;
    }

    /**
     * Decrements the reference counter, returning true if it has reached zero.
     */
    public function releaseRef () :Boolean
    {
        if (--_refCount == 0) {
            return true;
        }
        return false;
    }

    protected function logEvt (evt :ClientEvent) :void
    {
        log.info("Event on client", "client", _client, "event", evt);
    }

    protected function clientDidLogon (evt :ClientEvent) :void
    {
        for each (var listener :ResultListener in _listeners) {
            listener.requestCompleted(this);
        }
        _listeners = null;
    }

    protected function clientFailedToLogon (evt :ClientEvent) :void
    {
        const cause :Error = evt.getCause();
        for each (var listener :ResultListener in _listeners) {
            listener.requestFailed(cause);
        }
        _listeners = null;
    }

    protected function clientConnectionFailed (evt :ClientEvent) :void
    {
        clientFailedToLogon(evt);
    }

    protected function clientLoggedOff (evt :ClientEvent) :void
    {
        if (_refCount != 0) {
            log.warning("Window closed unexpectedly", "client", _client);
            _director.fatalError("Window closed unexpectedly");
        }
    }

    /** Create a logger for the entire package.. */
    protected const log :Log = Log.getLog(this);

    protected var _client :Client;
    protected var _listeners :Array = [];
    protected var _refCount :int;
    protected var _director :BureauDirector;
}
