//
// $Id$

package com.threerings.msoy.client {

import flash.system.Capabilities;

import mx.controls.Text;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * Displays a simple "About Whirled" dialog.
 */
public class AboutDialog extends FloatingPanel
{
    public function AboutDialog (ctx :MsoyContext)
    {
        super(ctx, Msgs.GENERAL.get("t.about"));
        open(false);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var textArea :Text = new Text();
        textArea.width = 300;
        textArea.selectable = false;
        textArea.htmlText = Msgs.GENERAL.get("m.about", DeploymentConfig.buildTime,
            Capabilities.version + (Capabilities.isDebugger ? " (debug)" : ""));
        addChild(textArea);

        addButtons(OK_BUTTON);
    }
}
}
