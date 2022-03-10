package controller;

import database.AppointmentDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.Appointment;
import model.User;
import support.ColumnSpec;
import database.DatabaseException;
import support.SceneHelper;
import view.InformationDialog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages the appointments tab on the main window. It provides a table of appointments
 * filtered according to a radio-button filter selection. It also provides arrow buttons
 * to allow users to navigate among monthly and weekly views of the appointments. Upon
 * logging in, when the appointments tab is first shown, it displays an alert for any
 * appointments that will be starting in the next 15 minutes. If no appointments start
 * in the next 15 minutes, it instead shows a briefly-flashing message at the bottom
 * saying so, clearing that message after a minute. It also implements the add, modify,
 * delete, and refresh buttons for appointments.
 *<p>
 * This controller does not implement the filtering logic for the radio buttons. Instead,
 * it delegates that logic to different filter objects providing different filters of
 * the appointments. The radio buttons merely select the filter object.
 * @author Joseph T. Lapp
 */

public class AppointmentsController extends TablePaneController<Appointment> {

    private static final String FXML_FILE = "table_tab.fxml";

    /**
     * A list of all of the appointments in the database, regardless of
     * which appointments are currently being shown.
     */
    private final ObservableList<Appointment> allAppointments
            = FXCollections.observableArrayList();

    /**
     * Whether appointments have ever been loaded into the table
     */
    private boolean firstRefresh = true;

    /**
     * Filter controlling the appointments visible in the table
     */
    private AppointmentFilter filter;

    /**
     * Radio button selecting a view of all appointments
     */
    private final RadioButton allButton = new RadioButton("All");

    /**
     * Radio button selecting a monthly view of appointments
     */
    private final RadioButton monthlyButton = new RadioButton("Monthly");

    /**
     * Radio button selecting a weekly view of appointments
     */
    private final RadioButton weeklyButton = new RadioButton("Weekly");

    /**
     * Radio button group indicating the selected filter
     */
    private final ToggleGroup filterGroup = new ToggleGroup();

    /**
     * Button for advancing the view to preceding appointments
     */
    private final Button leftButton = new Button();

    /**
     * Button for advancing the view to following appointments
     */
    private final Button rightButton = new Button();

    private final TableColumn<Appointment, Integer> idColumn = new TableColumn<>("ID");
    private final TableColumn<Appointment, String> titleColumn = new TableColumn<>("Title");
    private final TableColumn<Appointment, String> descriptionColumn = new TableColumn<>("Description");
    private final TableColumn<Appointment, String> typeColumn = new TableColumn<>("Type");
    private final TableColumn<Appointment, String> locationColumn = new TableColumn<>("Location");
    private final TableColumn<Appointment, String> contactColumn = new TableColumn<>("Contact");
    private final TableColumn<Appointment, String> startColumn = new TableColumn<>("Start Date/Time");
    private final TableColumn<Appointment, String> endColumn = new TableColumn<>("End Date/Time");
    private final TableColumn<Appointment, Integer> customerIdColumn = new TableColumn<>("Customer ID");

    /**
     * Specifications for constructing the table from the table columns
     */
    private final List<ColumnSpec<Appointment>> columnSpecs = Arrays.asList(
            new ColumnSpec<>(idColumn, "id", 30),
            new ColumnSpec<>(titleColumn, "title", 100),
            new ColumnSpec<>(descriptionColumn, "description", 100),
            new ColumnSpec<>(typeColumn, "type", 120),
            new ColumnSpec<>(locationColumn, "location", 100),
            new ColumnSpec<>(contactColumn, "contactName", 100),
            new ColumnSpec<>(startColumn, "startTimeString", 120),
            new ColumnSpec<>(endColumn, "endTimeString", 120),
            new ColumnSpec<>(customerIdColumn, "customerId", 80)
    );

    /**
     * Constructs the controller for the appointments tab and shows the scene.
     */
    public AppointmentsController() {
        super("Appointments", "appointment", FXML_FILE);
        load();
    }

    /**
     * Initializes the scene, including the appointments table, and assigns listeners
     * to the radio buttons and the left/right arrow buttons.
     *<p>
     * LAMBDA - All three of these lambdas provide the same benefit, which is to replace
     * what would have been three separate methods and passing references to these methods
     * instead of passing these lambdas. The lambdas prevent me from having to write those
     * methods. In addition to requiring less code, the lambda code is clearer, because
     * the lambdas present the event behavior directly alongside the objects to which they
     * respond (filterGroup, leftButton, and rightButton).
     */
    @FXML
    public void initialize() {
        super.initialize();

        allButton.setToggleGroup(filterGroup);
        monthlyButton.setToggleGroup(filterGroup);
        weeklyButton.setToggleGroup(filterGroup);
        Image leftArrow = SceneHelper.loadImage("view/left_arrow.png", true);
        Image rightArrow = SceneHelper.loadImage("view/right_arrow.png", true);
        leftButton.setGraphic(new ImageView(leftArrow));
        rightButton.setGraphic(new ImageView(rightArrow));
        controlsBox.getChildren().addAll(allButton, monthlyButton, weeklyButton,
                leftButton, rightButton);
        allButton.setSelected(true);
        SceneHelper.initializeTable(tableView, columnSpecs, "No appointments to show");

        filterGroup.selectedToggleProperty().addListener(
                (observableValue, toggle, t1) -> {
                    var radioButton = filterGroup.getSelectedToggle();
                    if (radioButton == null)
                        throw new RuntimeException("No appointment filter selected");
                    changeFilter(radioButton);
                    updateView();
            });

        leftButton.setOnAction(event -> {
            filter.previousView();
            updateView();
        });
        rightButton.setOnAction(event -> {
            filter.nextView();
            updateView();
        });
    }

