package support;

/**
 * Interface for controllers that provide views of the database that may
 * go stale due to activity outside the application.
 * @author Joseph T. Lapp
 */

public interface Refreshable {

    /**
     * Asks the receiver to refresh its data from the database.
     */
    void refresh();
}
