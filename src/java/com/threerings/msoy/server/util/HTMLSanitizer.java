//
// $Id$

package com.threerings.msoy.server.util;

import java.io.FileReader;
import java.net.URL;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;

import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;

import static com.threerings.msoy.Log.log;

/**
 * Makes sure HTML doesn't contain anything naughty.
 */
public class HTMLSanitizer
{
    /**
     * Removes unsavory bits from the supplied HTML and returns the remainder.
     */
    public static String sanitize (String html)
    {
        AntiSamy as = new AntiSamy();
        try {
            CleanResults cr = as.scan(html, _policy);
            for (Object error : cr.getErrorMessages()) {
                log.info(String.valueOf(error)); // for debuggery
            }
            return cr.getCleanHTML();

        } catch (Exception e) {
            log.log(Level.WARNING, "HTML sanitizer choked.", e);
            return "";
        }
    }

    protected static Policy _policy;
    static {
        URL policyLoc = HTMLSanitizer.class.getClassLoader().getResource("antisamy-config.xml");
        try {
            _policy = Policy.getInstance(policyLoc.toString());
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to parse HTML sanitizer policy file " +
                    "[loc=" + policyLoc + "].", e);
        }
    }
}
