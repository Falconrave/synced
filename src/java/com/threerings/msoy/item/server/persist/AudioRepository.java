//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link AudioRecord} items.
 */
@Singleton
public class AudioRepository extends ItemRepository<AudioRecord>
{
    @Entity(name="AudioMogMarkRecord")
    public static class AudioMogMarkRecord extends MogMarkRecord
    {
    }

    @Entity(name="AudioTagRecord")
    public static class AudioTagRecord extends TagRecord
    {
    }

    @Entity(name="AudioTagHistoryRecord")
    public static class AudioTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public AudioRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<AudioRecord> getItemClass ()
    {
        return AudioRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(AudioCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(AudioCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(AudioRatingRecord.class);
    }

    @Override
    protected MogMarkRecord createMogMarkRecord ()
    {
        return new AudioMogMarkRecord();
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new AudioTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new AudioTagHistoryRecord();
    }
}
