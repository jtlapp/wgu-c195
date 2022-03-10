package database;

import model.Customer;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO providing read/write access to customers in the database, which it
 * represents in the application as instances of Customer. It also provides
 * a specialized query for one of the reports.
 * @author Joseph T. Lapp
 */

public class CustomerDao extends ReadWriteDao<Customer> {

    /**
     * POJO for objects returned by getCountryDivisionCounts() for the report
     * of customer counts by customer country and "first level division".
     */
    public static class CountryDivisionCount {
        String country;
        String division;
        int count;

        CountryDivisionCount(String country, String division, int count) {
            this.country = country;
            this.division = division;
            this.count = count;
        }

        public String getCountry() { return country; }
        public String getDivision() { return division; }
        public int getCount() { return count; }
        public void setCountry(String country) { this.country = country; }
        public void setDivision(String division) { this.division = division; }
    }

    /**
     * Cache of all "first level divisions" found in the database
     */
    private final DivisionCache divisionCache;

    private final PreparedStatement insert;
    private final PreparedStatement updateById;
    private final PreparedStatement deleteAppointments;
    private final PreparedStatement selectCountryDivisions;

    /**
     * Constructs an customer DAO in contexts where a division cache is
     * NOT already available. It loads a division cache from the database.
     * @param conn An open database connection, which it leaves open
     * @param user The logged-in use
     * @throws DatabaseException on database error
     */
    public CustomerDao(Connection conn, User user)
            throws DatabaseException
    {
        this(conn, user, new DivisionCache(conn, new CountryCache(conn)));
    }

    /**
     * Constructs a customer DAO in contexts where a division cache is
     * already available, sparing the DAO from having to load it.
     * @param conn An open database connection, which it leaves open
     * @param user The logged-in use
     * @param divisionCache The division cache
     * @throws DatabaseException on database error
     */
    public CustomerDao(Connection conn, User user, DivisionCache divisionCache)
            throws DatabaseException
    {
        super(conn, user,"customer", "customers",
                "Customer_ID",null);
        this.divisionCache = divisionCache;

        try {
            insert = conn.prepareStatement("INSERT INTO customers (" +
                    "Customer_Name, Address, Postal_Code, Phone, " +
                    "Created_By, Last_Update, Last_Updated_By, Division_ID) "+
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            updateById = conn.prepareStatement("UPDATE customers SET " +
                    "Customer_Name=?, Address=?, Postal_Code=?, Phone=?, " +
                    "Created_By=?, Last_Update=?, Last_Updated_By=?, Division_ID=? "+
                    "WHERE Customer_ID=?");
            deleteAppointments = conn.prepareStatement("DELETE FROM appointments "+
                    "WHERE Customer_ID=?");
            selectCountryDivisions = conn.prepareStatement(
                    "SELECT Country, Division, COUNT(*) AS Count "+
                    "FROM customers, first_level_divisions, countries "+
                    "WHERE customers.Division_ID = first_level_divisions.Division_ID AND "+
                            "first_level_divisions.COUNTRY_ID = countries.Country_ID "+
                    "GROUP BY Country, Division ORDER BY Country, Division");
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing " + itemName + " DAO", e);
        }
    }

    /**
     * Deletes the indicated customer from the database, first deleting any
     * appointments that reference the customer.
     * @param customer Customer to delete
     * @throws DatabaseException on database error
     */
    @Override
    public void delete(Customer customer) throws DatabaseException {
        try {
            deleteAppointments.setLong(1, customer.getId());
            // JDBC driver returns 0 on successful deletion, so ignore return value.
            deleteAppointments.executeUpdate();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error deleting "+ itemName +" appointments", e);
        }
        super.delete(customer);
    }

    /**
     * Queries the database for customer counts by customer country and
     * "first level division".
     * @return A list of customer counts by customer country and division
     * @throws DatabaseException on database error
     */
    public List<CountryDivisionCount> getCountryDivisionCounts()
            throws DatabaseException
    {
        var items = new ArrayList<CountryDivisionCount>();
        try {
            var results = selectCountryDivisions.executeQuery();
            while (results.next()) {
                items.add(new CountryDivisionCount(
                        results.getString("Country"),
                        results.getString("Division"),
                        results.getInt("Count")
                ));
            }
            return items;
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting country division counts", e);
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
            deleteAppointments.close();
        }
        catch (SQLException e) {
            throw new DatabaseException("Error closing " + itemName + " DAO", e);
        }
    }

    /**
     * Returns an insert statement prepared for inserting a customer.
     * @return An insert statement for inserting a customer
     */
    protected PreparedStatement getInsertStatement() {
        return insert;
    }

    /**
     * Returns an update statement prepared for updating a customer. The last
     * parameter of this prepared statement must take a customer ID.
     * @return An update statement for updating a customer
     */
    protected PreparedStatement getUpdateStatement() {
        return updateById;
    }

    /**
     * Creates a customer from the provided result set.
     * @param results Query results containing properties of the customer
     * @return The newly-created customer
     * @throws DatabaseException on database error
     */
    protected Customer createItem(ResultSet results)
            throws DatabaseException
    {
        try {
            return new Customer(
                    results.getLong("Customer_ID"),
                    results.getString("Customer_Name"),
                    results.getString("Address"),
                    results.getString("Postal_Code"),
                    results.getString("Phone"),
                    results.getString("Created_By"),
                    results.getTimestamp("Last_Update").toLocalDateTime(),
                    results.getString("Last_Updated_By"),
                    results.getLong("Division_ID"),
                    divisionCache
            );
        }
        catch (SQLException e) {
            throw new DatabaseException("Error loading " + itemName, e);
        }
    }

    /**
     * Loads a prepared statement with the properties of a customer.
     * @param ps Prepared statement to load
     * @param customer Customer from which to load the prepared statement
     * @throws DatabaseException on database error
     */
    protected void loadStatement(PreparedStatement ps, Customer customer)
            throws DatabaseException
    {
        try {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getAddress());
            ps.setString(3, customer.getPostalCode());
            ps.setString(4, customer.getPhone());
            ps.setString(5, customer.getCreatedBy());
            ps.setTimestamp(6, null); // let DB set last update time
            ps.setString(7, user.name); // lastUpdatedBy
            ps.setLong(8, customer.getDivisionId());
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing " + itemName, e);
        }
    }
}
