package database;

import model.User;
import model.WithID;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class of DAOs that provide read and write access to a class of items in
 * the database. Each item has an associated ID and getters/setters for the ID.
 * This base class generically implements a number of database operations that
 * are common to items having unique IDs, relying on the implementations of the
 * abstract methods createItem() and loadStatement() to tailor the queries.
 * @author Joseph T. Lapp
 * @param <I> The class of items to which the DAO provides access
 */

public abstract class ReadWriteDao<I extends WithID>
{
    /**
     * Reference to the logged-in user
     */
    protected final User user;

    /**
     * English term for the class of items that this DAO manages
     */
    protected final String itemName;

    /**
     * Name of the database table containing the class of items
     */
    protected final String tableName;

    /**
     * Name of the table's primary key column
     */
    protected final String idColumnName;

    /**
     * Prepared query for retrieving all items in the table
     */
    private final PreparedStatement queryAll;

    /**
     * Prepared query for retrieving an item from the table by ID
     */
    private final PreparedStatement queryById;

    /**
     * Prepared query for deleting an item from the table by ID
     */
    private final PreparedStatement deleteById;

    /**
     * Constructs a DAO providing read/write access to a table
     * containing a class of items.
     * @param conn An open database connection, which it leaves open
     * @param user The logged-in user
     * @param itemName English term for the class of items
     * @param tableName Name of the database table
     * @param idColumnName Name of the primary key column
     * @param orderByColumnName Name of column by which to order table,
     *                          or null to leave unordered
     * @throws DatabaseException on database error
     */
    public ReadWriteDao(Connection conn, User user, String itemName, String tableName,
                        String idColumnName, String orderByColumnName)
            throws DatabaseException
    {
        this.user = user;
        this.itemName = itemName;
        this.tableName = tableName;
        this.idColumnName = idColumnName;

        try {
            if (orderByColumnName == null) {
                queryAll = conn.prepareStatement("SELECT * FROM " + tableName);
            } else {
                queryAll = conn.prepareStatement("SELECT * FROM " + tableName +
                        " ORDER BY "+ orderByColumnName);
            }
            queryById = conn.prepareStatement("SELECT * FROM " + tableName +
                    " WHERE " + idColumnName + "=?");
            deleteById = conn.prepareStatement("DELETE FROM " + tableName +
                    " WHERE " + idColumnName + "=?");
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing " + itemName + " DAO", e);
        }
    }

    /**
     * Queries the database for the item of the given ID and returns that item.
     * @param id ID of the item
     * @return Item having that ID
     * @throws DatabaseException on error
     */
    public I getById(long id) throws DatabaseException
    {
        try {
            queryById.setLong(1, id);
            var results = queryById.executeQuery();
            if (!results.next()) {
                return null;
            }
            return createItem(results);
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting " + itemName, e);
        }
    }

    /**
     * Queries the database for all items in the table
     * @return All items in the table
     * @throws DatabaseException on error
     */
    public List<I> getAll() throws DatabaseException
    {
        var items = new ArrayList<I>();
        try {
            var results = queryAll.executeQuery();
            while (results.next()) {
                items.add(createItem(results));
            }
            return items;
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting all customers", e);
        }
    }

    /**
     * Write the provided item to the database. If the item has an ID of 0,
     * the item is added to the database and given the ID that the database
     * automatically assigns to it. If the ID is non-zero, updates the item.
     * @param item Item to write to the database
     * @throws DatabaseException on database error
     */
    public void save(I item) throws DatabaseException
    {
        if (item.getId() == 0) {
            try {
                var insert = getInsertStatement();
                loadStatement(insert, item);
                if (insert.executeUpdate() != 1) {
                    throw new DatabaseException("No " + itemName + " inserted");
                }
                var keys = insert.getGeneratedKeys();
                if (!keys.next()) {
                    throw new DatabaseException("No ID returned from insert");
                }
                item.setId(keys.getLong(1));
            } catch (SQLException e) {
                throw new DatabaseException("Error inserting " + itemName, e);
            }
        } else {
            try {
                var update = getUpdateStatement();
                var params = update.getParameterMetaData();
                loadStatement(update, item);
                update.setLong(params.getParameterCount(), item.getId());
                if (update.executeUpdate() != 1) {
                    throw new DatabaseException("No " + itemName + " updated");
                }
            } catch (SQLException e) {
                throw new DatabaseException("Error updating " + itemName, e);
            }
        }
    }

    /**
     * Deletes the indicated item from the database.
     * @param item Item to delete
     * @throws DatabaseException on database error
     */
    public void delete(I item) throws DatabaseException {
        try {
            deleteById.setLong(1, item.getId());
            // JDBC driver returns 0 on successful deletion, so ignore return value.
            deleteById.executeUpdate();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error deleting " + itemName, e);
        }
    }

    /**
     * Closes any open resources in this DAO, but does not close the
     * database connection, allowing the connection to be shared with
     * other DAOs for faster processing of a sequence of operations.
     * @throws DatabaseException on database error
     */
    public void close() throws DatabaseException
    {
        try {
            queryAll.close();
            queryById.close();
            deleteById.close();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error closing " + itemName + " DAO", e);
        }
    }

    /**
     * Returns an insert statement prepared for the class of items.
     * @return An insert statement for the class of items
     */
    protected abstract PreparedStatement getInsertStatement();

    /**
     * Returns an update statement prepared for the class of items. The last
     * parameter of this prepared statement must take an ID.
     * @return An insert statement for the class of items
     */
    protected abstract PreparedStatement getUpdateStatement();

    /**
     * Creates an item from the provided result set.
     * @param results Query results containing properties of the item
     * @return The newly-created item
     * @throws DatabaseException on database error
     */
    protected abstract I createItem(ResultSet results)
            throws DatabaseException;

    /**
     * Loads a prepared statement with the properties of an item.
     * @param ps Prepared statement to load
     * @param item Item from which to load the prepared statement
     * @throws DatabaseException on database error
     */
    protected abstract void loadStatement(PreparedStatement ps, I item)
            throws DatabaseException;
}
