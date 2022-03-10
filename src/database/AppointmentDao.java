package database;

import model.Appointment;
import model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO providing read/write access to appointments in the database, which it
 * represents in the application as instances of Appointment. It also provides
 * specialized queries for reports and appointment overlap detection.
 * @author Joseph T. Lapp
 */

public class AppointmentDao extends ReadWriteDao<Appointment> {

    /**
     * POJO for objects returned by getMonthTypeCounts() for the report
     * of appointment counts by appointment month and type.
     */
    public static class MonthTypeCount {
        String month;
        String type;
        int count;

        MonthTypeCount(String month, String type, int count) {
            this.month = month;
            this.type = type;
            this.count = count;
        }

        public String getMonth() { return month; }
        public String getType() { return type; }
        public int getCount() { return count; }
        public void setMonth(String month) { this.month = month; }
    }

    /**
     * Cache of all contacts found in the database
     */
    private final ContactCache contactCache;

    private static final String[] INDEXED_MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private final PreparedStatement insert;
    private final PreparedStatement updateById;
    private final PreparedStatement selectOverlap;
    private final PreparedStatement selectMonthTypes;
    private final PreparedStatement selectAllOrderedByContact;

    /**
     * Constructs an appointment DAO in contexts where a contact cache is
     * NOT already available. It loads a contact cache from the database.
     * @param conn An open database connection, which it leaves open
     * @param user The logged-in use
     * @throws DatabaseException on database error
     */
    public AppointmentDao(Connection conn, User user)
            throws DatabaseException
    {
        this(conn, user, new ContactCache(conn));
    }

    /**
     * Constructs an appointment DAO in contexts where a contact cache is
     * already available, sparing the DAO from having to load it.
     * @param conn An open database connection, which it leaves open
     * @param user The logged-in use
     * @param contactCache The contact cache
     * @throws DatabaseException on database error
     */
    public AppointmentDao(Connection conn, User user, ContactCache contactCache)
            throws DatabaseException
    {
        super(conn, user,"appointment", "appointments",
                "Appointment_ID", "Start");
        this.contactCache = contactCache;

        try {
            insert = conn.prepareStatement("INSERT INTO appointments (" +
                    "Title, Description, Location, Type, Start, End, " +
                    "Created_By, Last_Update, Last_Updated_By, Customer_ID, " +
                    "User_ID, Contact_ID) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            updateById = conn.prepareStatement("UPDATE appointments SET " +
                    "Title=?, Description=?, Location=?, Type=?, Start=?, End=?, " +
                    "Created_By=?, Last_Update=?, Last_Updated_By=?, " +
                    "Customer_ID=?, User_ID=?, Contact_ID=? WHERE Appointment_ID=?");
            selectOverlap = conn.prepareStatement("SELECT COUNT(*) FROM appointments " +
                    "WHERE Customer_ID=? AND Start < ? AND End > ? "+
                    "AND Appointment_ID <> ?");
            selectMonthTypes = conn.prepareStatement("SELECT MONTH(Start) AS Month, " +
                    "Type, COUNT(*) AS Count FROM appointments GROUP BY Month, Type "+
                    "ORDER BY Month, Type");
            selectAllOrderedByContact = conn.prepareStatement("SELECT * "+
                    "FROM appointments NATURAL JOIN contacts ORDER BY Contact_Name");
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing "+ itemName +" DAO", e);
        }
    }

    /**
     * Queries the database for all appointments ordered by contact.
     * @return A list of all appointments ordered by contact
     * @throws DatabaseException on database error
     */
    public List<Appointment> getAllOrderedByContact() throws DatabaseException
    {
        var items = new ArrayList<Appointment>();
        try {
            var results = selectAllOrderedByContact.executeQuery();
            while (results.next()) {
                items.add(createItem(results));
            }
            return items;
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting appointments by contact", e);
        }
    }

    /**
     * Queries the database for appointment counts by appointment month and type.
     * @return A list of appointment counts by appointment month and type
     * @throws DatabaseException on database error
     */
    public List<MonthTypeCount> getMonthTypeCounts()
            throws DatabaseException
    {
        var items = new ArrayList<MonthTypeCount>();
        try {
            var results = selectMonthTypes.executeQuery();
            while (results.next()) {
                items.add(new MonthTypeCount(
                        INDEXED_MONTHS[results.getInt("Month") - 1],
                        results.getString("Type"),
                        results.getInt("Count")
                ));
            }
            return items;
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting month type counts", e);
        }
    }

