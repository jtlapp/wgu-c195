package controller;

import database.DatabaseException;
import database.DatabaseOperation;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import model.WithID;
import support.*;
import view.ConfirmationDialog;
import view.InformationDialog;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Base class for a controller that manages a particular tab of the main window.
 * Each tab is assumed to contain data that can be refreshed to make consistent
 * with the changes made to the database by other users. It provides hooks for
 * a standard add, modify, delete, and refresh buttons. It also provides a few
 * tools that are generally useful to tabs showing data.
 * @author Joseph T. Lapp
 * @param <I> Class representing the items to which table rows correspond
 */

public abstract class TablePaneController<I extends WithID> extends Controller
        implements Refreshable
{
    /**
     * How many times the notice message should blink
     */
    private final static int NOTICE_BLINK_COUNT = 5;

    /**
     * How long the notice message should stay visible after blinking
     */
    private final static int NOTICE_TIMER_SECONDS = 60;

    /**
     * Title of the scene shown in the tab
     */
    protected final String sceneTitle;

    /**
     * English name for the database items shown in the table
     */
    protected final String itemName;

    /**
     * Timer for the notice message
     */
    private Timer noticeTimer;

    /**
     * Control that shows the scene's title
     */
    @FXML
    protected Label sceneTitleLabel;

    /**
     * Control that holds any special controls that the tab might need,
     * beyond those provided in the FXML. The tab adds the controls itself.
     */
    @FXML
    protected HBox controlsBox;

    /**
     * Control for the table containing the database items
     */
    @FXML
    protected TableView<I> tableView;

    /**
     * Control that shows a temporary, initially-blinking notice
     */
    @FXML
    protected Label noticeLabel;

    /**
     * Constructs a TablePaneController for use in a tab of MainController.
     * @see MainController
     * @param sceneTitle Title of the scene shown in the tab
     * @param itemName English name for the database items shown in the table
     * @param fxmlResourceFile The FXML resource file for the tab's scene
     */
    public TablePaneController(String sceneTitle, String itemName, String fxmlResourceFile) {
        super(fxmlResourceFile, null);
        this.sceneTitle = sceneTitle;
        this.itemName = itemName;
    }

    /**
     * Initializes the scene.
     */
    @FXML
    public void initialize() {
        sceneTitleLabel.setText(sceneTitle);
    }

    /**
     * Sets the application context.
     * @param context Global application context
     */
    public void setContext(AppContext context) {
        super.setContext(context);
        refresh();
    }

    /**
     * Closes resources when the window is closed.
     */
    public boolean onWindowClosing() {
        var closing = super.onWindowClosing();
        if (closing) {
            if (noticeTimer != null) {
                noticeTimer.cancel();
            }
        }
        return closing;
    }

    /**
     * Handles the user pressing the scene's "Add" button.
     * @param event User event
     */
    @FXML
    protected abstract void requestAdd(ActionEvent event);

    /**
     * Handles the user pressing the scene's "Delete" button.
     * @param event User event
     */
    @FXML
    protected abstract void requestDelete(ActionEvent event);

    /**
     * Handles the user pressing the scene's "Modify" button.
     * @param event User event
     */
    @FXML
    protected abstract void requestModify(ActionEvent event);

    /**
     * Handles the user pressing the scene's "Refresh" button.
     * @param event User event
     */
    @FXML
    protected void requestRefresh(ActionEvent event) {
        refresh();
    }

    /**
     * Returns a list of the items shown in the table.
     * @return A list of the items shown in the table
     */
    protected abstract ObservableList<I> getItemList();

    /**
     * Returns the item associated with the row currently selected
     * in the table.
     * @return The item selected in the table
     */
    protected I getSelection() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    /**
     * Deletes the selected item indirectly via the provided reference to
     * a database operation. The method abstracts the logic of deleting
     * items so that it need not be repeated in each controller.
     *<p>
     * LAMBDA - Reduces the amount of code I have to write by comparison to the
     * alternative, which would have been to write a method for handling delete
     * confirmation, passing that method to showAndConfirm() instead.
     * @param operation Implementation for deleting the item from the database
     */
    protected void deleteSelection(DatabaseOperation<I> operation) {
        final I item = getSelection();
        if (item == null) {
            InformationDialog dialog = new InformationDialog("NOTICE",
                    "No "+ itemName +" selected");
            dialog.showAndWait();
        } else {
            ConfirmationDialog dialog1 = new ConfirmationDialog("DELETE?",
                    "Delete "+ itemName +" ID "+ item.getId() +"?");
            dialog1.showAndConfirm(() -> {
                try {
                    var successMessage = operation.perform(item);
                    InformationDialog dialog2 = new InformationDialog("DELETED",
                            successMessage);
                    dialog2.showAndWait();
                }
                catch (DatabaseException e) {
                    reportDbError(e);
                }
            });
        }
    }

    /**
     * Modifies the selected item indirectly via the provided reference to
     * a database operation. The method abstracts the logic of modifying
     * items so that it need not be repeated in each controller.
     * @param operation Implementation for modifying the item in the database
     */
    protected void modifySelection(DatabaseOperation<I> operation) {
        final I item = getSelection();
        if (item == null) {
            InformationDialog dialog = new InformationDialog("NOTICE",
                    "No "+ itemName +" selected");
            dialog.showAndWait();
        } else {
            try {
                operation.perform(item);
            }
            catch (DatabaseException e) {
                reportDbError(e);
            }
        }
    }

    /**
     * Displays a temporary, initially-blinking notice in the notice label.
     * @param message Message to display in the notice label
     */
    protected void showNotice(String message) {
        noticeLabel.setText(message);
        noticeLabel.setVisible(true);
        noticeTimer = new Timer();
        scheduleNoticeBlink(false, NOTICE_BLINK_COUNT, 500);
    }

    /**
     * Schedules a change in visibility of the notice label.
     * @param visible Whether to show or hide the notice next
     * @param count Number of times to blink the notice
     * @param millis Number of milliseconds before making the change
     */
    private void scheduleNoticeBlink(boolean visible, int count, long millis) {
        var noticeTimerTask = new TimerTask() {
            @Override
            public void run() {
                noticeLabel.setVisible(visible);
                if (count > 0) {
                    if (visible)
                        scheduleNoticeBlink(false, count, 500);
                    else
                        scheduleNoticeBlink(true, count - 1, 150);
                } else if (visible) {
                    scheduleNoticeBlink(false, count, NOTICE_TIMER_SECONDS * 1000);
                }
            }
        };
        noticeTimer.schedule(noticeTimerTask, millis);
    }
}
