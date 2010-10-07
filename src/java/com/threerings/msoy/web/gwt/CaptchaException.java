//
// $Id$

package com.threerings.msoy.web.gwt;

import com.threerings.web.gwt.ServiceException;

/**
 * A special case ServiceExeption for captcha failures.
 */
public class CaptchaException extends ServiceException
{
    public CaptchaException (String message)
    {
        super(message);
    }

    /**
     * Default constructor for use when unserializing.
     */
    public CaptchaException ()
    {
    }
}
