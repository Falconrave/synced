//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.orth.data.MediaDesc;

/**
 * Set of room decor information, including room settings and a background bitmap.
 */
public class Decor extends Item
{
    /** Type constant for a room with no background, just bare walls. This constant is deprecated,
     *  please do not use. Legacy decor of this type will be drawn using default type. */
    public static final byte DRAWN_ROOM_DEPRECATED = 0;

    /** Type constant for a standard room. The room will use standard layout, and its background
     *  image will be drawn behind all furniture. */
    public static final byte IMAGE_OVERLAY = 1;

    /** Type constant for a room whose background is fixed to the viewport, instead of scene. */
    public static final byte FIXED_IMAGE = 2;

    /** Type constant for a room with non-standard, flat layout. */
    public static final byte FLAT_LAYOUT = 3;

    /** Type constant for a room with a bird's eye view layout. */
    public static final byte TOPDOWN_LAYOUT = 4;

    /** The number of type constants. */
    public static final int TYPE_COUNT = 5;

    /** Room type. Specifies how the background wallpaper and layout are handled. */
    public byte type;

    /** Room height, in pixels. */
    public short height;

    /** Room width, in pixels. */
    public short width;

    /** Room depth, in pixels. */
    public short depth;

    /** Horizon position, in [0, 1]. */
    public float horizon;

    /** Specifies whether side walls should be displayed. */
    public boolean hideWalls;

    /** The adjusted scale of actors in this room. */
    public float actorScale;

    /** The adjusted scale of furni in this room. */
    public float furniScale;

    @Override // from Item
    public MsoyItemType getType ()
    {
        return MsoyItemType.DECOR;
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && (_furniMedia.hasFlashVisual() || _furniMedia.isRemixed()) &&
            nonBlank(name, MAX_NAME_LENGTH) &&
            type < TYPE_COUNT && width > 0 && height > 0 && depth > 0 &&
            horizon <= 1.0f && horizon >= 0.0f && actorScale > 0 && furniScale > 0;
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        return getFurniMedia();
    }

    @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return null; // there is no default
    }
}
