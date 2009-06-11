//
// $Id$

package com.threerings.msoy.client {

import com.threerings.io.TypedArray;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java MemberService interface.
 */
public interface MemberService extends InvocationService
{
    // from Java interface MemberService
    function acknowledgeWarning (arg1 :Client) :void;

    // from Java interface MemberService
    function bootFromPlace (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function complainMember (arg1 :Client, arg2 :int, arg3 :String) :void;

    // from Java interface MemberService
    function ditchFollower (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function followMember (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function getCurrentMemberLocation (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function getDisplayName (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function getHomeId (arg1 :Client, arg2 :int, arg3 :int, arg4 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function inviteAllToBeFriends (arg1 :Client, arg2 :TypedArray /* of int */, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function inviteToBeFriend (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function inviteToFollow (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function setAvatar (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setAway (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setDisplayName (arg1 :Client, arg2 :String, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setHomeSceneId (arg1 :Client, arg2 :int, arg3 :int, arg4 :int, arg5 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setMuted (arg1 :Client, arg2 :int, arg3 :Boolean, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function updateStatus (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void;
}
}
