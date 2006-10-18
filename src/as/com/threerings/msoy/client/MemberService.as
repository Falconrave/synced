//
// $Id$

package com.threerings.msoy.client {

import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java MemberService interface.
 */
public interface MemberService extends InvocationService
{
    // from Java interface MemberService
    function alterFriend (arg1 :Client, arg2 :int, arg3 :Boolean, arg4 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function getMemberHomeId (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function purchaseRoom (arg1 :Client, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setAvatar (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function setDisplayName (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void;
}
}
