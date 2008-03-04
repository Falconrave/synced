//
// $Id$

package client.util;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.shell.CShell;

/**
 * Contains useful user interface related methods.
 */
public class MsoyUI
{
    /**
     * Creates a label with the supplied text and style.
     */
    public static Label createLabel (String text, String styleName)
    {
        Label label = new Label(text);
        if (styleName != null) {
            label.setStyleName(styleName);
        }
        return label;
    }

    /**
     * Creates a label that triggers an action using the supplied text and listener.
     */
    public static Label createActionLabel (String text, ClickListener listener)
    {
        return createActionLabel(text, null, listener);
    }

    /**
     * Creates a label that triggers an action using the supplied text and listener. The label will
     * be styled as specified with an additional style that configures the mouse pointer and adds
     * underline to the text.
     */
    public static Label createActionLabel (String text, String style, ClickListener listener)
    {
        Label label = createCustomActionLabel(text, style, listener);
        label.addStyleName("actionLabel");
        return label;
    }

    /**
     * Creates a label that triggers an action using the supplied text and listener. The label will
     * only be styled with the specified style.
     */
    public static Label createCustomActionLabel (String text, String style, ClickListener listener)
    {
        Label label = createLabel(text, style);
        if (listener != null) {
            label.addClickListener(listener);
        }
        return label;
    }

    /**
     * Creates a text box with all of the configuration that you're bound to want to do.
     */
    public static TextBox createTextBox (String text, int maxLength, int visibleLength)
    {
        TextBox box = new TextBox();
        if (text != null) {
            box.setText(text);
        }
        if (maxLength > 0) {
            box.setMaxLength(maxLength);
        }
        if (visibleLength > 0) {
            box.setVisibleLength(visibleLength);
        }
        return box;
    }

    /**
     * Creates a text area with all of the configuration that you're bound to want to do.
     */
    public static TextArea createTextArea (String text, int width, int height)
    {
        TextArea area = new TextArea();
        if (text != null) {
            area.setText(text);
        }
        area.setCharacterWidth(width);
        area.setVisibleLines(height);
        return area;
    }

    /**
     * Creates a button with tiny text.
     */
    public static Button createTinyButton (String label, ClickListener listener)
    {
        Button button = new Button(label, listener);
        button.addStyleName("tinyButton");
        return button;
    }

    /**
     * Creates a button with big text.
     */
    public static Button createBigButton (String label, ClickListener listener)
    {
        Button button = new Button(label, listener);
        button.addStyleName("bigButton");
        return button;
    }

    /**
     * Creates a button for closing things (a square with an x in it).
     */
    public static Widget createCloseButton (ClickListener listener)
    {
        final Label close = createActionLabel("", "closeButton", listener);
        close.addMouseListener(new MouseListenerAdapter() {
            public void onMouseEnter (Widget sender) {
                close.addStyleDependentName("hovering");
            }
            public void onMouseLeave (Widget sender) {
                close.removeStyleDependentName("hovering");
            }
        });
        return close;
    }

    /**
     * Creates a pair of previous and next buttons in a horizontal panel.
     */
    public static Widget createPrevNextButtons (ClickListener onPrev, ClickListener onNext)
    {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setStyleName("pagedGrid"); // hijack PagedGrid styles
        Button prev = new Button(CShell.cmsgs.prev());
        prev.setStyleName("Button");
        prev.addStyleName("PrevButton");
        prev.addClickListener(onPrev);
        panel.add(prev);
        panel.add(WidgetUtil.makeShim(5, 5));
        Button next = new Button(CShell.cmsgs.next());
        next.setStyleName("Button");
        next.addStyleName("NextButton");
        next.addClickListener(onNext);
        panel.add(next);
        return panel;
    }

    /**
     * Creates an arrow that does History.back().
     */
    public static Image createBackArrow ()
    {
        return createBackArrow(new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        });
    }

    /**
     * Creates an arrow that invokes the specified callback.
     */
    public static Image createBackArrow (ClickListener callback)
    {
        return createActionImage("/images/ui/back_arrow.png", callback);
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (String path, ClickListener onClick)
    {
        return createActionImage(path, null, onClick);
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (String path, String tip, ClickListener onClick)
    {
        Image image = new Image(path);
        image.addStyleName("actionLabel");
        image.addClickListener(onClick);
        if (tip != null) {
            image.setTitle(tip);
        }
        return image;
    }

    /**
     * Creates an image that will render inline with text (rather than forcing a break).
     */
    public static Image createInlineImage (String path)
    {
        Image image = new Image(path);
        image.setStyleName("inline");
        return image;
    }

    /**
     * Creates a box with a rounded header. Additional bits can be added as long as they are
     * colspan 3.
     */
    public static SmartTable createHeaderBox (String icon, String title)
    {
        SmartTable box = new SmartTable("headerBox", 0, 0);
        box.setWidget(0, 0, new Image("/images/ui/box/header_left.png"), 1, "Corner");
        if (icon != null) {
            FlowPanel tbox = new FlowPanel();
            tbox.add(new Image(icon));
            tbox.add(new Label(title));
            box.setWidget(0, 1, tbox, 1, "Title");
        } else {
            box.setText(0, 1, title, 1, "Title");
        }
        box.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_CENTER);
        box.setWidget(0, 2, new Image("/images/ui/box/header_right.png"), 1, "Corner");
        return box;
    }

    /**
     * Creates a box with a rounded header.
     */
    public static Widget createHeaderBox (String icon, String title, Widget contents)
    {
        SmartTable box = createHeaderBox(icon, title);
        box.addWidget(contents, 3, "Contents");
        return box;
    }

    /**
     * Displays informational feedback to the user in a non-offensive way.
     */
    public static void info (String message)
    {
        infoAction(message, null, null);
    }

    /**
     * Displays informational feedback along with an action button which will dismiss the info
     * display and take an action.
     */
    public static void infoAction (String message, String actionLabel, ClickListener action)
    {
        HorizontalPanel panel = new HorizontalPanel();
        final InfoPopup popup = new InfoPopup(panel);
        ClickListener hider = new ClickListener() {
            public void onClick (Widget sender) {
                popup.hide();
            }
        };
        panel.add(new Label(message));
        panel.add(WidgetUtil.makeShim(20, 10));
        if (actionLabel != null) {
            Button button = new Button(actionLabel, action);
            button.addClickListener(hider);
            panel.add(button);
            panel.add(WidgetUtil.makeShim(5, 10));
        }
        panel.add(new Button(CShell.cmsgs.dismiss(), hider));
        popup.show();
    }

    /**
     * Displays informational feedback to the user next to the supplied widget in a non-offensive
     * way.
     */
    public static void infoNear (String message, Widget source)
    {
        new InfoPopup(message).showNear(source);
    }

    /**
     * Displays error feedback to the user in a non-offensive way.
     */
    public static void error (String message)
    {
        // TODO: style this differently than info feedback
        new InfoPopup(message).show();
    }

//     /**
//      * Creates a pair of submit and cancel buttons in a horizontal row.
//      */
//     public static RowPanel createSubmitCancel (
//         PopupPanel popup, ClickListener onSubmit)
//     {
//         RowPanel buttons = new RowPanel();
//         buttons.add(new Button(CShell.cmsgs.submit(), onSubmit));
//         buttons.add(new Button(CShell.cmsgs.cancel(), new ClickListener() {
//             public void onClick (Widget sender) {
//                 box.hide();
//             }
//         }));
//         return buttons;
//     }
}
