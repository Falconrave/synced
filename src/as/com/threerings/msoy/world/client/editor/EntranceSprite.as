//
// $Id$

package com.threerings.msoy.world.client.editor {

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.data.MsoyLocation;

public class EntranceSprite extends FurniSprite
{
    /** The sprite image used for positioning the entrance location. */
    [Embed(source="../../../../../../../../rsrc/media/entrance.png")]
    public static const MEDIA_CLASS :Class;

    public function EntranceSprite (ctx :WorldContext, location :MsoyLocation)
    {
        // fake furni data for the fake sprite
        var furniData :EntranceFurniData = new EntranceFurniData();
        furniData.media = new MediaDesc();
        furniData.loc = location;
        super(ctx, furniData);

        setMediaClass(MEDIA_CLASS);
        setLocation(location);

        // since this is an embedded image, we'll never get the COMPLETED event -
        // so let's just clean up right here.
        stoppedLoading();
    }

    // from FurniSprite
    override public function isRemovable () :Boolean
    {
        return false;
    }

    // from FurniSprite
    override public function isActionModifiable () :Boolean
    {
        return false;
    }                  
}
}
