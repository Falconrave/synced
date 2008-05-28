//
// $Id$

package client.games;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.PlayerRating;

import client.shell.Application;
import client.util.MsoyUI;
import client.util.ThumbBox;

/**
 * Displays top-rankings for a particular game.
 */
public class TopRankingPanel extends VerticalPanel
{
    public TopRankingPanel (int gameId, boolean onlyMyFriends)
    {
        setStyleName("topRankingPanel");
        _gameId = gameId;
        _onlyMyFriends = onlyMyFriends;
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _gameId == 0) {
            return;
        }

        // it's possible to have this tab shown and be a guest; so we avoid freakoutage
        if (_onlyMyFriends && CGames.isGuest()) {
            addNote(CGames.msgs.trpLogin());
            return;
        }

        addNote(CGames.msgs.trpLoading());
        CGames.gamesvc.loadTopRanked(CGames.ident, _gameId, _onlyMyFriends, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotRankings((PlayerRating[][])result);
            }
            public void onFailure (Throwable caught) {
                CGames.log("getTopRanked failed", caught);
                addNote(CGames.serverError(caught));
            }
        });
        _gameId = 0; // note that we've asked for our data
    }

    protected void gotRankings (PlayerRating[][] results)
    {
        clear();

        int totalRows = Math.max(results[0].length, results[1].length);
        if (totalRows == 0) {
            addNote(_onlyMyFriends ? CGames.msgs.trpMyNoRankings() :
                      CGames.msgs.trpTopNoRankings());
            return;
        }

        add(_grid = new SmartTable("Grid", 3, 0));

        int col = 0;
        if (results[0].length > 0) {
            displayRankings(CGames.msgs.trpSingleHeader(), col, results[0], totalRows);
            col += COLUMNS;
        }
        if (results[1].length > 0) {
            displayRankings(CGames.msgs.trpMultiHeader(), col, results[1], totalRows);
            col += COLUMNS;
        }

        String text = _onlyMyFriends ? CGames.msgs.trpSingleTip() : CGames.msgs.trpMultiTip();
        add(MsoyUI.createLabel(text, "Footer"));
    }

    protected void displayRankings (String header, int col, PlayerRating[] results, int totalRows)
    {
        for (int cc = 0; cc < COLUMNS; cc++) {
            int hcol = (cc + col);
            switch (cc) {
            case 2: _grid.setText(0, hcol, header); break;
            case 3: _grid.setText(0, hcol, CGames.msgs.trpRatingHeader()); break;
            default: _grid.setHTML(0, hcol, "&nbsp;"); break;
            }
            _grid.getFlexCellFormatter().setStyleName(0, hcol, (cc == 3) ? "HeaderTip" : "Header");
        }

        for (int ii = 0; ii < totalRows; ii++) {
            final PlayerRating rating = results[ii];
            int row = 1 + ii*2;
            if (ii >= results.length || rating.name == null) {
                for (int cc = 0; cc < COLUMNS; cc++) {
                    _grid.setHTML(row, cc+col, "&nbsp;", 1, "Cell");
                }
                continue;
            }

            _grid.setText(row, col, CGames.msgs.gameRank("" + (ii+1)), 1, "Cell");
            _grid.getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_RIGHT);

            ThumbBox box = new ThumbBox(rating.photo, MediaDesc.QUARTER_THUMBNAIL_SIZE, null);
            _grid.setWidget(row, col+1, box, 1, "Cell");
            _grid.getFlexCellFormatter().addStyleName(row, col+1, "Photo");
            _grid.getFlexCellFormatter().setHorizontalAlignment(
                row, col+1, HasAlignment.ALIGN_CENTER);
            _grid.getFlexCellFormatter().setVerticalAlignment(
                row, col+1, HasAlignment.ALIGN_MIDDLE);

            _grid.setWidget(row, col+2, Application.memberViewLink(rating.name), 1, "Cell");

            _grid.setText(row, col+3, ""+rating.rating, 1, "Cell");
            _grid.getFlexCellFormatter().setHorizontalAlignment(
                row, col+3, HasAlignment.ALIGN_RIGHT);

            _grid.setHTML(row, col+4, "&nbsp;", 1, "Cell");
            _grid.getFlexCellFormatter().addStyleName(row, col+4, "Gap");

            if (rating.name.getMemberId() == CGames.getMemberId()) {
                for (int cc = 0; cc < COLUMNS; cc++) {
                    _grid.getFlexCellFormatter().addStyleName(row, col+cc, "Self");
                }
            }
        }
    }

    protected void addNote (String text)
    {
        add(MsoyUI.createLabel(text, "Note"));
    }

    protected int _gameId;
    protected boolean _onlyMyFriends;
    protected SmartTable _grid;

    protected static final int COLUMNS = 5;
}