    /**
     * Loads the appointments table from the database, applying the appropriate
     * filter. On the first load, checks to see if there are any appointments
     * pending within the next 15 minutes, taking advantage of the fact that we
     * have a database connection, so this check isn't slowed by having to get one.
     */
    public void refresh() {
        try (var openConn = db.openConnection()) {
            var appointmentDao = new AppointmentDao(openConn.get(), user);

            // Can't replace allAppointments because it's in use by a filter.
            allAppointments.clear();
            allAppointments.addAll(appointmentDao.getAll());

            if (firstRefresh) {
                changeFilter(allButton); // also sets to last view
            }
            updateView();

            if (firstRefresh) {
                displayPendingAppointments(appointmentDao);
                firstRefresh = false;
            }
        }
        catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Handles a user request to add an appointment.
     * @param event User event
     */
    @FXML
    protected void requestAdd(ActionEvent event) {
        try {
            new AppointmentController(appContext, null, this);
        }
        catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Handles a user request to delete an appointment.
     *<p>
     * LAMBDA - This lambda prevents me from having to write another method, which I
     * would have passed to deleteSelection() instead. Moreover, the lambda makes the
     * intended behavior clearer by showing it directly alongside the operation --
     * deleteSelection() -- that employs the method.
     * @param event User event
     */
    @FXML
    protected void requestDelete(ActionEvent event) {
        deleteSelection((appointment) -> {
            try (var openConn = db.openConnection()) {
                var appointmentDao = new AppointmentDao(openConn.get(), user);
                appointmentDao.delete(appointment);
                allAppointments.remove(appointment);
            }
            refresh();
            return "Appointment ID " + appointment.getId() + " (type '" + appointment.getType() +
                    "') was successfully deleted.\n\n" +
                    "If you are viewing by month or week and delete the " +
                    "only appointment shown, you may need to use the arrow " +
                    "buttons to locate existing appointments.";
        });
    }

    /**
     * Handles a user request to modify an appointment.
     *<p>
     * LAMBDA - This lambda prevents me from having to write another method, which I
     * would have passed to modifySelection() instead. Moreover, the lambda makes the
     * intended behavior clearer by showing it directly alongside the operation --
     * modifySelection() -- that employs the method.
     * @param event User event
     */
    @FXML
    protected void requestModify(ActionEvent event) {
        modifySelection((appointment) -> {
            new AppointmentController(appContext, appointment, this);
            return null; // no success message
        });
    }

    /**
     * Returns a list of all appointments cached from the database.
     * @return A list of all appointments cached from the database
     */
    protected ObservableList<Appointment> getItemList() { return allAppointments; }

    /**
     * Changes the appointment filter to the filter indicated by the provided
     * radio button. The filter will be applied within the next call to updateView().
     * @param selectedButton Radio button indicating chosen filter
     */
    private void changeFilter(Toggle selectedButton) {
        if (selectedButton == allButton) {
            filter = new AppointmentFilter.None(allAppointments);
        } else if (selectedButton == monthlyButton) {
            filter = new AppointmentFilter.Monthly(allAppointments);
        } else {
            filter = new AppointmentFilter.Weekly(allAppointments);
        }
    }

    /**
     * Checks for appointments pending within the next 15 minutes. If any are
     * pending, it shows an alert listing the pending appointments. If none
     * are pending, it displays a less-obtrusive temporary notice.
     * @param appointmentDao DAO providing access to appointments in the DB
     */
    private void displayPendingAppointments(AppointmentDao appointmentDao) {
        // We have a requirement that the evaluator be able to test this
        // feature by changing the local system time. This precludes us from
        // doing the time comparison server-side via SQL. I assume that means
        // I have to loop and compare all appointments.

        var pendingAppointments = new ArrayList<Appointment>();
        var now = LocalDateTime.now();
        var nowPlus15 = now.plusMinutes(15);
        try {
            for (var appointment : appointmentDao.getAll()) {
                var startTime = appointment.getStartTime();
                // isEqual() comparisons unnecessary given millisec granularity
                if (startTime.isAfter(now) && startTime.isBefore(nowPlus15)) {
                    pendingAppointments.add(appointment);
                }
            }
            if (pendingAppointments.isEmpty()) {
                showNotice("There are no appointments within 15 minutes.");
            } else {
                var notice = "The following appointments are within 15 minutes:\n\n";
                for (var appointment : pendingAppointments) {
                    var startTime = appointment.getStartTime();
                    var endTime = appointment.getEndTime();
                    notice += "  - (ID "+ appointment.getId() +") On "+
                        startTime.format(User.DATE_FORMAT) +
                        " from "+ startTime.format(User.TIME_FORMAT) +" until "+
                        endTime.format(User.TIME_FORMAT) +"\n";
                }
                var dialog = new InformationDialog("NOTICE", notice);
                dialog.showAndWait();
            }
        }
        catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Updates the table to reflect the current filter.
     */
    private void updateView() {
        var subtitle = filter.getSubtitle();
        if (subtitle == null) {
            sceneTitleLabel.setText(sceneTitle);
        } else {
            sceneTitleLabel.setText(sceneTitle +" - "+ subtitle);
        }
        leftButton.setDisable(!filter.hasPreviousView());
        rightButton.setDisable(!filter.hasNextView());

        tableView.setItems(filter.getFilteredList());
    }
}
