//
// $Id$

package client.util;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Displays an optionally clickable thumbnail image.
 */
public class ThumbBox extends SimplePanel
{
    public ThumbBox (MediaDesc desc, ClickListener onClick)
    {
        this(desc, MediaDesc.THUMBNAIL_SIZE, onClick);
    }

    public ThumbBox (MediaDesc desc, int size, ClickListener onClick)
    {
        this(desc, MediaDesc.DIMENSIONS[2*size], MediaDesc.DIMENSIONS[2*size+1], onClick);
    }

    public ThumbBox (MediaDesc desc, int width, int height, ClickListener onClick)
    {
        setWidth(width + "px");
        setHeight(height + "px");
        DOM.setStyleAttribute(getElement(), "overflow", "hidden");
        add(MediaUtil.createMediaView(desc, width, height, onClick));
    }
}
