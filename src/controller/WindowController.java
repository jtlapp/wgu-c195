package controller;

import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Base class for all controllers that represent windows (having a stage),
 * configuring the details of the window, including its modality.
 * @author Joseph T. Lapp
 */

public abstract class WindowController extends Controller {

    /**
     * The JavaFX stage for the present window
     */
    protected Stage stage;

    /**
     * The title of the present window
     */
    protected String windowTitle;

    /**
     * Constructs a window controller but does not open the FXML or the window.
     * @param stage The stage for the window
     * @param windowTitle The window's title
     * @param fxmlResourceFile The FXML resource file for the window's scene
     * @param i18nPrefix Prefix for the internationalization resource file
     * @param modality Controls whether to block access to prior windows
     */
    public WindowController(Stage stage, String windowTitle, String fxmlResourceFile,
                            String i18nPrefix, Modality modality) {
        super(fxmlResourceFile, i18nPrefix);
        this.stage = stage;
        this.windowTitle = windowTitle;
        stage.setTitle(windowTitle);
        stage.setResizable(true);
        stage.setMaximized(false);

        // The main stage crashes if I try to assign it Modality.NONE.
        if (modality != Modality.NONE) {
            stage.initModality(modality);
        }
    }

    /**
     * Closes the window by closing the stage. This method is not be responsible
     * for closing external and system resources because the user may close the
     * window by means that the OS user interface provides, and subsequently
     * calling this method would attempt to close the window again. The method
     * is final to prevent subclasses from attempting this. Use onWindowClose()
     * instead to close such resources. (I never actually tested doubly-closing
     * windows, but even if it works now it may not in a future JavaFX.)
     * @see Controller#onWindowClosing
     */
    public final void closeWindow() {
        if (onWindowClosing()) {
            stage.close();
        }
    }

    /**
     * Loads the FXML resource and opens the window with the scene.
     *<p>
     * LAMBDA - This lambda replaces what would have been another method and passing a
     * reference to that method instead of passing this lambda. The lambda prevents me
     * from having to write that method. In fact, this particular lambda is only half a
     * line of code -- dramatically shorter than the alternative. In addition to requiring
     * less code, the lambda code is clearer, because the lambda presents the event
     * behavior directly alongside the object to which it responds (stage).
     */
    public void load() {
        super.load();
        stage.setTitle(windowTitle);
        stage.setScene(scene); // assign the scene to a new or provided stage
        stage.show();

        // Only called when the user closes the window via system UI controls, not when
        // the app closes the window. Hence, the app must call onWindowClosing() prior
        // to closing a window.
        stage.setOnCloseRequest(event -> {
            if (!onWindowClosing()) {
                event.consume();
            };
        });
    }
}
