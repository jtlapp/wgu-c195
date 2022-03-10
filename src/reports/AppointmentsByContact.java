package reports;

import database.AppointmentDao;
import database.ContactCache;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import model.Appointment;
import support.AppContext;
import support.ColumnSpec;
import database.DatabaseException;
import support.SceneHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Report generating a table of appointments sorted by contact name, showing
 * contact names in the first column, and replacing duplicate contact names with
 * quotation marks to emphasize contact groupings.
 * @author Joseph T. Lapp
 */

public class AppointmentsByContact extends Report<Appointment> {

    private final TableColumn<Appointment, String> contactColumn = new TableColumn<>("Contact Name");
    private final TableColumn<Appointment, Integer> idColumn = new TableColumn<>("Appt. ID");
    private final TableColumn<Appointment, String> titleColumn = new TableColumn<>("Title");
    private final TableColumn<Appointment, String> descriptionColumn = new TableColumn<>("Description");
    private final TableColumn<Appointment, String> typeColumn = new TableColumn<>("Type");
    private final TableColumn<Appointment, String> startColumn = new TableColumn<>("Start Date/Time");
    private final TableColumn<Appointment, String> endColumn = new TableColumn<>("End Date/Time");
    private final TableColumn<Appointment, Integer> customerIdColumn = new TableColumn<>("Customer ID");

    /**
     * Specifications for constructing the report table from the table columns
     */
    private final List<ColumnSpec<Appointment>> columnSpecs = Arrays.asList(
            new ColumnSpec<>(contactColumn, "contactName", 100),
            new ColumnSpec<>(idColumn, "id", 60),
            new ColumnSpec<>(titleColumn, "title", 100),
            new ColumnSpec<>(descriptionColumn, "description", 100),
            new ColumnSpec<>(typeColumn, "type", 120),
            new ColumnSpec<>(startColumn, "startTimeString", 120),
            new ColumnSpec<>(endColumn, "endTimeString", 120),
            new ColumnSpec<>(customerIdColumn, "customerId", 80)
    );

    /**
     * Returns the name of the report
     * @return Name of the report
     */
    public String getName() { return "Appointments by Contact"; }

    /**
     * Constructs the report as a populated instance of TableView.
     * @param appContext Global application context
     * @return The constructed, populated table serving as the report
     * @throws DatabaseException on database error
     */
    public TableView<?> createTableView(AppContext appContext)
            throws DatabaseException
    {
        var tableView = new TableView<Appointment>();
        SceneHelper.initializeTable(tableView, columnSpecs, "There are no appointments");

        try (var openConn = appContext.getDatabase().openConnection()) {
            var contactCache = new ContactCache(openConn.get());
            var appointmentDao = new AppointmentDao(openConn.get(), appContext.getUser(), contactCache);
            var items = appointmentDao.getAllOrderedByContact();
            abbreviateDuplicates(items, Appointment::getContactName,
                    Appointment::setContactName);
            tableView.setItems(FXCollections.observableArrayList(items));
        }
        return tableView;
    }
}
