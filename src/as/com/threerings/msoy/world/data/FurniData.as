package com.threerings.msoy.world.data {

import com.threerings.util.ClassUtil;
import com.threerings.util.Cloneable;
import com.threerings.util.Hashable;
import com.threerings.util.Util;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

public class FurniData
    implements Cloneable, Hashable, Streamable, DSet_Entry
{
    /** An actionType indicating 'no action'.
        actionData = null to capture mouse events, or "-" to pass through. */
    public static const ACTION_NONE :int = 0;

    /** An actionType indicating that actionData is a URL.
        actionData = "<url>" */
    public static const ACTION_URL :int = 1;

    /** An actionType indicating that actionData is a lobby game item id.
        actionData = "<gameId>:<gameName>" */
    public static const ACTION_LOBBY_GAME :int = 2;

    /** An actionType indicating that we're a portal.
        actionData = "<targetSceneId>:<targetSceneName>" */
    public static const ACTION_PORTAL :int = 3;

    /** An actionType indicating that actionData is a world game item id.
        actionData = "<gameId>:<gameName>" */
    public static const ACTION_WORLD_GAME :int = 4;
    
    /** The id of this piece of furni. */
    public var id :int;

    /** Identifies the type of the item that was used to create this furni,
     * or Item.NOT_A_TYPE. */
    public var itemType :int;

    /** Identifies the id of the item that was used to create this. */
    public var itemId :int;

    /** Info about the media that represents this piece of furni. */
    public var media :MediaDesc;

    /** Layout information, used for perspectivization, etc. */
    public var layoutInfo :int;

    /** The location in the scene. */
    public var loc :MsoyLocation;

    /** A scale factor in the X direction. */
    public var scaleX :Number = 1;

    /** A scale factor in the Y direction. */
    public var scaleY :Number = 1;

    /** The type of action, determines how to use actionData. */
    public var actionType :int;

    /** The action, interpreted using actionType. */
    public var actionData :String;

    /**
     * Returns the identifier for the item for which we're presenting a visualization.
     */
    public function getItemIdent () :ItemIdent
    {
        return new ItemIdent(itemType, itemId);
    }

    /**
     * Return the actionData as two strings, split after the first colon.
     * If there is no colon, then a single-element array is returned.
     */
    public function splitActionData () :Array
    {
        if (actionData == null) {
            return [ null ];
        }
        var colonDex :int = actionData.indexOf(":");
        if (colonDex == -1) {
            return [ actionData ];
        }
        return [ actionData.substring(0, colonDex),
            actionData.substring(colonDex + 1) ];
    }

    /**
     * Sets whether this furniture is in 'perspective' mode.
     * TODO: support floor/ceiling perspectivization
     */
    public function setPerspective (perspective :Boolean) :void
    {
        if (perspective) {
            layoutInfo |= 1;
        } else {
            layoutInfo &= ~1;
        }
    }

    /**
     * Is this furniture being perspectivized?
     */
    public function isPerspective () :Boolean
    {
        return (layoutInfo & 1) != 0;
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return id;
    }

    // documentation inherited from superinterface Equalable
    public function equals (other :Object) :Boolean
    {
        return (other is FurniData) &&
            (other as FurniData).id == this.id;
    }

    // documentation inherited from interface Hashable
    public function hashCode () :int
    {
        return id;
    }

    /**
     * @return true if the other FurniData is identical.
     */
    public function equivalent (that :FurniData) :Boolean
    {
        return (this.id == that.id) &&
            (this.itemType == that.itemType) &&
            (this.itemId == that.itemId) &&
            this.media.equals(that.media) &&
            this.loc.equals(that.loc) &&
            (this.layoutInfo == that.layoutInfo) &&
            (this.scaleX == that.scaleX) &&
            (this.scaleY == that.scaleY) &&
            (this.actionType == that.actionType) &&
            Util.equals(this.actionData, that.actionData);
    }

    public function toString () :String
    {
        var s :String = "Furni[id=" + id + ", itemType=" + itemType;
        if (itemType != Item.NOT_A_TYPE) {
            s += ", itemId=" + itemId;
        }
        s += ", actionType=" + actionType;
        if (actionType != ACTION_NONE) {
            s += ", actionData=\"" + actionData + "\"";
        }
        s += "]";

        return s;
    }

    /** Overwrites this instance's fields with a shallow copy of the other object. */
    protected function copyFrom (that :FurniData) :void
    {
        this.id = that.id;
        this.itemType = that.itemType;
        this.itemId = that.itemId;
        this.media = that.media;
        this.loc = that.loc;
        this.layoutInfo = that.layoutInfo;
        this.scaleX = that.scaleX;
        this.scaleY = that.scaleY;
        this.actionType = that.actionType;
        this.actionData = that.actionData;
    }
    
    // documentation inherited from interface Cloneable
    public function clone () :Object
    {
        // just a shallow copy at present
        var that :FurniData = (ClassUtil.newInstance(this) as FurniData);
        that.copyFrom(this);
        return that;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeShort(id);
        out.writeByte(itemType);
        out.writeInt(itemId);
        out.writeObject(media);
        out.writeObject(loc);
        out.writeByte(layoutInfo);
        out.writeFloat(scaleX);
        out.writeFloat(scaleY);
        out.writeByte(actionType);
        out.writeField(actionData);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readShort();
        itemType = ins.readByte();
        itemId = ins.readInt();
        media = (ins.readObject() as MediaDesc);
        loc = (ins.readObject() as MsoyLocation);
        layoutInfo = ins.readByte();
        scaleX = ins.readFloat();
        scaleY = ins.readFloat();
        actionType = ins.readByte();
        actionData = (ins.readField(String) as String);
    }
}
}
