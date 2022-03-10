package controller;

import database.DatabaseException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.ZoneId;

/**
 * Manages the login form, displaying errors on failure to login, and
 * launching MainController on successful login to show the main window.
 * This is the only window in the application that has been
 * internationalized, in accordance with the project requirements.
 * @author Joseph T. Lapp
 */

public class LoginController extends WindowController {

    private static final String FXML_FILE = "login.fxml";

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    @FXML
    private Label locationLabel;

    @FXML
    private Label errorLabel;

    /**
     * Constructs a login form window and opens it.
     * @param stage The window's stage
     * @param appTitle The title of the application
     */
    public LoginController(Stage stage, String appTitle) {
        super(stage, appTitle, FXML_FILE, "login", Modality.NONE);
        stage.setResizable(false);
        System.out.println("Created new login controller");
        load();
    }

    /**
     * Initializes the login form scene.
     *<p>
     * LAMBDA - These two lambdas provide the same benefit, which is to replace
     * what would have been two separate methods and passing references to these methods
     * instead of passing these lambdas. The lambdas prevent me from having to write those
     * methods. In addition to requiring less code, the lambda code is clearer, because
     * the lambdas present the event behavior directly alongside the objects to which they
     * respond (usernameField and passwordField).
     */
    @FXML
    public void initialize() {
        locationLabel.setText(i18n.getString("label.location") +": "+
                ZoneId.systemDefault());

        usernameField.textProperty().addListener(
                (observable, oldValue, newValue) -> errorLabel.setVisible(false));
        passwordField.textProperty().addListener(
                (observable, oldValue, newValue) -> errorLabel.setVisible(false));
    }

    /**
     * Handles a user request to login with the provided credentials. Displays
     * errors on error, otherwise opens the main window after logging in.
     * @see MainController
     * @param event User event
     */
    @FXML
    private void requestLogin(ActionEvent event) {
        var username = usernameField.getText();
        var password = passwordField.getText();
        var error = "";

        if (username == null || username.isBlank() ||
                password == null || password.isBlank()) {
            error = i18n.getString("error.empty_field");
        }

        if (error.isEmpty()) {
            try {
                if (appContext.login(username.trim(), password)) {
                    closeWindow(); // close the login window
                    var mainController = new MainController(stage, appContext.getAppTitle());
                    mainController.setContext(appContext);
                } else {
                    error = i18n.getString("error.invalid_login");
                }
            }
            catch (DatabaseException e) {
                reportDbError(e);
            }
        }
        if (!error.isEmpty()) {
            errorLabel.setText(error);
            errorLabel.setVisible(true);
        }
    }
}
