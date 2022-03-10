package database;

/**
 * Abstract representation of an operation to add or modify a database item. It
 * serves to allow controllers to generically implement add and modify logic
 * that would otherwise be duplicated across controllers.
 * @author Joseph T. Lapp
 * @param <I> Class representing the database item to operate on
 */

public interface DatabaseOperation<I> {

    /**
     * Performs the represented operation on the provided item.
     * @param item Item on which to perform the operation
     * @return Message to display when the operation is successful;
     *          null in contexts not requiring this message
     * @throws DatabaseException on database error
     */
    String perform(I item) throws DatabaseException;
}
