//
// $Id: AVRGameControlBackend.as 6424 2007-10-28 23:20:25Z zell $

package com.threerings.msoy.game.client {

import com.threerings.util.Iterator;
import com.threerings.util.Log;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.presents.dobj.*;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.world.client.RoomView;

public class QuestControlBackend
{
    public static const log :Log = Log.getLog(AVRGameControlBackend);

    public function QuestControlBackend (
        gctx :GameContext, backend :AVRGameControlBackend, gameObj :AVRGameObject)
    {
        _gctx = gctx;
        _mctx = gctx.getWorldContext();
        _backend = backend;
        _gameObj = gameObj;

        _playerObj = _gctx.getPlayerObject();
        _playerObj.addListener(_playerStateListener);
    }

    public function shutdown () :void
    {
         _playerObj.removeListener(_playerStateListener);
    }

    public function populateSubProperties (o :Object) :void
    {
        // QuestControl (sub)
        o["offerQuest_v1"] = offerQuest_v1;
        o["updateQuest_v1"] = updateQuest_v1;
        o["completeQuest_v1"] = completeQuest_v1;
        o["cancelQuest_v1"] = cancelQuest_v1;
        o["getActiveQuests_v1"] = getActiveQuests_v1;
    }

    protected function offerQuest_v1 (questId :String, intro :String, initialStatus :String)
        :Boolean
    {
        if (!_backend.isPlaying() || isOnQuest(questId)) {
            return false;
        }
        var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
        if (view == null) {
            // should hopefully not happen
            return false;
        }

        var actualOffer :Function = function() :void {
            _gameObj.avrgService.startQuest(_gctx.getClient(), questId, initialStatus,
                loggingConfirmListener("startQuest", function () :void {
//                    _mctx.displayFeedback(null, "Quest begun: " + initialStatus);
                }));
        };

        if (intro == null) {
            // only the tutorial is allowed to skip the UI
            if (_mctx.getGameDirector().isPlayingTutorial()) {
                actualOffer();
                return true;
            }
            return false;
        }

        view.getRoomController().offerQuest(_gctx, intro, actualOffer);
        return true;
    }

    protected function updateQuest_v1 (questId :String, step :int, status :String) :Boolean
    {
        if (!isOnQuest(questId)) {
            return false;
        }
        _gameObj.avrgService.updateQuest(
            _gctx.getClient(), questId, step, status, loggingConfirmListener(
                "updateQuest", function () :void {
//                    _mctx.displayFeedback(null, "Quest update: " + status);
                }));
        return true;
    }

    protected function completeQuest_v1 (questId :String, outro :String, payout :int) :Boolean
    {
        if (!_backend.isPlaying() || !isOnQuest(questId)) {
            return false;
        }
        var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
        if (view == null) {
            // should hopefully not happen
            return false;
        }

        var actualComplete :Function = function() :void {
            _gameObj.avrgService.completeQuest(
                _gctx.getClient(), questId, payout, loggingConfirmListener(
                    "completeQuest", function () :void {
//                        _mctx.displayFeedback(null, "Quest completed!");
                    }));
        };

        if (outro == null) {
            // only the tutorial is allowed to skip the UI
            if (_mctx.getGameDirector().isPlayingTutorial()) {
                actualComplete();
                return true;
            }
            return false;
        }

        view.getRoomController().completeQuest(_gctx, outro, actualComplete);
        return true;
    }

    protected function cancelQuest_v1 (questId :String) :Boolean
    {
        if (!_backend.isPlaying() || !isOnQuest(questId)) {
            return false;
        }
        // TODO: confirmation dialog
        _gameObj.avrgService.cancelQuest(
            _gctx.getClient(), questId, loggingConfirmListener(
                "cancelQuest", function () :void {
//                    _mctx.displayFeedback(null, "Quest cancelled!");
                }));
        return true;
    }

    protected function getActiveQuests_v1 () :Array
    {
        var list :Array = new Array();
        var i :Iterator = _playerObj.questState.iterator();
        while (i.hasNext()) {
            var state :QuestState = QuestState(i.next());
            list.push([ state.questId, state.step, state.status ]);
        }
        return list;
    }

    protected function isOnQuest (questId :String) :Boolean
    {
        var i :Iterator = _playerObj.questState.iterator();
        while (i.hasNext()) {
            var state :QuestState = QuestState(i.next());
            if (state.questId == questId) {
                return true;
            }
        }
        return false;
    }

    protected function loggingConfirmListener (svc :String, processed :Function = null)
        :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        }, processed);
    }

    protected function loggingInvocationListener (svc :String) :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _backend :AVRGameControlBackend;
    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;

    protected var _playerStateListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == PlayerObject.QUEST_STATE) {
                _backend.callUserCode(
                    "questStateChanged_v1", QuestState(event.getEntry()).questId, true);
            }
        },
        null,
        function (event :EntryRemovedEvent) :void {
            if (event.getName() == PlayerObject.QUEST_STATE) {
                _backend.callUserCode("questStateChanged_v1", event.getKey(), false);
            }
        });
}
}
