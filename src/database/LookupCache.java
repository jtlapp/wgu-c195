package database;

import java.util.*;

/**
 * Base class for caches of database items, used for lookups in the application.
 * Requires each item to have a unique ID of type long.
 * @author Joseph T. Lapp
 * @param <I> Class of items being cached
 */

public abstract class LookupCache<I>
{
    /**
     * Hash map mapping IDs to the items being cached
     */
    final protected HashMap<Long, I> idMap = new HashMap<>();

    /**
     * Returns all items from the cache.
     * @return A list of all items in the cache
     */
    public Collection<I> getAll() {
        return idMap.values();
    }

    /**
     * Returns the item having the indicated ID.
     * @param id ID of the item to return
     * @return Item having the indicated ID
     */
    public I getById(long id) {
        return idMap.get(id);
    }
}
