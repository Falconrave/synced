package com.threerings.msoy.ui {

import com.threerings.flash.MediaContainer;

import mx.core.Container;

/**
 * Wraps a MediaContainer into a UIComponent.
 */
public class MediaWrapper extends Container
{
    /**
     * @param cont the container to wrap
     * @param altReportedWidth a width to report when the media width is 0.
     * @param altReportedHeight a height to report when the media height is 0.
     */
    public function MediaWrapper (
        cont :MediaContainer,
        altReportedWidth :Number = 0, altReportedHeight :Number = 0, alwaysUseAlt :Boolean = false)
    {
        _cont = cont;
        _altWidth = altReportedWidth;
        _altHeight = altReportedHeight;
        _alwaysUseAlt = alwaysUseAlt;
        rawChildren.addChild(cont);
        if (!alwaysUseAlt) {
            cont.addEventListener(MediaContainer.SIZE_KNOWN, handleMediaSizeChanged,
                false, 0, true);
        }
    }

    public function getMediaContainer () :MediaContainer
    {
        return _cont;
    }

    override protected function measure () :void
    {
        // nothing needed
    }

    override protected function updateDisplayList (unscaledWidth: Number, 
        unscaledHeight :Number) :void
    {
        _cont.containerDimensionsUpdated(unscaledWidth, unscaledHeight);
    }

    override public function get measuredWidth () :Number
    {
        var w :Number = _cont.getContentWidth();
        return (w == 0 || _alwaysUseAlt) ? _altWidth : w;
    }

    override public function get measuredHeight () :Number
    {
        var h :Number = _cont.getContentHeight();
        return (h == 0 || _alwaysUseAlt) ? _altHeight : h;
    }

    /**
     * React to changes in the size of the MediaContainer.
     */
    protected function handleMediaSizeChanged (evt :Object) :void
    {
        invalidateSize();
    }

    protected var _cont :MediaContainer;

    protected var _altWidth :Number;
    protected var _altHeight :Number;

    protected var _alwaysUseAlt :Boolean;
}
}
