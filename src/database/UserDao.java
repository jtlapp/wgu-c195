package database;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DAO providing access to users in the database. The only operation this
 * application performs with users is application login, so the class provides
 * only a single static method for this purpose.
 * @author Joseph T. Lapp
 */

public class UserDao {

    /**
     * Constructor made private so that it is never constructed.
     */
    private UserDao() { } // not for construction

    /**
     * Validate user credentials against those found in the database, returning
     * the user if found and null otherwise.
     * @param conn An open database connection, which it leaves open
     * @param username Username credential
     * @param password Password credential
     * @return A user if the credentials are valid; null otherwise
     * @throws DatabaseException on database error
     */
    public static User validateUser(Connection conn, String username, String password)
            throws DatabaseException
    {
        // A real app would use password hashes.
        try (PreparedStatement select = conn.prepareStatement(
                "SELECT User_ID FROM users WHERE User_name=? AND Password=?")) {
            select.setString(1, username);
            select.setString(2, password);
            var results = select.executeQuery();
            if (results.next()) {
                return new User(results.getLong("User_ID"), username);
            }
            return null;
        }
        catch (SQLException e) {
            throw new DatabaseException("Error validating user", e);
        }
    }
}
