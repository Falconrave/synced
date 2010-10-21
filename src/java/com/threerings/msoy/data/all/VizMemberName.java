//
// $Id$

package com.threerings.msoy.data.all;

import com.threerings.util.Name;

/**
 * A member name and profile photo all rolled into one!
 *
 * <p> NOTE: this class (and all {@link Name} derivatives} must use custom field serializers (in
 * this case {@link VizMemberName_CustomFieldSerializer}) because IsSerializable only serializes
 * the fields in the class that declares that interface and all subclasses, it does not serialize
 * fields from the superclass. In this case, we have fields from our superclass that need to be
 * serialized, but we can't make {@link Name} implement IsSerializable without introducing an
 * otherwise unwanted dependency on GWT in Narya.
 *
 * <p> If you extend this class (or if you extend {@link Name}) you will have to implement a custom
 * field serializer for your derived class.
 */
public class VizMemberName extends MemberName
{
    /** The default profile photo. */
    public static final ConstrainedMediaDesc DEFAULT_PHOTO =
        new StaticMediaDesc(MediaMimeTypes.IMAGE_PNG, "photo", "profile_photo",
                            // we know that we're 50x60
                            ConstrainedMediaDesc.HALF_VERTICALLY_CONSTRAINED);

    /** For unserialization. */
    public VizMemberName ()
    {
    }

    /**
     * Creates a new name with the supplied data.
     */
    public VizMemberName (String displayName, int memberId, MediaDesc photo)
    {
        super(displayName, memberId);
        _photo = photo;
    }

    public VizMemberName (MemberName name, MediaDesc photo)
    {
        super(name.toString(), name.getMemberId());
        _photo = photo;
    }

    /**
     * Returns this member's photo.
     */
    public MediaDesc getPhoto ()
    {
        return _photo;
    }

    @Override
    public MemberName toMemberName ()
    {
        return new MemberName(_name, _memberId);
    }

    /** This member's profile photo. */
    protected MediaDesc _photo;
}
