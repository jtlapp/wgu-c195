package view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * ConfirmationDialog is a class representing a confirmation dialog,
 * which asks the user to confirm or cancel an operation.
 * @author Joseph T. Lapp
 */

public class ConfirmationDialog {

    /**
     * View control for the confirmation dialog
     */
    private Alert alert;

    /**
     * Constructs a ConfirmationDialog
     * @param header The text to place in the dialog header
     * @param question The question being asked of the user
     */
    public ConfirmationDialog(String header, String question) {
        alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("...");
        alert.setHeaderText(header);
        alert.setContentText(question);
    }

    /**
     * Shows the dialog and waits for the user to either confirm
     * or cancel the indicated operation.
     *<p>
     * LAMBDA - Alert::showAndWait() returns an instance of Optional, which contains
     * functional methods designed to take lambdas. It is possible to write this
     * succinctly as procedural code, but the lambda solution makes it easy to
     * write declarative code instead. I personally think it's a wash whether this
     * code is better declarative or procedural; I had to pick one, regardless.
     * However, were one to try to implement the declarative code without lambdas,
     * one would have to write separate methods for passing to filter() and ifPresent(),
     * and THAT would be a lot more code, and unclear as well.
     * @param onConfirm Function to call if the user confirms
     */
    public void showAndConfirm(Runnable onConfirm) {
        alert.showAndWait()
            .filter(res -> res == ButtonType.OK)
            .ifPresent(res -> onConfirm.run());
    }
}
