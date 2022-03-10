package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Class representing the database and managing connections to the database. It
 * also retains caches of countries and "first level divisions" pulled from the
 * database, because the requirements spec said we can assume these are only
 * updated once per year, making it reasonable to cache them per usage session.
 * @author Joseph T. Lapp
 */

public final class Database {

    private static final String DB_URL = "jdbc:mysql://wgudb.ucertify.com/WJ08q0P" +
            "?connectionTimeZone=SERVER"; // auto-convert to/from server-side UTC
    private static final String DB_USER = "U08q0P";
    private static final String DB_PASSWORD = "53689364520";

    /**
     * Managed open database connection, which wraps the JDBC database connection.
     * The application receives instances of this class upon requesting a connection
     * from the Database class. It's main advantage over passing around JDBC
     * connections is that it can be used in try-with-resources blocks without a
     * clause for catching SQLException on failure to close the connection, needing
     * no catch clause at all in contexts that already handle DatabaseException.
     */
    public class OpenConnection implements AutoCloseable {

        /**
         * Constructs a managed open database connection.
         * @throws DatabaseException on failure to open a connection
         */
        OpenConnection() throws DatabaseException {
            if (conn != null) {
                throw new RuntimeException("Attempted to open multiple connections");
            }
            try {
                conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
            catch (SQLException e) {
                throw new DatabaseException("Failed to open DB connection", e);
            }
        }

        /**
         * Returns the underlying JDBC database connection.
         * @return The underlying JDBC database connection
         */
        public Connection get() {
            if (conn == null) {
                throw new RuntimeException("Attempted to get a closed DB connection");
            }
            return conn;
        }

        /**
         * Closes the open database connection, including its underlying JDBC connection.
         * @throws DatabaseException on failure to close the connection
         */
        public void close() throws DatabaseException {
            try {
                conn.close();
                conn = null;
            }
            catch (SQLException e) {
                throw new DatabaseException("Failed to close DB connection");
            }
        }
    }

    /**
     * Cache of countries loaded from the database at user login
     */
    public CountryCache countryCache;

    /**
     * Cache of "first level divisions" loaded from the database at user login
     */
    public DivisionCache divisionCache;

    /**
     * The active (or most recent) JDBC database connection, which is managed within
     * the wrapper class OpenConnection. Never access this variable outside of
     * OpenConnection, even within the Database class (where it's private).
     */
    private Connection conn;

    /**
     * Constructs an object representing the database and tests connecting with
     * the database. It also caches countries and "first level divisions" from
     * the database, because this data only changes once per year, according to
     * the project requirements.
     * @throws DatabaseException on failure to connect to the database
     */
    public Database() throws DatabaseException {
        try (var openConn = openConnection()) {
            countryCache = new CountryCache(openConn.get());
            divisionCache = new DivisionCache(openConn.get(), countryCache);
        }
    }

    /**
     * Returns an open, managed database connection. Allows only one connection to
     * be open at a time. Although this is not a requirement of either databases or
     * applications, it did help with finding unintentional nesting of attempts to
     * open connections, which were inefficient for being able to share a connection.
     * @return An managed open database connection
     * @throws DatabaseException on failure to open the connection
     */
    public OpenConnection openConnection() throws DatabaseException {
        return new OpenConnection();
    }
}
