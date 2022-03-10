package database;

import java.sql.SQLException;

/**
 * Exception representing an application-sanitized database error,
 * including a user-readable message for explanation.
 * @author Joseph T. Lapp
 */

public class DatabaseException extends Exception {

    /**
     * Reports an application-detected error NOT involving an underlying
     * SQLException.
     * @param message Description of the error for the user
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Reports an application-detected error involving an underlying
     * SQLException.
     * @param message Description of the error for the user
     * @param e Exception that prompted the present exception
     */
    public DatabaseException(String message, SQLException e) {
        super(message, e);
    }
}
