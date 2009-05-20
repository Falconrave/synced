//
// $Id$

package client.game;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.gwt.GameGenre;

import client.shell.DynamicLookup;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;

/**
 * Displays a game's name, genre, creator and description.
 */
public class GameNamePanel extends VerticalPanel
{
    public GameNamePanel (String name, GameGenre genre, MemberName creator, String descrip)
    {
        setStyleName("gameName");
        add(MsoyUI.createLabel(name, "Name"));
        if (genre != GameGenre.HIDDEN) {
            add(MsoyUI.createLabel(_dmsgs.xlate("genre_" + genre), "Genre"));
        }
        add(WidgetUtil.makeShim(5, 5));
        add(new CreatorLabel(creator));
        add(MsoyUI.createLabel(descrip, "Descrip"));
    }

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
