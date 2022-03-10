package reports;

import database.AppointmentDao;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import support.AppContext;
import support.ColumnSpec;
import database.DatabaseException;
import support.SceneHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Report generating a table of appointment counts for each combination of month
 * and appointment type, sorted by month. It shows months in the first column,
 * replacing duplicate months with quotation marks to emphasize month groupings.
 * @author Joseph T. Lapp
 */

public class AppointmentsByMonthAndType extends Report<AppointmentDao.MonthTypeCount> {

    private final TableColumn<AppointmentDao.MonthTypeCount, String> monthColumn =
            new TableColumn<>("Month");
    private final TableColumn<AppointmentDao.MonthTypeCount, String> typeColumn =
            new TableColumn<>("Appointment Type");
    private final TableColumn<AppointmentDao.MonthTypeCount, Integer> countColumn =
            new TableColumn<>("No. of Appointments");

    /**
     * Specifications for constructing the report table from the table columns
     */
    private final List<ColumnSpec<AppointmentDao.MonthTypeCount>> columnSpecs = Arrays.asList(
            new ColumnSpec<>(monthColumn, "month", 100),
            new ColumnSpec<>(typeColumn, "type", 150),
            new ColumnSpec<>(countColumn, "count", 120)
    );

    /**
     * Returns the name of the report
     * @return Name of the report
     */
    public String getName() { return "Appointments by Month and Type"; }

    /**
     * Constructs the report as a populated instance of TableView.
     * @param appContext Global application context
     * @return The constructed, populated table serving as the report
     * @throws DatabaseException on database error
     */
    public TableView<?> createTableView(AppContext appContext)
            throws DatabaseException
    {
        var tableView = new TableView<AppointmentDao.MonthTypeCount>();
        SceneHelper.initializeTable(tableView, columnSpecs, "There are no appointments");

        try (var openConn = appContext.getDatabase().openConnection()) {
            var appointmentDao = new AppointmentDao(openConn.get(), appContext.getUser());
            var rows = appointmentDao.getMonthTypeCounts();
            abbreviateDuplicates(rows, AppointmentDao.MonthTypeCount::getMonth,
                    AppointmentDao.MonthTypeCount::setMonth);
            tableView.setItems(FXCollections.observableArrayList(rows));
        }
        return tableView;
    }
}
