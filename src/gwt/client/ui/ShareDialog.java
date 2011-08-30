//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;

import com.threerings.orth.data.MediaDesc;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.ShellMessages;

/**
 * A wee dialog that pops up to allow the user to share whirled content on other
 * sites.
 */
public class ShareDialog extends BorderedDialog
{
    public static class Info
    {
        public Pages page;
        public Args args;
        public String what;
        public String title;
        public String descrip;
        public MediaDesc image;
    }

    public ShareDialog (Info info)
    {
        super(true); // autohide
        setHeaderTitle(_cmsgs.shareTitle(info.what));

        SmartTable panel = new SmartTable();
        panel.setCellPadding(20);

        String token = info.page.makeToken(info.args);
        String goURL = URL.encodeComponent(DeploymentConfig.serverURL + "go/" + token);
        String welcURL = URL.encodeComponent(DeploymentConfig.serverURL + "welcome/" +
            (CShell.isGuest() ? "0" : CShell.getMemberId()) + "/" + token);
        String eTitle = URL.encodeComponent(info.title);
        String eDesc = URL.encodeComponent(info.descrip);

        // facebook
        final String facebookURL = "http://www.facebook.com/sharer.php" +
            "?u=" + welcURL + "&t=" + eTitle;
        ClickHandler facebookListener = new ClickHandler() {
            public void onClick (ClickEvent event) {
                Window.open(facebookURL, "Whirled", "width=620,height=440");
            }
        };
        panel.setWidget(0, 0, MsoyUI.createButtonPair(
            MsoyUI.createActionImage(FACEBOOK_IMG, facebookListener),
            MsoyUI.createActionLabel(_cmsgs.shareFacebook(), facebookListener)));

        // myspace
        final String myspaceURL = "http://www.myspace.com/index.cfm?fuseaction=postto" +
            "&u=" + welcURL + "&t=" + eTitle + "&l=1" +
            // TODO: change this to the embed, and not the snapshot?
            "&c=" + URL.encodeComponent("<img src='" + info.image.getMediaPath() + "'>");
        ClickHandler myspaceListener = new ClickHandler() {
            public void onClick (ClickEvent event) {
                Window.open(myspaceURL, "Whirled", "width=1024,height=650");
            }
        };
        panel.setWidget(0, 1, MsoyUI.createButtonPair(
            MsoyUI.createActionImage(MYSPACE_IMG, myspaceListener),
            MsoyUI.createActionLabel(_cmsgs.shareMyspace(), myspaceListener)));

        // Digg:
        // ... is a little different: we need to use the "go" url because all submitted
        // urls should be the same. We also just create a raw HTML link that opens
        // a new page
        String diggURL = "http://digg.com/submit" +
            "?url=" + goURL + "&title=" + eTitle + "&bodytext=" + eDesc +
            "&media=news&topic=playable_web_games";
        panel.setWidget(0, 2, new HTML("<a target='_blank' href='" + diggURL + "'>" +
                                       "<img src='/images/ui/digg.png' border=0></a>"));

        setContents(panel);
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final String FACEBOOK_IMG =
        "http://b.static.ak.fbcdn.net/images/share/facebook_share_icon.gif?8:26981";
    protected static final String MYSPACE_IMG =
        "http://cms.myspacecdn.com/cms/post_myspace_icon.gif";
}
