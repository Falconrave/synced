//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.ItemListInfo;

/**
 * Meta-data defining a list of items.
 */
@Entity
@TableGenerator(name="listId", pkColumnValue="ITEM_LIST_INFO")
public class ItemListInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ItemListInfoRecord> _R = ItemListInfoRecord.class;
    public static final ColumnExp<Integer> LIST_ID = colexp(_R, "listId");
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Byte> TYPE = colexp(_R, "type");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    // AUTO-GENERATED: FIELDS END

    /** Our depot schema version. */
    public static final int SCHEMA_VERSION = 4;

    /** Transforms a persistent record to a runtime record. */
    public static Function<ItemListInfoRecord, ItemListInfo> TO_INFO =
        new Function<ItemListInfoRecord, ItemListInfo>() {
        public ItemListInfo apply (ItemListInfoRecord record) {
            return record.toItemListInfo();
        }
    };

    @Id @GeneratedValue(generator="listId", strategy=GenerationType.TABLE, allocationSize=1)
    public int listId;

    @Index(name="ixMember")
    public int memberId;

    public byte type;

    public String name;

    /**
     * Default constructor.
     */
    public ItemListInfoRecord ()
    {
    }

    /**
     * Construct a record from an ItemListInfo.
     */
    public ItemListInfoRecord (ItemListInfo info, int memberId)
    {
        this.listId = info.listId;
        this.memberId = memberId;
        this.type = info.type;
        this.name = info.name;
    }

    public ItemListInfo toItemListInfo ()
    {
        ItemListInfo info = new ItemListInfo();
        info.listId = this.listId;
        info.type = this.type;
        info.name = this.name;

        return info;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ItemListInfoRecord}
     * with the supplied key values.
     */
    public static Key<ItemListInfoRecord> getKey (int listId)
    {
        return newKey(_R, listId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(LIST_ID); }
    // AUTO-GENERATED: METHODS END
}
