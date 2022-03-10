package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import javafx.stage.Modality;
import javafx.stage.Stage;
import support.AppContext;
import support.Refreshable;

/**
 * Manages the main window that opens after logging in. It provides tabs for the
 * different views and a user menu at the upper-right for logging out or exiting
 * the application. It delegates to other controllers to manage each tab.
 * @author Joseph T. Lapp
 */

public class MainController extends WindowController {

    private static final String FXML_FILE = "main.fxml";

    /**
     * Minimum width of the main window
     */
    private static final double MIN_WINDOW_WIDTH = 800.0;

    /**
     * Minimum height of the main window
     */
    private static final double MIN_WINDOW_HEIGHT = 400.0;

    /**
     * Control containing the title of the application
     */
    @FXML
    private Label titleLabel;

    /**
     * Control for the user menu at the upper-right of the window
     */
    @FXML
    private MenuButton userMenu;

    /**
     * Control holding the window's tabs
     */
    @FXML
    private TabPane tabPane;

    @FXML
    private Tab appointmentsTab;

    @FXML
    private Tab customersTab;

    @FXML
    private Tab reportsTab;

    /**
     * Constructs application's main window and opens the window.
     * @param stage The stage for the window
     * @param appTitle Title of the application
     */
    public MainController(Stage stage, String appTitle) {
        super(stage, appTitle, FXML_FILE, null, Modality.NONE);
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(MIN_WINDOW_HEIGHT);
        load();
    }

    /**
     * Initializes the scene controls for the main window and constructs
     * all of its tabs. Ensures that every time a user selects a new tab,
     * the data displayed in the new tab is fresh.
     *<p>
     * LAMBDA - This lambda replaces what would have been another method and passing a
     * reference to that method instead of passing this lambda. The lambda prevents me
     * from having to write that method. In addition to requiring less code, the lambda
     * code is clearer, because the lambda presents the event behavior directly alongside
     * the object to which it responds (tabPane).
     */
    @FXML
    public void initialize() {

        titleLabel.setText(windowTitle);

        loadTab(appointmentsTab, new AppointmentsController());
        loadTab(customersTab, new CustomersController());
        loadTab(reportsTab, new ReportsController());

        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTab, newTab) -> {
                    var refreshable = (Refreshable) newTab.getUserData();
                    refreshable.refresh();
                });
    }

    /**
     * Assigns an application context to the controller. At this point we have
     * the logged-in user and can update the user menu.
     * @param context Global application context
     */
    public void setContext(AppContext context) {
        super.setContext(context);
        userMenu.setText(user.name);
    }

    /**
     * Loads a tab with the provided controller.
     * @param tab Tab into which to load controller
     * @param controller Controller to load into tab
     */
    protected void loadTab(Tab tab, Controller controller) {
        addController(controller);
        tab.setContent(controller.getSceneRoot());
        tab.setUserData(controller);
    }

    /**
     * Handles a user request to log out of the application. This closes
     * the main window and opens a login window.
     * @param event User event
     */
    @FXML
    private void requestLogout(ActionEvent event) {
        closeWindow();
        appContext.logoutAndLogin(new Stage());
    }

    /**
     * Handles a user request to exit the application, exiting the application.
     * @param event User event
     */
    @FXML
    private void requestExit(ActionEvent event) {
        closeWindow();
        Platform.exit();
    }
}
