package support;

import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import view.InformationDialog;

import java.io.InputStream;
import java.util.List;

/**
 * Class providing static methods that help with building scenes.
 * @author Joseph T. Lapp
 */

public class SceneHelper {

    /**
     * Initializes a TableView from a list of column specifications,
     * assigning a cell value factory and an initial width to each column.
     * @param tableView Table to initialize
     * @param columnSpecs Column specifications from which to initialize the table
     * @param emptyTableMessage Message to report when the table is empty
     * @param <I> Class representing the items to which table rows correspond
     */
    public static <I> void initializeTable(TableView<I> tableView,
                                           List<ColumnSpec<I>> columnSpecs,
                                           String emptyTableMessage) {
        for (var spec : columnSpecs) {
            spec.column.setPrefWidth(spec.initialWidth);
            spec.column.setCellValueFactory(new PropertyValueFactory<>(spec.propertyName));
            tableView.getColumns().add(spec.column);
        }
        tableView.setPlaceholder(new Label(emptyTableMessage));
    }

    /**
     * Loads an image from a file and returns it.
     * @param imageFilePath Path to image file
     * @param exitOnFail Whether to exit application on failure to load image
     * @return An image object suitable for inclusion in a scene
     */
    public static Image loadImage(String imageFilePath, boolean exitOnFail) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try {
            InputStream stream = classLoader.getResourceAsStream(imageFilePath);
            return new Image(stream);
        }
        catch (Exception e) {
            showLoadFailure(imageFilePath, exitOnFail);
            return null;
        }
    }

    /**
     * Reports to the user a failure to load a file resource and optionally
     * exits the application (with error code) afterward.
     * @param fileName Name of the file that failed to load
     * @param exitOnFail Whether to exit the application after showing the error
     */
    public static void showLoadFailure(String fileName, boolean exitOnFail) {
        String message = "Failed to load file '"+ fileName +"'.";
        if (exitOnFail) {
            message += ".\n\nPress OK to close the application.";
        }
        InformationDialog dialog = new InformationDialog("FAILURE", message);
        dialog.showAndWait();
        if (exitOnFail) {
            System.exit(1);
        }
    }
}
