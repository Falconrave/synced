//
// $Id$

package client.trophy;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.NaviUtil;

/**
 * Displays a grid of trophies, each of which link to their respective game's detail page.
 */
public class TrophyGrid extends FlexTable
{
    public static final int COLUMNS = 6;

    public static void populateTrophyGrid (FlexTable grid, Trophy[] trophies)
    {
        grid.addStyleName("trophyGrid");

        int row = 0;
        for (int ii = 0; ii < trophies.length; ii++) {
            if (ii > 0 && ii % COLUMNS == 0) {
                row += 2;
            }
            int col = ii % COLUMNS;

            final Trophy trophy = trophies[ii];
            ClickHandler trophyClick = new ClickHandler() {
                public void onClick (ClickEvent event) {
                    Link.go(Pages.GAMES, NaviUtil.GameDetails.TROPHIES.args(trophy.gameId));
                }
            };

            grid.setWidget(row, col, MediaUtil.createMediaView(
                               trophy.trophyMedia, MediaDescSize.THUMBNAIL_SIZE, trophyClick));
            grid.getFlexCellFormatter().setStyleName(row, col, "Trophy");
            grid.setWidget(row+1, col, MsoyUI.createActionLabel(trophy.name, trophyClick));
            grid.getFlexCellFormatter().setStyleName(row+1, col, "Name");
        }

        // if they have only one or two trophies total, we need to add blank cells to ensure that
        // things are only as wide as appropriate
        for (int ii = trophies.length; ii < COLUMNS; ii++) {
            grid.setText(0, ii, "");
            grid.getFlexCellFormatter().setStyleName(0, ii, "Trophy");
        }
    }

    public TrophyGrid (Trophy[] trophies)
    {
        setCellSpacing(5);
        setCellPadding(0);
        populateTrophyGrid(this, trophies);
    }
}
