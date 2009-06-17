//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.TrophyCase;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.trophy.TrophyGrid;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.InfoCallback;

/**
 * Displays all trophies owned by the specified player.
 */
public class TrophyCasePanel extends VerticalPanel
{
    public TrophyCasePanel (int memberId)
    {
        setStyleName("trophyCase");

        if (memberId <= 0) {
            setHeader(_msgs.noSuchPlayer());
            return;
        }

        add(MsoyUI.createNowLoading());
        _gamesvc.loadTrophyCase(memberId, new InfoCallback<TrophyCase>() {
            public void onSuccess (TrophyCase tc) {
                setTrophyCase(tc);
            }
        });
    }

    protected void setTrophyCase (TrophyCase tcase)
    {
        if (tcase == null) {
            setHeader(_msgs.noSuchPlayer());
            return;
        }

        CShell.frame.setTitle(_msgs.caseTitle(tcase.owner.toString()));
        if (tcase.shelves.length == 0) {
            setHeader((CShell.getMemberId() == tcase.owner.getMemberId()) ?
                     _msgs.caseEmptyMe() : _msgs.caseEmpty());
            return;
        }

        setHeader(_msgs.caseBlurb());
        for (int ii = 0; ii < tcase.shelves.length; ii++) {
            TrophyCase.Shelf shelf = tcase.shelves[ii];
            TongueBox box = new TongueBox(shelf.name, new TrophyGrid(shelf.trophies));
            int ownerId = tcase.owner.getMemberId();
            if (!CShell.isGuest() && CShell.getMemberId() != ownerId) {
                box.setFooterLink(_msgs.caseCompare(), Pages.GAMES, "ct", shelf.gameId, ownerId);
            }
            add(box);
        }
    }

    protected void setHeader (String title)
    {
        clear();
        add(new TongueBox(null, title, false));
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
