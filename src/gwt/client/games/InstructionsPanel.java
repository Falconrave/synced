//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.item.RichTextToolbar;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.SafeHTML;
import client.util.InfoCallback;

/**
 * Displays the instructions for a game.
 */
public class InstructionsPanel extends VerticalPanel
{
    public InstructionsPanel (GameDetail detail)
    {
        setStyleName("instructionsPanel");
        _detail = detail;
        showInstructions();
    }

    protected void showInstructions ()
    {
        clear();
        DOM.setStyleAttribute(getElement(), "color", "");
        DOM.setStyleAttribute(getElement(), "background", "none");

        setHorizontalAlignment(ALIGN_LEFT);
        if (_detail.instructions == null || _detail.instructions.length() == 0) {
            add(new Label(_msgs.ipNoInstructions()));
        } else {
            // snip off our background color if we have one
            String[] bits = decodeInstructions(_detail.instructions);
            add(new SafeHTML(bits[0]));
            if (bits[1] != null) {
                DOM.setStyleAttribute(getElement(), "color", bits[1]);
            }
            if (bits[2] != null) {
                DOM.setStyleAttribute(getElement(), "background", bits[2]);
            }
        }

        // if this is the owner of the game, add an edit button below the instructions
        if (_detail.info.isCreator(CShell.getMemberId()) || CShell.isAdmin()) {
            setHorizontalAlignment(ALIGN_RIGHT);
            add(new Button("Edit", new ClickHandler() {
                public void onClick (ClickEvent event) {
                    editInstructions();
                }
            }));
        }
    }

    protected String[] decodeInstructions (String instructions)
    {
        String[] results = new String[3];
        if (instructions.length() > 9 && instructions.substring(0, 10).matches("{t#......}")) {
            results[1] = instructions.substring(2, 9);
            instructions = instructions.substring(10);
        }
        if (instructions.length() > 10 && instructions.substring(0, 11).matches("{bg#......}")) {
            results[2] = instructions.substring(3, 10);
            instructions = instructions.substring(11);
        }
        results[0] = instructions;
        return results;
    }

    protected void editInstructions ()
    {
        clear();
        DOM.setStyleAttribute(getElement(), "color", "");
        DOM.setStyleAttribute(getElement(), "background", "none");

        final RichTextArea editor = new RichTextArea();
        editor.setWidth("100%");
        editor.setHeight("300px");

        setHorizontalAlignment(ALIGN_LEFT);
        final RichTextToolbar toolbar = new RichTextToolbar(editor, true);
        add(toolbar);
        add(editor);

        if (_detail.instructions != null) {
            String[] bits = decodeInstructions(_detail.instructions);
            editor.setHTML(bits[0]);
            toolbar.setPanelColors(bits[1], bits[2]);
        }

        setHorizontalAlignment(ALIGN_RIGHT);
        Button cancel = new Button("Cancel", new ClickHandler() {
            public void onClick (ClickEvent event) {
                showInstructions();
            }
        });
        Button update = new Button("Update", new ClickHandler() {
            public void onClick (ClickEvent event) {
                String instructions = editor.getHTML();
                String bgcolor = toolbar.getBackgroundColor();
                if (bgcolor != null && bgcolor.matches("#......")) {
                    instructions = "{bg" + bgcolor + "}" + instructions;
                }
                String tcolor = toolbar.getTextColor();
                if (tcolor != null && tcolor.matches("#......")) {
                    instructions = "{t" + tcolor + "}" + instructions;
                }
                saveInstructions(instructions);
            }
        });
        add(MsoyUI.createButtonPair(cancel, update));
    }

    protected void saveInstructions (final String instructions)
    {
        if (instructions.length() > GameDetail.MAX_INSTRUCTIONS_LENGTH) {
            int excess = instructions.length() - GameDetail.MAX_INSTRUCTIONS_LENGTH;
            MsoyUI.error(_msgs.ipInstructionsTooLong(""+excess));
            return;
        }
        _gamesvc.updateGameInstructions(_detail.gameId, instructions, new InfoCallback<Void>() {
            public void onSuccess (Void result) {
                _detail.instructions = instructions;
                showInstructions();
            }
        });
    }

    protected GameDetail _detail;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
