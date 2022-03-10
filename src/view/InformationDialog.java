package view;

import javafx.scene.control.Alert;

/**
 * InformationDialog is a class representing an information dialog,
 * which informs the user of an issue and waits for acknowledgement.
 * @author Joseph T. Lapp
 */

public class InformationDialog {

    /**
     * View control for the information dialog
     */
    private Alert alert;

    /**
     * Constructs a InformationDialog
     * @param header The text to place in the dialog header
     * @param message The message to present to the user
     */
    public InformationDialog(String header, String message) {
        alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("...");
        alert.setHeaderText(header);
        alert.setContentText(message);
    }

    /**
     * Shows the dialog and waits for the user to acknowledge it.
     */
    public void showAndWait() {
        alert.showAndWait();
    }
}
