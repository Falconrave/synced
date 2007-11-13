//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.io.TypedArray;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.util.Log;
import com.threerings.util.ResultListener;

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.client.PendingData;
import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.client.persist.SceneRepository;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomCodes;

/**
 * Handles custom scene traversal and extra bits for Whirled.
 */
public class MsoySceneDirector extends SceneDirector
{
    private static const log :Log = Log.getLog(MsoySceneDirector);

    public function MsoySceneDirector (
        ctx :WorldContext, locDir :LocationDirector, repo :SceneRepository)
    {
        super(ctx, locDir, repo, new MsoySceneFactory());
    }

    /**
     * Traverses the specified portal using the MsoySceneService which handles switching between
     * servers and other useful business.
     *
     * @return true if we issued the request, false if it was rejected for some reason.
     */
    public function traversePortal (portalId :int) :Boolean
    {
        // look up the destination scene and location
        var scene :MsoyScene = (getScene() as MsoyScene);
        if (scene == null) {
            log.warning("Asked to traverse portal when we have no scene [id=" + portalId + "].");
            return false;
        }

        // find the portal they're talking about
        var dest :MsoyPortal = (scene.getPortal(portalId) as MsoyPortal);
        if (dest == null) {
            log.warning("Requested to traverse non-existent portal [portalId=" + portalId +
                        ", portals=" + scene.getPortals() + "].");
            return false;
        }

        // prepare to move to this scene (sets up pending data)
        if (!prepareMoveTo(dest.targetSceneId, null)) {
            log.info("Portal traversal vetoed [portalId=" + portalId + "].");
            return false;
        }

        // note our departing portal id and target location in the destination scene
        _departingPortalId = portalId;
        (_pendingData as MsoyPendingData).destLoc = dest.dest;

        // now that everything is noted (in case we have to switch servers) ask to move
        sendMoveRequest();
        return true;
    }

    // from SceneDirector
    override public function moveTo (sceneId :int) :Boolean
    {
        if (sceneId == _sceneId) {
            // ignore this as we're just hearing back from our browser URL update mechanism
            return false;
        }
        return super.moveTo(sceneId);
    }

    // from SceneDirector
    override public function moveSucceeded (placeId :int, config :PlaceConfig) :void
    {
        var wctx :WorldContext = _ctx as WorldContext;
        var data :MsoyPendingData = _pendingData as MsoyPendingData;
        if (data != null && data.message != null) {
            wctx.displayFeedback(MsoyCodes.GENERAL_MSGS, data.message);
        }

        super.moveSucceeded(placeId, config);

        // tell our controller to update the URL of the browser to reflect our new location
        wctx.getMsoyController().wentToScene(_sceneId);
    }

    // from SceneDirector
    override public function requestFailed (reason :String) :void
    {
        // remember which scene we came from, possibly on another peer
        var pendingPreviousScene :int = _pendingData != null ?
            (_pendingData as MsoyPendingData).previousSceneId : -1;

        _departingPortalId = -1;
        super.requestFailed(reason);
        (_ctx as WorldContext).displayFeedback(MsoyCodes.GENERAL_MSGS, reason);

        // let's deal with the player getting bumped back from a locked scene
        if (reason == RoomCodes.E_ENTRANCE_DENIED) {
            bounceBack(_sceneId, pendingPreviousScene, reason);
        }
    }

    // from SceneDirector
    override protected function createPendingData () :PendingData
    {
        return new MsoyPendingData();
    }

    // from SceneDirector
    override public function prepareMoveTo (sceneId :int, rl :ResultListener) :Boolean
    {
        var result :Boolean = super.prepareMoveTo(sceneId, rl);
        if (result) {
            // super creates a pending request - fill it in with extra data
            var data :MsoyPendingData = _pendingData as MsoyPendingData;
            data.previousSceneId = _sceneId;
            data.message = _postMoveMessage;
            _postMoveMessage = null;
        }
        return result;
    }

    // from SceneDirector
    override protected function sendMoveRequest () :void
    {
        var data :MsoyPendingData = _pendingData as MsoyPendingData;
        
        // check the version of our cached copy of the scene to which we're requesting to move; if
        // we were unable to load it, assume a cached version of zero
        var sceneVers :int = 0;
        if (data.model != null) {
            sceneVers = data.model.version;
        }

        // note: _departingPortalId is only needed *before* a server switch, so we intentionally
        // allow it to get cleared out in the clientDidLogoff() call that happens as we're
        // switching from one server to another

        // issue a moveTo request
        log.info("Issuing moveTo(" + data.previousSceneId + "->" + data.sceneId + ", " +
                 sceneVers + ", " + _departingPortalId + ", " + data.destLoc + ").");
        _msservice.moveTo(
            _wctx.getClient(), data.sceneId, sceneVers, _departingPortalId, data.destLoc, this);
    }

    // documentation inherited
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);
        _departingPortalId = -1;
    }

    // from SceneDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);
        // get a handle on our special scene service
        _msservice = (client.requireService(MsoySceneService) as MsoySceneService);
    }

    /**
     * Do whatever cleanup is appropriate after we failed to enter a locked room.
     */
    protected function bounceBack (localSceneId :int, remoteSceneId :int, reason :String) :void
    {
        var wctx :WorldContext = _ctx as WorldContext;
        var ctrl :MsoyController = wctx.getMsoyController();
        
        // if we tried to move from one scene to another on the same peer, there's nothing to clean
        // up, just update the URL to make GWT happy
        if (localSceneId != -1) {
            ctrl.wentToScene(localSceneId);
            return;
        }

        // if we came here from a scene on another peer, let's go back there
        if (remoteSceneId != -1) {
            log.info("Returning to remote scene [sceneId=" + remoteSceneId + "].");
            _postMoveMessage = reason; // remember the error message
            ctrl.handleGoScene(remoteSceneId);
            return;
        }

        // we have nowhere to go back. let's just go home.
        var memberId :int = wctx.getMemberObject().memberName.getMemberId();
        if (memberId != 0) {
            log.info("Scene locked, returning home [memberId=" + memberId + "].");
            ctrl.handleGoMemberHome(memberId);
            return;
        }

        // we're a guest and don't have a home! just go to the generic public area.
        var commonAreaId :int = 1; // = SceneRecord.PUBLIC_ROOM.getSceneId() 
        ctrl.handleGoScene(commonAreaId);
    }
    
    protected var _postMoveMessage :String;
    protected var _msservice :MsoySceneService;
    protected var _departingPortalId :int = -1;
}
}
