package model;

import database.ReadWriteDao;

/**
 * Interface for model objects having accessors for an ID property.
 * @see ReadWriteDao
 * @author Joseph T. Lapp
 */

public interface WithID {

    /**
     * Getter for the object's unique ID.
     * @return The object's unique ID
     */
    long getId();

    /**
     * Setting for an object's unique ID
     * @param id Unique ID to assign to the object
     */
    void setId(long id);
}
