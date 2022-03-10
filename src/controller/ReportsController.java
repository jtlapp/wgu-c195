package controller;

import database.DatabaseException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import reports.*;
import support.Refreshable;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the reports tab on the main window, providing a drop-down menu for
 * selecting the desired report, as well as a refresh button for updating the
 * report to reflect the activity of other users. Rather than hard-coding the
 * report implementations into the controller, the controller maintains a
 * list of reports and delegates report generation to the objects in this list.
 * Adding reports is just a matter of adding objects to the list. All of the
 * reports are assumed to produce a single table.
 * @author Joseph T. Lapp
 */

public class ReportsController extends Controller
    implements Refreshable
{
    private static final String FXML_FILE = "report_tab.fxml";

    /**
     * Array of reports to make available from this tab
     */
    private final List<Report<?>> availableReports = new ArrayList<>(List.of(
            new AppointmentsByMonthAndType(),
            new AppointmentsByContact(),
            new CustomersByCountryAndDivision()
    ));

    /**
     * Report currently being displayed
     */
    private Report<?> currentReport;

    /**
     * The pane for the report tab in which the report table can be placed
     */
    @FXML
    private AnchorPane reportTabPane;

    /**
     * Control that selects the report to show
     */
    @FXML
    private ComboBox<String> reportTypeComboBox;

    /**
     * Control for the refresh button, which may need to be disabled.
     */
    @FXML
    protected Button refreshButton;

    /**
     * Constructs the controller for the reports tab and shows the scene.
     */
    public ReportsController() {
        super(FXML_FILE, null);
        load();
    }

    /**
     * Initializes the scene, populating the report type selection combo box
     * with the names of the available reports. Keeps the refresh button
     * disabled until a report is selected and shown.
     *<p>
     * LAMBDA - The first lambda yields slightly less code than its equivalent 'for'
     * loop implementation would for iterating over availableReports. The lambda
     * is only one line, while the for loop would have been two lines.
     *<p>
     * LAMBDA - The second lambda replaces what would have been another method and passing
     * a reference to that method instead of passing this lambda. The lambda prevents me
     * from having to write that method. In addition to requiring less code, the lambda
     * code is clearer, because the lambda presents the event behavior directly alongside
     * the object to which it responds (reportTypeComboBox).
     */
    @FXML
    public void initialize() {
        availableReports.forEach(report -> reportTypeComboBox.getItems().add(report.getName()));
        // Set value before attaching listener that responds to value.
        reportTypeComboBox.setValue("Please select a report");
        reportTypeComboBox.getSelectionModel().selectedItemProperty().addListener(
                (options, oldValue, newValue) -> {
                    displayReport(newValue);
                    refreshButton.setDisable(false);
                });
        refreshButton.setDisable(true);
    }

    /**
     * Refreshes the current report from the database.
     */
    public void refresh() {
        if (currentReport != null) {
            displayReport(currentReport.getName());
        }
    }

    /**
     * Handles a user request to refresh the report.
     * @param event User event
     */
    @FXML
    private void requestRefresh(ActionEvent event) {
        refresh();
    }

    /**
     * Displays the report having the indicated name.
     * @param reportName Name of the report to display
     */
    private void displayReport(String reportName) {
        currentReport = findReport(reportName);
        try {
            TableView<?> tableView = currentReport.createTableView(appContext);
            AnchorPane.setTopAnchor(tableView, 60.0);
            AnchorPane.setBottomAnchor(tableView, 25.0);
            AnchorPane.setLeftAnchor(tableView, 25.0);
            AnchorPane.setRightAnchor(tableView, 25.0);
            reportTabPane.getChildren().add(tableView);
        } catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Returns the report having the indicated report name.
     * @param reportName Name of the report to return
     * @return Report having the indicated name
     */
    private Report<?> findReport(String reportName) {
        // The streams version of this was complicated and unreadable.
        for (Report<?> report : availableReports) {
            if (reportName.equals(report.getName())) {
                return report;
            }
        }
        throw new RuntimeException("Report not found");
    }
}
