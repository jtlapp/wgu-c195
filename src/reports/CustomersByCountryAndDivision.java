package reports;

import database.CustomerDao;
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
 * Report generating a table of customer counts for each combination of country and
 * "first level division", sorted by country. It shows countries in the first column,
 * replacing duplicate countries with quotation marks to emphasize country groupings.
 * @author Joseph T. Lapp
 */

public class CustomersByCountryAndDivision extends Report<CustomerDao.CountryDivisionCount> {

    private final TableColumn<CustomerDao.CountryDivisionCount, String> countryColumn =
            new TableColumn<>("Country");
    private final TableColumn<CustomerDao.CountryDivisionCount, String> divisionColumn =
            new TableColumn<>("Division");
    private final TableColumn<CustomerDao.CountryDivisionCount, Integer> countColumn =
            new TableColumn<>("No. of Customers");

    /**
     * Specifications for constructing the report table from the table columns
     */
    private final List<ColumnSpec<CustomerDao.CountryDivisionCount>> columnSpecs = Arrays.asList(
            new ColumnSpec<>(countryColumn, "country", 100),
            new ColumnSpec<>(divisionColumn, "division", 150),
            new ColumnSpec<>(countColumn, "count", 120)
    );

    /**
     * Returns the name of the report
     * @return Name of the report
     */
    public String getName() { return "Customers by Country and Division"; }

    /**
     * Constructs the report as a populated instance of TableView.
     * @param appContext Global application context
     * @return The constructed, populated table serving as the report
     * @throws DatabaseException on database error
     */
    public TableView<?> createTableView(AppContext appContext)
            throws DatabaseException
    {
        var tableView = new TableView<CustomerDao.CountryDivisionCount>();
        SceneHelper.initializeTable(tableView, columnSpecs, "There are no customers");

        try (var openConn = appContext.getDatabase().openConnection()) {
            var customerDao = new CustomerDao(openConn.get(), appContext.getUser());
            var rows = customerDao.getCountryDivisionCounts();
            abbreviateDuplicates(rows, CustomerDao.CountryDivisionCount::getCountry,
                    CustomerDao.CountryDivisionCount::setCountry);
            abbreviateDuplicates(rows, CustomerDao.CountryDivisionCount::getDivision,
                    CustomerDao.CountryDivisionCount::setDivision);
            tableView.setItems(FXCollections.observableArrayList(rows));
        }
        return tableView;
    }
}
