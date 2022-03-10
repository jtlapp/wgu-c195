package controller;

import database.ContactCache;
import database.AppointmentDao;
import database.CustomerDao;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Appointment;
import model.Contact;
import model.Customer;
import support.AppContext;
import database.DatabaseException;
import support.InvalidArgException;
import support.Refreshable;
import view.InformationDialog;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller for managing the windows that add and modify appointments. It constructs
 * the forms, waits for user input, delegates to the Appointment class for validating
 * the user-provided fields, and delegates to AppointmentDao to perform the database
 * operation to add or update the appointment. It also checks to see whether the
 * appointment overlaps with any other customer appointments and refuses to add or
 * update the appointment until the overlap is corrected. Appointments are limited to
 * the business hours of 08:00 to 22:00 EST by restricting combo box values.
 * @author Joseph T. Lapp
 */

public class AppointmentController extends WindowController {

    private static final String FXML_FILE = "appointment.fxml";

    /**
     * The user is only allowed to set meeting start and end times to
     * multiples of this number of minutes in the hour
     */
    private final static int APPT_MINUTES_INCREMENT = 5;

    /**
     * Appointment being modified, or null to indicate adding an appointment
     */
    private final Appointment appointment;

    /**
     * Reference to the object needing to be refreshed afterward
     */
    private final Refreshable refreshable;

    /**
     * Contacts the user can select from
     */
    private final ContactCache contactCache;

    /**
     * The user's zone ID
     */
    private final ZoneId userZoneId;

    /**
     * The offset in hours of the user's local time relative to EST.
     * Used for constructing combo boxes for inputting meeting times.
     */
    private final int localOffsetHours;

    /**
     * List of contact names ending with their IDs in parentheses
     */
    private final List<String> uniqueContactNames;

    /**
     * List of customer names ending with their IDs in parentheses
     */
    private final List<String> uniqueCustomerNames;

    /**
     * Contact name of an appointment being modified prior to modification,
     * with the contact's ID in parentheses
     */
    private String initialUniqueContactName;

    /**
     * Customer name of an appointment being modified prior to modification,
     * with the customer's ID in parentheses
     */
    private String initialUniqueCustomerName;

    /**
     * The hour corresponding to the first option in the meeting end hour
     * combo box, expressed in EST
     */
    private int firstEndingHourOptionInEST;

    /**
     * Title of this form window
     */
    @FXML
    private Label formWindowTitle;

    @FXML
    private TextField idField;

    @FXML
    private TextField appointmentTitleField;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField typeField;

    @FXML
    private TextField locationField;

    /**
     * Control allowing the user to select a contact. Shows each contact with
     * a parenthesized ID, allowing for multiple contacts with the same name,
     * because the ID uniquely identifies the contact.
     */
    @FXML
    private ComboBox<String> contactComboBox;

    /**
     * Control allowing the user to select a customer. Shows each customer with
     * a parenthesized ID, allowing for multiple customers with the same name,
     * because the ID uniquely identifies the customer.
     */
    @FXML
    private ComboBox<String> customerComboBox;

    @FXML
    private DatePicker dateField;

    @FXML
    private ComboBox<String> startHourComboBox;

    @FXML
    private ComboBox<String> startMinsComboBox;

    /**
     * Control displaying the user's time zone next to the meeting start time
     */
    @FXML
    private Label startZoneLabel;

    @FXML
    private ComboBox<String> endHourComboBox;

    @FXML
    private ComboBox<String> endMinsComboBox;

    /**
     * Control displaying the user's time zone next to the meeting end time
     */
    @FXML
    private Label endZoneLabel;

    @FXML
    private Label errorMessageField;

