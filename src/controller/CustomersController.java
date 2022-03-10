package controller;

import database.CustomerDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import model.Customer;
import support.ColumnSpec;
import database.DatabaseException;
import support.SceneHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Manages the customers tab on the main window, which displays a table of all
 * customers. It implements the add, modify, delete, and refresh buttons for
 * customers.
 * @author Joseph T. Lapp
 */

public class CustomersController extends TablePaneController<Customer> {

    private static final String FXML_FILE = "table_tab.fxml";

    /**
     * List of all customers in the database
     */
    private ObservableList<Customer> customerList;

    private final TableColumn<Customer, Integer> idColumn = new TableColumn<>("ID");
    private final TableColumn<Customer, String> nameColumn = new TableColumn<>("Customer Name");
    private final TableColumn<Customer, String> phoneColumn = new TableColumn<>("Phone");
    private final TableColumn<Customer, String> addressColumn = new TableColumn<>("Address");
    private final TableColumn<Customer, String> postalCodeColumn = new TableColumn<>("Postal Code");
    private final TableColumn<Customer, Integer> divisionColumn = new TableColumn<>("Division");
    private final TableColumn<Customer, Integer> countryColumn = new TableColumn<>("Country");

    /**
     * Specifications for constructing the table from the table columns
     */
    private final List<ColumnSpec<Customer>> columnSpecs = Arrays.asList(
            new ColumnSpec<>(idColumn, "id", 30),
            new ColumnSpec<>(nameColumn, "name", 120),
            new ColumnSpec<>(phoneColumn, "phone", 120),
            new ColumnSpec<>(addressColumn, "address", 150),
            new ColumnSpec<>(postalCodeColumn, "postalCode", 80),
            new ColumnSpec<>(divisionColumn, "divisionName", 140),
            new ColumnSpec<>(countryColumn, "countryName", 65)
    );

    /**
     * Constructs the controller for the customers tab and shows the scene.
     */
    public CustomersController() {
        super("Customers", "customer", FXML_FILE);
        load();
    }

    /**
     * Initializes the scene, including the customers table.
     */
    @FXML
    public void initialize() {
        super.initialize();
        SceneHelper.initializeTable(tableView, columnSpecs, "There are no customers");
    }

    /**
     * Loads the customers table from the database.
     */
    public void refresh() {
        try (var openConn = db.openConnection()) {
            var customerDao = new CustomerDao(openConn.get(), user);
            customerList = FXCollections.observableArrayList(customerDao.getAll());
            tableView.setItems(customerList);
        }
        catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Handles a user request to add a customer.
     * @param event User event
     */
    @FXML
    protected void requestAdd(ActionEvent event) {
        try {
            new CustomerController(appContext, null, this);
        }
        catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Handles a user request to delete a customer.
     *<p>
     * LAMBDA - This lambda prevents me from having to write another method, which I
     * would have passed to deleteSelection() instead. Moreover, the lambda makes the
     * intended behavior clearer by showing it directly alongside the operation --
     * deleteSelection() -- that employs the method.
     * @param event User event
     */
    @FXML
    protected void requestDelete(ActionEvent event) {
        deleteSelection((customer) -> {
            try (var openConn = db.openConnection()) {
                var customerDao = new CustomerDao(openConn.get(), user);
                customerDao.delete(customer);
                customerList.remove(customer);
            }
            return "Customer ID "+ customer.getId() +" was successfully deleted.";
        });
    }

    /**
     * Handles a user request to modify a customer.
     *<p>
     * LAMBDA - This lambda prevents me from having to write another method, which I
     * would have passed to modifySelection() instead. Moreover, the lambda makes the
     * intended behavior clearer by showing it directly alongside the operation --
     * modifySelection() -- that employs the method.
     * @param event User event
     */
    @FXML
    protected void requestModify(ActionEvent event) {
        modifySelection((customer) -> {
            new CustomerController(appContext, customer, this);
            return null; // no success message
        });
    }

    /**
     * Returns a list of all customers cached from the database.
     * @return A list of all customers cached from the database
     */
    protected ObservableList<Customer> getItemList() { return customerList; }
}