    /**
     * Queries the database for the number of appointments in the database whose
     * meeting times overlap with proposed meeting times for a given customer.
     * @param customerId ID of customer for whom to look for meeting overlap
     * @param appointmentStart Proposed start date/time of the meeting
     * @param appointmentEnd Proposed end date/time of the meeting
     * @param excludedAppointmentId The ID of the appointment to exclude from checking
     *              for overlap, in case the user is modifying an existing appointment;
     *              specify 0 when adding a new appointment
     * @return The number of appointments in the database whose meeting times overlap
     *              with proposed meeting times for the indicated customer
     * @throws DatabaseException on database error
     */
    public int getMeetingOverlapCount(long customerId,
                                      LocalDateTime appointmentStart,
                                      LocalDateTime appointmentEnd,
                                      long excludedAppointmentId)
            throws DatabaseException
    {
        try {
            selectOverlap.setLong(1, customerId);
            selectOverlap.setTimestamp(2, Timestamp.valueOf(appointmentEnd));
            selectOverlap.setTimestamp(3, Timestamp.valueOf(appointmentStart));
            selectOverlap.setLong(4, excludedAppointmentId);
            var results = selectOverlap.executeQuery();
            if (!results.next()) {
                throw new DatabaseException("Overlap count query didn't return a count");
            }
            return results.getInt(1);
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting "+ itemName +" overlap", e);
        }
    }

    /**
     * Closes any open resources in this DAO, but does not close the
     * database connection, allowing the connection to be shared with
     * other DAOs for faster processing of a sequence of operations.
     * @throws DatabaseException on database error
     */
    public void close() throws DatabaseException
    {
        super.close();
        try {
            insert.close();
            updateById.close();
            selectOverlap.close();
            selectMonthTypes.close();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error closing "+ itemName +" DAO", e);
        }
    }

    /**
     * Returns an insert statement prepared for inserting an appointment.
     * @return An insert statement for inserting an appointment
     */
    protected PreparedStatement getInsertStatement() {
        return insert;
    }

    /**
     * Returns an update statement prepared for updating an appointment. The last
     * parameter of this prepared statement must take an appointment ID.
     * @return An update statement for updating an appointment
     */
    protected PreparedStatement getUpdateStatement() {
        return updateById;
    }

    /**
     * Creates an appointment from the provided result set.
     * @param results Query results containing properties of the appointment
     * @return The newly-created appointment
     * @throws DatabaseException on database error
     */
    protected Appointment createItem(ResultSet results)
            throws DatabaseException
    {
        try {
            return new Appointment(
                    results.getLong("Appointment_ID"),
                    results.getString("Title"),
                    results.getString("Description"),
                    results.getString("Location"),
                    results.getString("Type"),
                    results.getTimestamp("Start").toLocalDateTime(),
                    results.getTimestamp("End").toLocalDateTime(),
                    results.getString("Created_By"),
                    results.getTimestamp("Last_Update").toLocalDateTime(),
                    results.getString("Last_Updated_By"),
                    results.getLong("Customer_ID"),
                    results.getLong("User_ID"),
                    results.getLong("Contact_ID"),
                    contactCache
            );
        }
        catch (SQLException e) {
            throw new DatabaseException("Error loading "+ itemName, e);
        }
    }

    /**
     * Loads a prepared statement with the properties of an appointment.
     * @param ps Prepared statement to load
     * @param appointment Appointment from which to load the prepared statement
     * @throws DatabaseException on database error
     */
    protected void loadStatement(PreparedStatement ps, Appointment appointment)
            throws DatabaseException
    {
        try {
            ps.setString(1, appointment.getTitle());
            ps.setString(2, appointment.getDescription());
            ps.setString(3, appointment.getLocation());
            ps.setString(4, appointment.getType());
            ps.setTimestamp(5, Timestamp.valueOf(appointment.getStartTime()));
            ps.setTimestamp(6, Timestamp.valueOf(appointment.getEndTime()));
            ps.setString(7, appointment.getCreatedBy());
            ps.setDate(8, null); // let DB set last update time
            ps.setString(9, user.name); // lastUpdatedBy
            ps.setLong(10, appointment.getCustomerId());
            ps.setLong(11, appointment.getUserId());
            ps.setLong(12, appointment.getContactId());
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing "+ itemName, e);
        }
    }
}
