package controller;

import database.CustomerDao;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Customer;
import support.AppContext;
import database.DatabaseException;
import support.InvalidArgException;
import support.Refreshable;

/**
 * Controller for managing the windows that add and modify customers. It constructs
 * the forms, waits for user input, delegates to the Customer class for validating
 * the user-provided fields, and delegates to CustomerDao to perform the database
 * operation to add or update the customer.
 * @author Joseph T. Lapp
 */

public class CustomerController extends WindowController {

    private static final String FXML_FILE = "customer.fxml";

    /**
     * Customer being modified, or null to indicate adding a customer
     */
    private final Customer customer;

    /**
     * Reference to the object needing to be refreshed afterward
     */
    private final Refreshable refreshable;

    @FXML
    private Label formWindowTitle;

    @FXML
    private TextField idField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField postalCodeField;

    @FXML
    private TextField phoneField;

    @FXML
    private ComboBox<String> countryComboBox;

    @FXML
    private ComboBox<String> divisionComboBox;

    @FXML
    private Label errorMessageField;

    /**
     * Constructs a window for displaying a form to add or modify a customer.
     * @param appContext Global application context
     * @param customer Customer to modify; if null, then adding an customer
     * @param refreshable Entity to refresh after adding or modifying an customer
     * @throws DatabaseException on database error
     */
    public CustomerController(AppContext appContext, Customer customer,
                              Refreshable refreshable)
            throws DatabaseException
    {
        super(new Stage(), (customer == null ? "Add Customer" : "Modify Customer"),
                FXML_FILE, null, Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        setContext(appContext);
        this.refreshable = refreshable;

        try (var openConn = db.openConnection()) {
            if (customer == null) {
                this.customer = null;
            } else {
                // Load customer anew from database before modifying.
                var customerDao = new CustomerDao(openConn.get(), user, db.divisionCache);
                this.customer = customerDao.getById(customer.getId());
                if (this.customer == null) {
                    throw new DatabaseException("Customer ID "+ customer.getId() +
                            " no longer exists.");
                }
            }
        }
        load();
    }

    /**
     * Initializes the customer form, including populating the combo boxes and
     * setting initial field values when modifying an customer.
     *<p>
     * LAMBDA - This lambda replaces what would have been another method and passing a
     * reference to that method instead of passing this lambda. The lambda prevents me
     * from having to write that method. In addition to requiring less code, the lambda
     * code is clearer, because the lambda presents the event behavior directly alongside
     * the object to which it responds (countryComboBox).
     */
    @FXML
    public void initialize() {
        formWindowTitle.setText(windowTitle);

        for (var country : db.countryCache.getAll()) {
            countryComboBox.getItems().add(country.name);
        }

        if (customer != null) {
            populateDivisionField(customer.getDivision().country.name);
            idField.setText(Long.toString(customer.getId()));
            nameField.setText(customer.getName());
            countryComboBox.setValue(customer.getCountryName());
            divisionComboBox.setValue(customer.getDivisionName());
            addressField.setText(customer.getAddress());
            postalCodeField.setText(customer.getPostalCode());
            phoneField.setText(customer.getPhone());
        }

        countryComboBox.getSelectionModel().selectedItemProperty().addListener(
                (options, oldValue, newValue) -> populateDivisionField(newValue));
    }

    /**
     * Handles a user request to cancel adding or modifying a customer.
     * @param event User event
     */
    @FXML
    private void requestCancel(ActionEvent event) {
        closeWindow();
    }

    /**
     * Handles a user request to write the new or modified customer to the database,
     * reporting errors instead when the provided data is invalid.
     * @param event User event
     */
    @FXML
    private void requestSave(ActionEvent event) {
        var countryName = countryComboBox.getValue();
        var divisionName = divisionComboBox.getValue();
        var divisionId = (divisionName == null || divisionName.isEmpty()
                ? 0 : db.divisionCache.getByName(countryName, divisionName).id);

        try {
            try (var openConn = db.openConnection()) {
                var customerDao = new CustomerDao(openConn.get(), user, db.divisionCache);

                if (customer == null) {
                    var newCustomer = new Customer(
                            nameField.getText(),
                            addressField.getText(),
                            postalCodeField.getText(),
                            phoneField.getText(),
                            user.name,
                            divisionId,
                            db.divisionCache
                    );
                    customerDao.save(newCustomer);
                } else {
                    customer.set(
                            nameField.getText(),
                            addressField.getText(),
                            postalCodeField.getText(),
                            phoneField.getText(),
                            user.name,
                            divisionId
                    );
                    customerDao.save(customer);
                }
            }
            refreshable.refresh();
            closeWindow();
        }
        catch (InvalidArgException e) {
            var errors = e.getMessage();
            errors = "ERRORS:\n- " + errors.replaceAll(";", "\n-");
            // If the country name isn't specified, neither is the division name,
            // ensuring that we get an exception and this code is executed.
            if (countryName == null || countryName.isBlank()) {
                errors += "\n- country not specified";
            }
            errorMessageField.setText(errors);
            errorMessageField.setVisible(true);
        }
        catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Populate the country "first level division" combo box for a given country.
     *<p>
     * LAMBDA - These two lambdas together implement a loop that selects only the
     * divisions of a particular country. It is possible to write this as a loop
     * in the same number of lines (three), but only by excluding curly braces for
     * both the 'for' loop and its nested 'if' condition -- not good programming
     * practice. So there is slightly less code, but in addition, the lambda
     * presentation is declarative, offering a non-procedural statement of what
     * is done, which some people may find clearer.
     * @param forCountry Country to which to restrict division options
     */
    private void populateDivisionField(String forCountry) {
        divisionComboBox.getItems().clear();
        db.divisionCache.getAll().stream()
                .filter(div -> div.country.name.equals(forCountry))
                .forEach(div -> divisionComboBox.getItems().add(div.name));
        divisionComboBox.setValue("");
    }
}
