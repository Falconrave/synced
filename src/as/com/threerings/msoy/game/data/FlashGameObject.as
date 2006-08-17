package com.threerings.msoy.game.data {

import flash.events.Event;

import flash.utils.ByteArray;

import com.threerings.util.FlashObjectMarshaller;
import com.threerings.util.Name;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.TypedArray;

import com.threerings.parlor.game.data.GameObject;
import com.threerings.parlor.turn.data.TurnGameObject;

public class FlashGameObject extends GameObject
    implements TurnGameObject
{
    /** The identifier for a MessageEvent containing a user message. */
    public static const USER_MESSAGE :String = "Umsg";

    /** The identifier for a MessageEvent containing game-system chat. */
    public static const GAME_CHAT :String = "Uchat";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>turnHolder</code> field. */
    public static const TURN_HOLDER :String = "turnHolder";

    /** The field name of the <code>flashGameService</code> field. */
    public static const FLASH_GAME_SERVICE :String = "flashGameService";
    // AUTO-GENERATED: FIELDS END

    /** The current turn holder. */
    public var turnHolder :Name;

    /** The service interface for requesting special things from the server. */
    public var flashGameService :FlashGameMarshaller;

    /**
     * Access the underlying user properties.
     */
    public function getUserProps () :Object
    {
        return _props;
    }

    // from TurnGameObject
    public function getTurnHolderFieldName () :String
    {
        return TURN_HOLDER;
    }

    // from TurnGameObject
    public function getTurnHolder () :Name
    {
        return turnHolder;
    }

    // from TurnGameObject
    public function getPlayers () :TypedArray /* of Name */
    {
        return players;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>turnHolder</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public function setTurnHolder (value :Name) :void
    {
        var ovalue :Name = this.turnHolder;
        requestAttributeChange(
            TURN_HOLDER, value, ovalue);
        this.turnHolder = value;
    }
    // AUTO-GENERATED: METHODS END

    /**
     * Called by a PropertySetEvent to enact a property change.
     * @return the old value
     */
    public function applyPropertySet (
        propName :String, value :Object, index :int) :Object
    {
        var oldValue :Object = _props[propName];
        if (index >= 0) {
            // set an array element
            var arr :Array = (oldValue as Array);
            oldValue = arr[index];
            arr[index] = value;

        } else if (value != null) {
            // normal property set
            _props[propName] = value;

        } else {
            // remove a property
            delete _props[propName];
        }
        return oldValue;
    }

    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        throw new Error("Un-needed");
        /*

        out.writeObject(turnHolder);
        out.writeObject(flashGameService);

        var keys :Array = [];
        var key :String;
        for (key in _props) {
            keys.push(key);
        }
        out.writeInt(keys.length);
        for (key in keys) {
            out.writeUTF(key);
            out.writeObject(FlashObjectMarshaller.encode(_props[key]));
        }
        */
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        // first read any regular bits
        turnHolder = (ins.readObject() as Name);
        flashGameService = (ins.readObject() as FlashGameMarshaller);

        // then user properties
        var count :int = ins.readInt();
        while (count-- > 0) {
            var key :String = ins.readUTF();
            var data :Object = ins.readObject();
            if (data is Array) {
                // read an array value
                var ta :Array = (data as Array);
                var array :Array = [];

                for (var ii :int = 0; ii < ta.length; ii++) {
                    array[ii] = FlashObjectMarshaller.decode(
                        ta[ii] as ByteArray);
                }
                _props[key] = array;

            } else {
                _props[key] = FlashObjectMarshaller.decode(
                    data as ByteArray);
            }
        }
    }

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}
}
