//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

/** Catalog Records for Documents. */
@TableGenerator(name="catalogId", pkColumnValue="DOCUMENT_CATALOG")
public class DocumentCatalogRecord extends CatalogRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<DocumentCatalogRecord> _R = DocumentCatalogRecord.class;
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp LISTED_ITEM_ID = colexp(_R, "listedItemId");
    public static final ColumnExp ORIGINAL_ITEM_ID = colexp(_R, "originalItemId");
    public static final ColumnExp LISTED_DATE = colexp(_R, "listedDate");
    public static final ColumnExp CURRENCY = colexp(_R, "currency");
    public static final ColumnExp COST = colexp(_R, "cost");
    public static final ColumnExp PRICING = colexp(_R, "pricing");
    public static final ColumnExp SALES_TARGET = colexp(_R, "salesTarget");
    public static final ColumnExp PURCHASES = colexp(_R, "purchases");
    public static final ColumnExp RETURNS = colexp(_R, "returns");
    public static final ColumnExp FAVORITE_COUNT = colexp(_R, "favoriteCount");
    public static final ColumnExp BASIS_ID = colexp(_R, "basisId");
    public static final ColumnExp DERIVATION_COUNT = colexp(_R, "derivationCount");
    // AUTO-GENERATED: FIELDS END

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DocumentCatalogRecord}
     * with the supplied key values.
     */
    public static Key<DocumentCatalogRecord> getKey (int catalogId)
    {
        return new Key<DocumentCatalogRecord>(
                DocumentCatalogRecord.class,
                new ColumnExp[] { CATALOG_ID },
                new Comparable[] { catalogId });
    }
    // AUTO-GENERATED: METHODS END
}