    /**
     * Constructs a window for displaying a form to add or modify an appointment.
     * @param appContext Global application context
     * @param appointment Appointment to modify; if null, then adding an appointment
     * @param refreshable Entity to refresh after adding or modifying an appointment
     * @throws DatabaseException on database error
     */
    public AppointmentController(AppContext appContext, Appointment appointment,
                                 Refreshable refreshable)
            throws DatabaseException
    {
        super(new Stage(), (appointment == null ? "Add Appointment" : "Modify Appointment"),
                FXML_FILE, null, Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        setContext(appContext);
        this.refreshable = refreshable;

        try (var openConn = db.openConnection()) {
            contactCache = new ContactCache(openConn.get());

            if (appointment == null) {
                this.appointment = null;
            } else {
                // Load appointment anew from database before modifying.
                var appointmentDao = new AppointmentDao(openConn.get(), user, contactCache);
                this.appointment = appointmentDao.getById(appointment.getId());
                if (this.appointment == null) {
                    throw new DatabaseException("Appointment ID "+ appointment.getId() +
                            " no longer exists.");
                }
            }

            uniqueContactNames = contactCache.getAll().stream()
                    .map(Contact::getUniqueName)
                    .sorted()
                    .collect(Collectors.toList());

            var customerDao = new CustomerDao(openConn.get(), user);
            uniqueCustomerNames = customerDao.getAll().stream()
                    .map(Customer::getUniqueName)
                    .sorted()
                    .collect(Collectors.toList());

            if (appointment != null) {
                initialUniqueContactName =
                        contactCache.getById(appointment.getContactId()).getUniqueName();
                initialUniqueCustomerName =
                        customerDao.getById(appointment.getCustomerId()).getUniqueName();
            }
        }

        var estZoneId = ZoneId.of("America/New_York");
        userZoneId = ZoneId.systemDefault();
        var now = LocalDateTime.now(); // arbitrary time
        var hereOffsetSeconds = userZoneId.getRules().getOffset(now).getTotalSeconds();
        var estOffsetSeconds = estZoneId.getRules().getOffset(now).getTotalSeconds();
        localOffsetHours = (hereOffsetSeconds - estOffsetSeconds) / 3600;

        load();
    }

    /**
     * Initializes the appointment form, including populating the combo boxes and
     * setting initial field values when modifying an appointment.
     *<p>
     * LAMBDA - These two lambdas yield slightly less code their equivalent 'for' loop
     * implementations would for iterating over uniqueContactNames and uniqueCustomerNames.
     */
    @FXML
    public void initialize() {
        formWindowTitle.setText(windowTitle);

        uniqueContactNames.forEach(name -> contactComboBox.getItems().add(name));
        uniqueCustomerNames.forEach(name -> customerComboBox.getItems().add(name));

        populateHourOptions(startHourComboBox, appContext.getStartOfBusinessHourEST(),
                appContext.getEndOfBusinessHourEST() - 1);
        populateMinuteOptions(startMinsComboBox, 0, 59);

        if (appointment != null) {
            idField.setText(Long.toString(appointment.getId()));
            appointmentTitleField.setText(appointment.getTitle());
            descriptionField.setText(appointment.getDescription());
            locationField.setText(appointment.getLocation());
            contactComboBox.setValue(initialUniqueContactName);
            customerComboBox.setValue(initialUniqueCustomerName);
            typeField.setText(appointment.getType());
            dateField.setValue(appointment.getStartTime().toLocalDate());
            var format = DateTimeFormatter.ofPattern("HH");
            startHourComboBox.setValue(appointment.getStartTime().format(format));
            endHourComboBox.setValue(appointment.getEndTime().format(format));
            format = DateTimeFormatter.ofPattern(":mm");
            startMinsComboBox.setValue(appointment.getStartTime().format(format));
            endMinsComboBox.setValue(appointment.getEndTime().format(format));
            populateEndTimeOptions(true); // before setting end time
        }

        var zoneAbbrev = userZoneId.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.ENGLISH);
        startZoneLabel.setText(zoneAbbrev);
        endZoneLabel.setText(zoneAbbrev);

        startHourComboBox.getSelectionModel().selectedItemProperty().addListener(this::startTimeChanged);
        startMinsComboBox.getSelectionModel().selectedItemProperty().addListener(this::startTimeChanged);
        endHourComboBox.getSelectionModel().selectedItemProperty().addListener(this::endHourChanged);
    }

    /**
     * Handles a user request to cancel adding or modifying an appointment.
     * @param event User event
     */
    @FXML
    private void requestCancel(ActionEvent event) {
        closeWindow();
    }

