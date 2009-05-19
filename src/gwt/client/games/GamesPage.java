//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.game.data.all.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil.GameDetails;

import client.shell.Page;

/**
 * Displays a page that allows a player to play a particular game. If it's single player the game
 * is shown, if it's multiplayer the lobby is first shown where the player can find opponents
 * against which to play.
 */
public class GamesPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we have d-NNN then we want to see game detail
        String action = args.get(0, "");
        if (action.equals("d")) {
            GameDetailPanel panel;
            if (getContent() instanceof GameDetailPanel) {
                panel = (GameDetailPanel)getContent();
            } else {
                setContent(panel = new GameDetailPanel());
            }
            panel.setGame(args.get(1, 0), GameDetails.getByCode(args.get(2, "")));

        } else if (action.equals("t")) {
            setContent(new TrophyCasePanel(args.get(1, 0)));

        } else if (action.equals("ct")) {
            setContent(_msgs.compareTitle(),
                new TrophyComparePanel(args.get(1, 0), args.get(2, 0)));

        } else if (action.equals("g")) {
            setContent(new GameGenrePanel((byte)args.get(1, GameGenre.ALL),
                                          GameInfo.Sort.fromToken(args.get(2, "")),
                                          args.get(3, null)));

        } else if (action.equals("m")) {
            setContent(new MyGamesPanel(GameInfo.Sort.fromToken(args.get(2, ""))));

        } else if (action.equals("c")) {
            setContent(_msgs.cgTitle(), new CreateGamePanel());

        } else if (action.equals("e")) {
            EditGamePanel panel;
            if (getContent() instanceof EditGamePanel) {
                panel = (EditGamePanel)getContent();
            } else {
                setContent(_msgs.egTitle(), panel = new EditGamePanel());
            }
            panel.setGame(args.get(1, 0), args.get(2, 0));

        } else {
            setContent(new ArcadePanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.GAMES;
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
