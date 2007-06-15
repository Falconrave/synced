//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.nio.ByteBuffer;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

/**
 * Scene related utility methods.
 */
public class SceneUtil
{
    /**
     * Creates a {@link MediaDesc} of the appropriate type based on the supplied hash and mime
     * type. The hash should previously have been created by calling {@link #flattenMediaDesc} on a
     * media descriptor.
     */
    public static MediaDesc createMediaDesc (byte[] mediaHash, byte mimeType)
    {
        if (mediaHash.length == 4 || mediaHash.length == 8) {
            // note: 8-byte descriptors are no longer supported
            // only the itemType int is used, and the media type is always assumed to be Furni
            byte itemType = (byte)ByteBuffer.wrap(mediaHash).asIntBuffer().get();
            return Item.getDefaultFurniMediaFor(itemType);
        } else {
            return new MediaDesc(mediaHash, mimeType);
        }
    }

    /**
     * Flattens the supplied {@link MediaDesc} into bytes that can later be decoded by
     * {@link #createMediaDesc} into the appropriate type of descriptor.
     */
    public static byte[] flattenMediaDesc (MediaDesc desc)
    {
        if (desc instanceof StaticMediaDesc) {
            StaticMediaDesc sdesc = (StaticMediaDesc)desc;

            // sanity check; if we later need to flatten other static types than furni, we can have
            // the type constant map to an integer and stuff that into the byte array as well
            if (!sdesc.getMediaType().equals(Item.FURNI_MEDIA)) {
                throw new IllegalArgumentException(
                    "Cannot flatten non-furni static media " + desc + ".");
            }

            ByteBuffer data = ByteBuffer.allocate(4);
            data.asIntBuffer().put(sdesc.getItemType());
            return data.array();

        } else {
            return desc.hash;
        }
    }
}