    /**
     * Handles a user request to write the new or modified appointment to the database,
     * reporting errors instead when the provided data is invalid, including indicating
     * whether the proposed meeting time overlaps with other meeting times for the
     * same customer and thus can't be allowed.
     * @param event User event
     */
    @FXML
    private void requestSave(ActionEvent event) {
        var errors = "";
        try {
            LocalDateTime startDateTime = toDateTime(
                    dateField, startHourComboBox, startMinsComboBox);
            LocalDateTime endDateTime = toDateTime(
                    dateField, endHourComboBox, endMinsComboBox);
            long customerId = -1;
            long contactId = -1;

            var customerText = customerComboBox.getSelectionModel().getSelectedItem();
            if (customerText != null && !customerText.isEmpty()) {
                customerId = extractIdFromUniqueName(customerText);
            }

            var contactText = contactComboBox.getSelectionModel().getSelectedItem();
            if (contactText != null && !contactText.isEmpty()) {
                contactId = extractIdFromUniqueName(contactText);
            }

            try (var openConn = db.openConnection()) {
                var appointmentDao = new AppointmentDao(openConn.get(), user, contactCache);

                if (hasOverlap(customerId, startDateTime, endDateTime, appointmentDao)) {
                    return;
                }
                if (appointment == null) {
                    var newAppointment = new Appointment(
                            appointmentTitleField.getText(),
                            descriptionField.getText(),
                            locationField.getText(),
                            typeField.getText(),
                            startDateTime,
                            endDateTime,
                            user.name,
                            customerId,
                            user.id,
                            contactId,
                            contactCache
                    );
                    appointmentDao.save(newAppointment);
                } else {
                    appointment.set(
                            appointmentTitleField.getText(),
                            descriptionField.getText(),
                            locationField.getText(),
                            typeField.getText(),
                            startDateTime,
                            endDateTime,
                            user.name,
                            customerId,
                            user.id,
                            contactId
                    );
                    appointmentDao.save(appointment);
                }
            }
            refreshable.refresh();
            closeWindow();
        }
        catch (InvalidArgException e) {
            errors = e.getMessage() + errors;
            errors = "ERRORS:\n- " + errors.replaceAll(";", "\n-");
            errorMessageField.setText(errors);
            errorMessageField.setVisible(true);
        }
        catch (DatabaseException e) {
            reportDbError(e);
        }
    }

