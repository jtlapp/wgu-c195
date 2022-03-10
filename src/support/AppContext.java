package support;

import database.Database;
import database.DatabaseException;
import javafx.stage.Stage;
import model.User;

/**
 * Interface providing access to application-wide resources and values. The
 * application has a single implementation of this interface, which it passes
 * to each controller via setContext() to configure the controller.
 *<p>
 * The AppContext is the gate guard for the logged-in user. A logged-in user
 * can only be assigned via a successful call to login(), and it can only be
 * retrieved via getUser(), preventing the rest of the application from
 * accidentally subverting what would otherwise have to be convention.
 *<p>
 * The app takes this approach rather than using static class members because
 * this approach supports mock injection for testing. We did not have a
 * requirement to create tests for this app, but I thought it important to
 * demonstrate maintainable structure for future reference.
 * @see controller.Controller#setContext
 * @author Joseph T. Lapp
 */

public interface AppContext {

    /**
     * Returns the title of the application
     * @return Title of the application
     */
    String getAppTitle();

    /**
     * Returns the object representing the database
     * @return Object representing the database
     */
    Database getDatabase();

    /**
     * Returns the logged-in user
     * @return The logged-in user; null prior to login
     */
    User getUser();

    /**
     * Attempts to log the user into the application. Does so by looking for
     * the provided username and password in the database.
     * @param username Username credential
     * @param password Password credential
     * @return true when the user was successfully logged in
     * @throws DatabaseException on database error
     */
    boolean login(String username, String password)
            throws DatabaseException;

    /**
     * Logs the current user out of the application, if a user is logged in,
     * and opens a new login window that asks for credentials.
     * @param stage Stage on which to open the window
     */
    void logoutAndLogin(Stage stage);

    /**
     * Returns the hour at which business starts, in EST.
     * @return The hour at which business starts, in EST
     */
    int getStartOfBusinessHourEST();

    /**
     * Returns the hour at which business end, in EST.
     * @return The hour at which business ends, in EST
     */
    int getEndOfBusinessHourEST();
}
