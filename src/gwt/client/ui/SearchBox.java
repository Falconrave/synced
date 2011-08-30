//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import client.shell.ShellMessages;

/**
 * A text box to use for search fields. It says "<search>" in it (which is cleared out when it is
 * focused) and it handles listening for pressing enter.
 */
public class SearchBox extends HorizontalPanel
{
    public static interface Listener {
        void search (String query);
        void clearSearch ();
    }

    public SearchBox (Listener listener)
    {
        setStyleName("searchBox");
        _listener = listener;

        add(_input = new TextBox());
        _input.addKeyDownHandler(new KeyDownHandler() {
            public void onKeyDown (KeyDownEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    doSearch();
                }
            }
        });
        _input.addFocusHandler(new FocusHandler() {
            public void onFocus (FocusEvent event) {
                if (_input.getText().equals(_cmsgs.searchDefault())) {
                    _input.removeStyleName("Faded");
                    _input.setText("");
                }
            }
        });
        _close = MsoyUI.createCloseButton(new ClickHandler() {
            public void onClick (ClickEvent event) {
                clearSearch(true);
            }
        });
        clearSearch(false);
    }

    /**
     * Creates a click listener that can be added to a "search" button to go along with this box.
     */
    public ClickHandler makeSearchListener () {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                doSearch();
            }
        };
    }

    /**
     * Configures the contents of the search box.
     */
    public void setText (String text)
    {
        if (text == null || text.length() == 0) {
            clearSearch(false);
        } else {
            _input.removeStyleName("Faded");
            _input.setText(text);
            if (!_close.isAttached()) {
                add(_close);
            }
        }
    }

    @Override // from Widget
    protected void onLoad ()
    {
        // on load, if there is search criteria in the box, focus on it
        super.onLoad();
        DeferredCommand.addCommand(new Command() {
            public void execute () {
                if (!_input.getText().equals(_cmsgs.searchDefault())
                    && _input.getText().length() > 0) {
                    _input.setFocus(true);
                }
            }
        });
    }

    protected void doSearch ()
    {
        String query = _input.getText().trim();
        if (query.length() == 0) {
            clearSearch(true);
        } else {
            _listener.search(query);
            if (!_close.isAttached()) {
                add(_close);
            }
        }
    }

    protected void clearSearch (boolean informListener)
    {
        _input.setText(_cmsgs.searchDefault());
        _input.addStyleName("Faded");
        _input.setFocus(false);
        if (informListener) {
            _listener.clearSearch();
        }
        if (_close.isAttached()) {
            remove(_close);
        }
    }

    protected Listener _listener;
    protected TextBox _input;
    protected Widget _close;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
