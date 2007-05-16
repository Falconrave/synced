//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays a popup with a nice border around it.
 */
public class BorderedPopup extends PopupPanel
{
    public BorderedPopup ()
    {
        this(false);
    }

    public BorderedPopup (boolean autoHide)
    {
        super(autoHide);
        setStyleName("borderedPopup");

        super.setWidget(_widget = new BorderedWidget());

        // listen for our own closes and export that in a handy calldown method
        addPopupListener(new PopupListener() {
            public void onPopupClosed (PopupPanel panel, boolean autoClosed) {
                onClosed(autoClosed);
            }
        });
    }

    // @Override // from SimplePanel
    public void setWidget (Widget contents)
    {
        _widget.setWidget(contents);
    }

    // @Override // from PopupPanel
    public void show ()
    {
        if (_centerOnShow) {
            // start off screen so that we are not visible until we can compute our proper 
            // location and center ourselves; we'd call setPopupPosition() but that foolishly 
            // bounds our position to greater than zero, to protect us from ourselves no doubt
            Element elem = getElement();
            DOM.setStyleAttribute(elem, "left", "-5000px");
            DOM.setStyleAttribute(elem, "top", "-5000px");
            super.show();
            recenter();
        }
    }

    // @Override // from PopupPanel
    public void setPopupPosition (int left, int top)
    {
        super.setPopupPosition(left, top);
        updateFrame();
    }

    /**
     * Recenters our popup.
     */
    protected void recenter ()
    {
        setPopupPosition((Window.getClientWidth() - getOffsetWidth()) / 2,
                         (Window.getClientHeight() - getOffsetHeight()) / 2);
    }

    /**
     * Called when this popup is dismissed.
     */
    protected void onClosed (boolean autoClosed)
    {
    }

    /**
     * This must be called any time a popup's dimensions change to update the hacky iframe that
     * ensures that the popup is visible over Flash or Java applets. It is called automatically
     * when the popup position changes, but there is no way to do it magically for size changes.
     */
    protected void updateFrame ()
    {
        // if I could access 'impl' here, I wouldn't have to do this lame hack, but the GWT
        // engineers conveniently made it private, so I can't
        hide();
        super.show();
    }

    protected BorderedWidget _widget;

    /** Set this to false to disable the default centering. */
    protected boolean _centerOnShow = true;
}
