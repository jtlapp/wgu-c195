package controller;

import database.Database;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import model.User;
import support.AppContext;
import database.DatabaseException;
import support.SceneHelper;
import view.InformationDialog;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * The base class for all controllers. It makes the application context, the database
 * the user, the scene, and the internationalization strings conveniently available
 * to all controllers. It segments the process of establishing a controller into
 * construction, loading the scene and its initialization, and establishing that the
 * application context data is available for use. It also manages multiple nested
 * controllers and provides a convenience method for displaying database errors.
 * @author Joseph T. Lapp
 */

public abstract class Controller {

    private static final String VIEW_FOLDER = "view/";
    private static final String I18N_FOLDER = "i18n/";

    /**
     * FXML resource filename stored away until it can be loaded
     */
    private final String fxmlResourceFile;

    /**
     * Localization keys for use by controller
     */
    protected final ResourceBundle i18n;

    /**
     * The app context in which the controller runs
     */
    protected AppContext appContext;

    /**
     * Convenience reference to the object representing the database
     */
    protected Database db;

    /**
     * Convenience reference to the logged-in user
     */
    protected User user;

    /**
     * The controller's top-level scene
     */
    protected Scene scene;

    /**
     * Controllers nested within the present controller, maintained for
     * the purpose of closing out their resources when the parent controller
     * closes.
     */
    private List<Controller> controllers;

    /**
     * Constructs a generic controller.
     * @param fxmlResourceFile Filename of the FXML resource file
     * @param i18nPrefix Prefix for the internationalization resource file
     */
    public Controller(String fxmlResourceFile, String i18nPrefix) {
        this.fxmlResourceFile = fxmlResourceFile;
        i18n = (i18nPrefix == null ? null : ResourceBundle.getBundle(I18N_FOLDER + i18nPrefix));
    }

    /**
     * Returns the scene's root node for the purpose of allowing the scenes of
     * nested controllers to be embedded within those of a parent controller.
     * @return The scene's root node
     */
    public Node getSceneRoot() {
        return scene.getRoot();
    }

    /**
     * Loads the scene, and in window controllers, opens the window. Only
     * called in the nested-most controller constructor, after the object has
     * been entirely configured, to ensure a valid state for initialization.
     * Induces a call to initialize().
     */
    public void load() {
        // Create scene from controller whose lifetime is tied to the scene
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        URL fxmlResource = classLoader.getResource(VIEW_FOLDER + fxmlResourceFile);
        FXMLLoader loader = (i18n == null ? new FXMLLoader(fxmlResource)
                : new FXMLLoader(fxmlResource, i18n));
        loader.setController(this);
        Parent parent;
        try {
            parent = loader.load(); // calls Controller::initialize()
        }
        catch(Exception e) {
            SceneHelper.showLoadFailure(fxmlResourceFile, true);
            return;
        }
        scene = new Scene(parent);
    }

    /**
     * Assigns an application context to the controller, giving the controller
     * access to application-wide data. The controller can assume that all
     * application data is valid and usable at the time this method is called,
     * which is why the context is not passed in via the constructor.
     *<p>
     * LAMBDA - This lambda yields slightly less code than its equivalent 'for'
     * loop implementation would for iterating over controllers. The lambda is
     * only one line, while the for loop would have been two lines.
     * @param context Global application context
     */
    public void setContext(AppContext context) {
        appContext = context;
        db = context.getDatabase();
        user = context.getUser();
        if (controllers != null) {
            controllers.forEach(controller -> controller.setContext(context));
        }
    }

    /**
     * Adds a nested controller whose lifetime is tied to that of the present controller.
     * @param controller Nested controller to add
     */
    protected void addController(Controller controller) {
        if (controllers == null) {
            controllers = new LinkedList<>();
        }
        controllers.add(controller);
    }

    /**
     * Receives notice that the window is about to close. It informs the nested
     * controllers so that they may close resources.
     * @return true allows the window to close; false aborts closing the window
     */
    protected boolean onWindowClosing() {
        if (controllers != null) {
            for (var controller : controllers) {
                if (!controller.onWindowClosing()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Utility for generically reporting unexpected database errors.
     * @param e The unexpected database error
     */
    protected void reportDbError(DatabaseException e) {
        var dialog = new InformationDialog("DB ERROR", e.getMessage());
        if (e.getCause() != null) {
            System.err.print(e.getCause().getMessage() +":\n"+
                    Arrays.toString(e.getCause().getStackTrace()).replace(", ", "\n"));
        }
        dialog.showAndWait();
    }
}