    /**
     * Indicates whether the proposed meeting times overlap with other meetings
     * that the customer already has, providing a message reporting overlap.
     * @param customerId ID of customer to check for meeting overlap
     * @param startDateTime Proposed meeting start date/time
     * @param endDateTime Proposed meeting end date/time
     * @param appointmentDao DAO providing access to appointments in the database
     * @return true when the proposed meeting overlaps with one or more existing
     *          meetings for the customer
     * @throws DatabaseException on database error
     */
    private boolean hasOverlap(long customerId, LocalDateTime startDateTime,
                               LocalDateTime endDateTime, AppointmentDao appointmentDao)
            throws DatabaseException
    {
        if (customerId >= 0 && startDateTime != null && endDateTime != null) {
            var overlapCount = appointmentDao.getMeetingOverlapCount(
                    customerId, startDateTime, endDateTime,
                    (appointment == null ? 0 : appointment.getId()));
            if (overlapCount > 0) {
                InformationDialog dialog = new InformationDialog("FAILURE",
                        "This appointment overlaps with " + overlapCount +
                                " other appointment(s) the customer already has.");
                dialog.showAndWait();
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the user having changed the meeting start time. Once the user
     * has selected meeting start hour and minutes, the method will populate
     * the meeting end time combo boxes with the possible meeting end times.
     * @param value Observable control value
     * @param oldValue Old meeting start time
     * @param newValue New meeting start time
     */
    private void startTimeChanged(ObservableValue<? extends String> value,
                                  String oldValue, String newValue) {
        if (!startHourComboBox.getSelectionModel().isEmpty() &&
                !startMinsComboBox.getSelectionModel().isEmpty()) {
            populateEndTimeOptions(true);
        }
    }

    /**
     * Handles the user having changed the meeting end hour. Used for
     * populating the possible meeting end time minutes.
     * @param value Observable control value
     * @param oldValue Old meeting end hour
     * @param newValue New meeting end hour
     */
    private void endHourChanged(ObservableValue<? extends String> value,
                                String oldValue, String newValue) {
        populateEndTimeOptions(false);
    }

    /**
     * Extracts the parenthesized Id from the provided contact or customer name.
     * This method allows the combo boxes to display contacts or customers having
     * identical names while still distinguishing them by ID. (I only took this
     * approach because I timed out trying to figure out how to use combo boxes
     * with structured objects. That approach seemed messier than this.)
     * @param uniqueName Name containing a parenthesized ID
     * @return ID found in the name
     */
    private long extractIdFromUniqueName(String uniqueName) {
        var pattern = Pattern.compile("\\((\\d+)\\)$");
        var matcher = pattern.matcher(uniqueName);
        if (!matcher.find())
            throw new RuntimeException("Customer ID not found in combo option");
        return Long.parseLong(matcher.group(1));

    }

    /**
     * Populate the options available in the meeting end times combo boxes. If
     * the meeting start time limits the end hour to the last available hour,
     * that hour is automatically assigned as the end hour. If the meeting starts
     * at the last possible minute of the last hour, the end time automatically
     * fills with the time of close of business.
     * @param updateHours Whether to also update the end hours combo box
     */
    private void populateEndTimeOptions(boolean updateHours) {
        var startingHourIndex = startHourComboBox.getSelectionModel().getSelectedIndex();
        var startingHourInEST = appContext.getStartOfBusinessHourEST() + startingHourIndex;
        var startingMinsText = startMinsComboBox.getSelectionModel().getSelectedItem();
        var startingMinutes = Integer.parseInt(startingMinsText.substring(1));

        if (updateHours) {
            if (!endHourComboBox.getSelectionModel().isEmpty()) {
                endHourComboBox.setValue(""); // clear the ending time
                endMinsComboBox.setValue("");
            }

            firstEndingHourOptionInEST = startingHourInEST;
            if (startingMinutes + APPT_MINUTES_INCREMENT >= 60) {
                ++firstEndingHourOptionInEST;
            }
            populateHourOptions(endHourComboBox, firstEndingHourOptionInEST,
                    appContext.getEndOfBusinessHourEST());
        }

        var firstMinutesOption = 0; // first option in combo box
        var upperLimitMinutes = 59;
        if (!endHourComboBox.getSelectionModel().isEmpty()) {
            var endingHourIndex = endHourComboBox.getSelectionModel().getSelectedIndex();
            var endingHourInEST = firstEndingHourOptionInEST + endingHourIndex;
            if (startingHourInEST == endingHourInEST) {
                firstMinutesOption = startingMinutes + APPT_MINUTES_INCREMENT;
            }
            if (endingHourInEST == appContext.getEndOfBusinessHourEST()) {
                upperLimitMinutes = 0;
            }
        }
        populateMinuteOptions(endMinsComboBox, firstMinutesOption, upperLimitMinutes);
    }

    /**
     * Populates an hour combo box (start or end).
     * @param hourComboBox Hour combo box to populate
     * @param firstHourOptionInEST Hour of first available option, in EST
     * @param upperLimitHoursInEST Hour of last available option, in EST
     */
    private void populateHourOptions(ComboBox<String> hourComboBox,
                                     int firstHourOptionInEST, int upperLimitHoursInEST)
    {
        hourComboBox.getItems().clear();
        int hourInEST = firstHourOptionInEST;
        while(hourInEST <= upperLimitHoursInEST) { // for loop was hard to read
            hourComboBox.getItems().add(toLocalHourString(hourInEST));
            ++hourInEST;
        }
    }

    /**
     * Populates a minutes combo box (start or end).
     * @param minsComboBox Minutes combo box to populate
     * @param firstMinuteOption Minutes of first available option
     * @param upperLimitMinutes Minutes of last available option
     */
    private void populateMinuteOptions(ComboBox<String> minsComboBox,
                                       int firstMinuteOption, int upperLimitMinutes)
    {
        minsComboBox.getItems().clear();
        int minutes = firstMinuteOption;
        while (minutes <= upperLimitMinutes) { // for loop was hard to read
            minsComboBox.getItems().add(toMinuteString(minutes));
            minutes += APPT_MINUTES_INCREMENT;
        }
        if (minsComboBox.getItems().size() == 1) {
            minsComboBox.setValue(minsComboBox.getItems().get(0));
        }
    }

    /**
     * Converts date, hour, and minutes view controls into a single date/time.
     * @param datePicker Control providing the date
     * @param hourComboBox Control providing the hour
     * @param minsComboBox Control providing the minutes
     * @return The indicated date/time in local time
     */
    private LocalDateTime toDateTime(DatePicker datePicker,
                                     ComboBox<String> hourComboBox,
                                     ComboBox<String> minsComboBox) {
        var date = datePicker.getValue();
        var hourText = hourComboBox.getValue();
        var minsText = minsComboBox.getValue();

        if (date == null || hourText == null || hourText.isEmpty() ||
                minsText == null || minsText.isEmpty()) {
            return null;
        }

        return date.atTime(Integer.parseInt(hourText),
                Integer.parseInt(minsText.substring(1)));
    }

    /**
     * Convert an EST hour time integer into a local hour string.
     * @param hourInEST Hour to convert to a string, in EST
     * @return Hour converted to a local time string
     */
    private String toLocalHourString(int hourInEST) {
        var localHour = hourInEST + localOffsetHours;
        if (localHour >= 24)
            localHour -= 24;
        return (localHour < 10 ? "0" : "") + localHour;
    }

    /**
     * Converts a minutes integer into a string representation.
     * @param minutes Minutes to convert to a string
     * @return Minutes represented as a string
     */
    private String toMinuteString(int minutes) {
        return ":" + (minutes < 10 ? "0" : "") + minutes;
    }
}
