import controller.LoginController;
import database.Database;
import database.UserDao;
import javafx.application.Application;
import javafx.stage.Stage;

import model.User;
import support.AppContext;
import database.DatabaseException;
import support.LoginLog;
import view.InformationDialog;

import java.io.IOException;
import java.util.Arrays;

/**
 * Main is the class representing the entire application. It maintains the
 * database and the logged-in user and serves as the application context
 * for all components of the application. It opens the login form.
 *<p>
 * The application displays all times in 24-hour military format.
 *<p>
 * This application assumes that the database tables for customers and
 * appointments are small enough to be entirely loaded within memory,
 * sparing the application the effort of paging through these tables.
 * @author Joseph T. Lapp
 */

public class Main extends Application implements AppContext
{
    /**
     * Title of the application
     */
    private static final String APP_TITLE = "Software II Scheduling App";

    /**
     * Hour at which business opens for appointments, in EST
     */
    private final static int START_OF_BUSINESS_EST = 8; // 8:00am EST

    /**
     * Hour at which business closes for appointments, in EST
     */
    private final static int END_OF_BUSINESS_EST = 22; // 10:00pm EST

    /**
     * Object encapsulating the database for the application
     */
    private Database db;

    /**
     * The logged-in user; null prior to login
     */
    private User user;

    /**
     * The main function, which launches the application
     * @param args Command line arguments; unused
     */
    public static void main(String[] args)
    {
        launch(args);
    }

    /**
     * Constructs the application.
     */
    public Main() {
        // nothing to do
    }

    /**
     * Starts the application and opens the login window.
     * @param stage The application's opening stage
     */
    public void start(Stage stage)
    {
        try {
            db = new Database();
        }
        catch (DatabaseException e) {
            var dialog = new InformationDialog("FAILURE",
                    e.getMessage() +"\n\n"+
                    "Press OK to exit application.");
            if (e.getCause() != null) {
                System.err.print(e.getCause().getMessage() +":\n"+
                        Arrays.toString(e.getCause().getStackTrace()).replace(", ", "\n"));
            }
            dialog.showAndWait();
            System.exit(1);
        }
        logoutAndLogin(stage);
    }

    /**
     * Returns the title of the application
     * @return Title of the application
     */
    public String getAppTitle() { return APP_TITLE; }

    /**
     * Returns the object representing the database
     * @return Object representing the database
     */
    public Database getDatabase() { return db; }

    /**
     * Returns the logged-in user
     * @return The logged-in user; null prior to login
     */
    public User getUser() { return user; }

    /**
     * Attempts to log the user into the application. Does so by looking for
     * the provided username and password in the database.
     * @param username Username credential
     * @param password Password credential
     * @return true when the user was successfully logged in
     * @throws DatabaseException on database error
     */
    public boolean login(String username, String password)
            throws DatabaseException
    {
        boolean success = false;
        try (var openConn = db.openConnection()){
            user = UserDao.validateUser(openConn.get(), username, password);
            success = (user != null);
        }
        try {
            LoginLog.append(username, success);
        }
        catch (IOException e) {
            var dialog = new InformationDialog("ERROR",
                    "Failed writing to login log:\n"+
                    e.getMessage());
            dialog.showAndWait();
        }
        return success;
    }

    /**
     * Logs the current user out of the application, if a user is logged in,
     * and opens a new login window that asks for credentials.
     * @param stage Stage on which to open the window
     */
    public void logoutAndLogin(Stage stage) {
        var loginController = new LoginController(stage, APP_TITLE);
        loginController.setContext(this);
    }

    /**
     * Returns the hour at which business starts, in EST.
     * @return The hour at which business starts, in EST
     */
    public int getStartOfBusinessHourEST() { return START_OF_BUSINESS_EST; }

    /**
     * Returns the hour at which business end, in EST.
     * @return The hour at which business ends, in EST
     */
    public int getEndOfBusinessHourEST() { return END_OF_BUSINESS_EST; }
}
