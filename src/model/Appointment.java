package model;

import database.ContactCache;
import support.InvalidArgException;

import java.time.LocalDateTime;

/**
 * Class representing an appointment, containing information about the appointment
 * from the database or prepared for storage in the database. The class includes a
 * set() method for simultaneously setting all modifiable properties, allowing the
 * class to provide a single error message reporting all problems in the data. The
 * class also implements some derived values for rendering in tables.
 * @author Joseph T. Lapp
 */

public class Appointment implements WithID {

    /**
     * The appointment's unique ID; 0 until stored in the database.
     */
    private long id; // 0 => not yet stored in DB

    private String title;

    private String description;

    private String location;

    private String type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String createdBy;

    private LocalDateTime lastUpdate;

    private String lastUpdatedBy;

    private long customerId;

    private long userId;

    private long contactId;

    /**
     * Contact name as derived from contactId
     */
    private String contactName; // derived

    /**
     * Constructs an appointment from user-provided information in preparation
     * for storage in the database. The appointment's ID will be given its
     * database-assigned value once it is stored in the database.
     * @param title Appointment title
     * @param description Appointment description
     * @param location Appointment location
     * @param type Appointment type
     * @param startTime Appointment starting time
     * @param endTime Appointment ending time
     * @param createdBy Name of user that created the appointment
     * @param customerId ID of customer having the appointment
     * @param userId ID of user that created the appointment
     * @param contactId ID of contact having the appointment
     * @param contactCache Contacts as cached from the database
     * @throws InvalidArgException when one or more of the provided arguments is
     *      invalid, in which case the error message lists all the errors
     *      in all of the arguments, delimited by semicolons
     */
    public Appointment(String title, String description, String location,
                       String type, LocalDateTime startTime, LocalDateTime endTime,
                       String createdBy, long customerId, long userId, long contactId,
                       ContactCache contactCache)
            throws InvalidArgException
    {
        set(title, description, location, type, startTime, endTime, createdBy,
                customerId, userId, contactId);
        setDerivedValues(contactCache);
    }

    /**
     * Constructs an appointment from database-provided information.
     * @param id Appointment's unique ID
     * @param title Appointment title
     * @param description Appointment description
     * @param location Appointment location
     * @param type Appointment type
     * @param startTime Appointment starting time
     * @param endTime Appointment ending time
     * @param createdBy Name of user who created the appointment
     * @param lastUpdate Date/time at which appointment was last updated
     * @param lastUpdatedBy Name of user that last updated the appointment
     * @param customerId ID of customer having the appointment
     * @param userId ID of user that created the appointment
     * @param contactId ID of contact having the appointment
     * @param contactCache Contacts as cached from the database
     */
    public Appointment(long id, String title, String description, String location,
                       String type, LocalDateTime startTime, LocalDateTime endTime,
                       String createdBy, LocalDateTime lastUpdate,
                       String lastUpdatedBy, long customerId, long userId, long contactId,
                       ContactCache contactCache) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdBy = createdBy;
        this.lastUpdate = lastUpdate;
        this.lastUpdatedBy = lastUpdatedBy;
        this.customerId = customerId;
        this.userId = userId;
        this.contactId = contactId;
        setDerivedValues(contactCache);
    }

    public long getId() { return id; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public String getLocation() { return location; }

    public String getType() { return type; }

    public LocalDateTime getStartTime() { return startTime; }

    /**
     * Returns a localized string representation of the appointment start time.
     * @return Localized string representation of the appointment start time
     */
    public String getStartTimeString() { return formatLocalDateTime(startTime); }

    public LocalDateTime getEndTime() { return endTime; }

    /**
     * Returns a localized string representation of the appointment end time.
     * @return Localized string representation of the appointment end time
     */
    public String getEndTimeString() { return formatLocalDateTime(endTime); }

    public String getCreatedBy() { return createdBy; }

    public LocalDateTime getLastUpdate() { return lastUpdate; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }

    public long getCustomerId() { return customerId; }

    public long getUserId() { return userId; }

    public long getContactId() { return contactId; }

    public String getContactName() {
        return contactName;
    }

    /**
     * Overrides the derived contact name for the purpose of making
     * table reports organized by contact name more readable.
     * @param contactName Replacement name
     */
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    /**
     * Modifies an appointment with the provided values all at once, so that the
     * method can throw a single exception reporting all of the problems with the
     * values, should one or more of the arguments be invalid.
     * @param title Appointment title
     * @param description Appointment description
     * @param location Appointment location
     * @param type Appointment type
     * @param startTime Appointment starting time
     * @param endTime Appointment ending time
     * @param updatedBy Name of user that updated the appointment
     * @param customerId ID of customer having the appointment
     * @param userId ID of user that updated the appointment
     * @param contactId ID of contact having the appointment
     * @throws InvalidArgException when one or more of the provided arguments is
     *      invalid, in which case the error message lists all the errors
     *      in all of the arguments, delimited by semicolons
     */
    public void set(String title, String description, String location,
                    String type, LocalDateTime startTime, LocalDateTime endTime,
                    String updatedBy, long customerId, long userId, long contactId)
            throws InvalidArgException
    {
        String errors = "";
        title = title.trim();
        if (title.isEmpty())
            errors += "; title is empty";
        description = description.trim();
        if (description.isEmpty())
            errors += "; description is empty";
        type = type.trim();
        if (type.isEmpty())
            errors += "; type is empty";
        location = location.trim();
        if (location.isEmpty())
            errors += "; location is empty";
        if (contactId <= 0)
            errors += "; contact ID not specified";
        if (customerId <= 0)
            errors += "; customer ID not specified";
        if (startTime == null)
            errors += "; no starting date/time";
        if (endTime == null)
            errors += "; no ending date/time";
        if (startTime != null && endTime != null) {
            if (!endTime.isAfter(startTime))
                errors += "; end type <= start time";
        }
        updatedBy = updatedBy.trim();
        if (updatedBy.isEmpty())
            errors += "; username not specified";
        if (userId <= 0)
            errors += "; user ID not specified";

        if (!errors.isEmpty()) {
            throw new InvalidArgException(errors.substring(2));
        }

        this.title = title;
        this.description = description;
        this.location = location;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
        if (createdBy == null)
            createdBy = updatedBy;
        lastUpdatedBy = updatedBy;
        this.customerId = customerId;
        this.userId = userId;
        this.contactId = contactId;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Returns a localized representation of the provided date/time.
     * @param dateTime Date/time to represent as a string
     * @return Localized representation of the provided date/time
     */
    private String formatLocalDateTime(LocalDateTime dateTime) {
        return dateTime.format(User.DATE_FORMAT) +" "+ dateTime.format(User.TIME_FORMAT);
    }

    /**
     * Set all values that are derived from externally-provided values.
     * @param contactCache Cache of contacts loaded from the database
     */
    private void setDerivedValues(ContactCache contactCache) {
        contactName = contactCache.getById(contactId).name;
    }
}
