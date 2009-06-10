//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.And;

import com.threerings.msoy.web.gwt.Promotion;

/**
 * Manages information on our active promotions.
 */
@Singleton
public class PromotionRepository extends DepotRepository
{
    @Inject
    public PromotionRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads all active promotions.
     */
    public List<PromotionRecord> loadActivePromotions ()
    {
        // to make this query cacheable, round our timestamp to the current hour or so
        Timestamp now = new Timestamp(System.currentTimeMillis() & ~0x1FFFFFL);
        return findAll(PromotionRecord.class,
                       new Where(new And(PromotionRecord.ENDS.greaterEq(now),
                                         PromotionRecord.STARTS.lessEq(now))));
    }

    /**
     * Loads all promotions.
     */
    public List<PromotionRecord> loadPromotions ()
    {
        return findAll(PromotionRecord.class);
    }

    /**
     * Loads a specific promotion.
     */
    public PromotionRecord loadPromotion (String id)
    {
        return load(PromotionRecord.class, PromotionRecord.getKey(id));
    }

    /**
     * Adds a promotion to the repository.
     */
    public void addPromotion (Promotion promo)
    {
        insert(PromotionRecord.fromPromotion(promo));
    }

    /**
     * Updates a promotion in the repository.
     */
    public void updatePromotion (Promotion promo)
    {
        update(PromotionRecord.fromPromotion(promo));
    }

    /**
     * Removes a promotion from the repository.
     */
    public void deletePromotion (String promoId)
    {
        delete(PromotionRecord.getKey(promoId));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (final Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(PromotionRecord.class);
    }
}
