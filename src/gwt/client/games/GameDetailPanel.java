//
// $Id$

package client.games;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.GameDetail;

import client.item.ItemRating;
import client.shell.Application;
import client.shell.Args;
import client.shell.CommentsPanel;
import client.shell.Frame;
import client.shell.Page;
import client.util.CreatorLabel;
import client.util.ItemUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.StyledTabPanel;
import client.util.ThumbBox;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends SmartTable
    implements TabListener
{
    public static final String INSTRUCTIONS_TAB = "i";
    public static final String COMMENTS_TAB = "c";
    public static final String TROPHIES_TAB = "t";
    public static final String MYRANKINGS_TAB = "mr";
    public static final String TOPRANKINGS_TAB = "tr";
    public static final String METRICS_TAB = "m";

    public GameDetailPanel ()
    {
        super("gameDetail", 0, 10);
    }

    public void setGame (final int gameId, final String tab)
    {
        if (_gameId == gameId) {
            selectTab(tab);
        } else {
            CGames.gamesvc.loadGameDetail(CGames.ident, gameId, new MsoyCallback() {
                public void onSuccess (Object result) {
                    setGameDetail(gameId, (GameDetail)result);
                    selectTab(tab);
                }
            });
        }
    }

    public void setGameDetail (int gameId, GameDetail detail)
    {
        // Note: the gameId may be the negative original gameId, but GameDetail's id is never 
        // negative to match
        _gameId = gameId;
        Frame.setTitle(detail.getGame().name);

        Game game = detail.getGame();
        VerticalPanel shot = new VerticalPanel();
        shot.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        shot.add(new ThumbBox(game.getShotMedia(), Game.SHOT_WIDTH, Game.SHOT_HEIGHT, null));
        if (detail.listedItem != null) {
            shot.add(WidgetUtil.makeShim(5, 5));
            shot.add(new ItemRating(detail.listedItem, detail.memberRating, false));
        }
        setWidget(0, 0, shot);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        setWidget(0, 1, new GameNamePanel(
                      game.name, game.genre, detail.creator, game.description), 2, null);
        setWidget(1, 0, new GameBitsPanel(detail.minPlayers, detail.maxPlayers,
                                          detail.averageDuration, detail.gamesPlayed));
        setWidget(1, 1, new PlayPanel(_gameId, detail.minPlayers, detail.maxPlayers,
                                      detail.playingNow), 1, "Play");

        // note that they're playing the developer version if so
        if (_gameId < 0) {
            addText(CGames.msgs.gdpDevVersion(), 3, "InDevTip");
        }

        _tabs = new StyledTabPanel();
        _tabs.addTabListener(this);
        addWidget(_tabs, 3, null);

        // add the about/instructions tab
        addTab(INSTRUCTIONS_TAB, CGames.msgs.tabInstructions(), new InstructionsPanel(detail));

        // add comments tab
        if (detail.listedItem != null) {
            addTab(COMMENTS_TAB, CGames.msgs.tabComments(),
                   new CommentsPanel(detail.listedItem.getType(), detail.listedItem.catalogId));
        }

        // add trophies tab, passing in the potentially negative gameId
        addTab(TROPHIES_TAB, CGames.msgs.tabTrophies(), new GameTrophyPanel(gameId));

        // add top rankings tabs
        if (!CGames.isGuest()) {
            addTab(MYRANKINGS_TAB, CGames.msgs.tabMyRankings(),
                   new TopRankingPanel(detail.gameId, true));
        }
        addTab(TOPRANKINGS_TAB, CGames.msgs.tabTopRankings(),
               new TopRankingPanel(detail.gameId, false));

        // if we're the owner of the game or an admin, add the metrics tab
        if ((detail.sourceItem != null && detail.sourceItem.ownerId == CGames.getMemberId()) ||
            CGames.isAdmin()) {
            addTab(METRICS_TAB, CGames.msgs.tabMetrics(), new GameMetricsPanel(detail));
        }
    }

    // from interface TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // route tab selection through the URL
        String tabCode = getTabCode(tabIndex);
        if (!tabCode.equals(_seltab)) {
            Application.go(Page.GAMES, Args.compose("d", ""+_gameId, tabCode));
            return false;
        } else {
            return true;
        }
    }

    // from interface TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // nada
    }

    protected void addTab (String ident, String title, Widget tab)
    {
        _tabs.add(tab, title);
        _tabmap.put(ident, new Integer(_tabs.getWidgetCount()-1));
    }

    protected void selectTab (String tab)
    {
        Integer tosel = (Integer)_tabmap.get(tab);
        if (tosel == null) {
            _seltab = getTabCode(0);
            _tabs.selectTab(0);
        } else {
            _seltab = tab;
            _tabs.selectTab(tosel.intValue());
        }
    }

    protected String getTabCode (int tabIndex)
    {
        for (Iterator iter = _tabmap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            if (((Integer)entry.getValue()).intValue() == tabIndex) {
                return (String)entry.getKey();
            }
        }
        return "";
    }

    protected StyledTabPanel _tabs;
    protected int _gameId;
    protected String _seltab;
    protected HashMap _tabmap = new HashMap();
}
