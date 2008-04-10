//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import client.shell.Application;

/**
 * Displays content with a tongue label header and an optional right-aligned footer widget.
 */
public class TongueBox extends SmartTable
{
    public TongueBox ()
    {
        super("tongueBox", 0, 0);
    }

    public TongueBox (String title, Widget content)
    {
        this(); // not sure if zero argument constructor is automatically called
        if (title != null) {
            setHeader(title);
        }
        if (content != null) {
            setContent(content);
        }
    }

    public TongueBox (String title, String content, boolean isHTML)
    {
        this(); // not sure if zero argument constructor is automatically called
        if (title != null) {
            setHeader(title);
        }
        if (content != null) {
            setContent(content, isHTML);
        }
    }

    public void setHeader (String title)
    {
        SmartTable header = new SmartTable("Header", 0, 0);
        header.setText(0, 0, title, 1, "Base");
        Image line = new Image("/images/ui/grey_line.png");
        line.setWidth("100%");
        line.setHeight("1px");
        header.setWidget(0, 1, line, 1, "Line");
        setWidget(0, 0, header);
    }

    public void setContent (Widget content)
    {
        setWidget(1, 0, content, 1, "Content");
    }

    public void setContent (String content, boolean isHTML)
    {
        if (isHTML) {
            setHTML(1, 0, content);
            getFlexCellFormatter().setStyleName(1, 0, "Content");
        } else {
            setText(1, 0, content, 1, "Content");
        }
    }

    public void setFooterLink (String text, String page, String args)
    {
        setFooter(Application.createLink(text, page, args));
    }

    public Label setFooterLabel (String text, ClickListener onClick)
    {
        // annoyingly we have to stick our label into a table otherwise the Label (a div) will
        // actually be the entire width of the page; if it's clickable, that is very weird
        HorizontalPanel box = new HorizontalPanel();
        Label label = MsoyUI.createActionLabel(text, onClick);
        box.add(label);
        setFooter(box);
        return label;
    }

    public void setFooter (Widget widget)
    {
        if (widget == null) {
            clearCell(2, 0);
        } else {
            setWidget(2, 0, widget, 1, "Footer");
            getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_RIGHT);
        }
    }
}
