package database;

import model.Contact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Cache of contacts pulled from the database. Used for lookups.
 * @author Joseph T. Lapp
 */

public class ContactCache extends LookupCache<Contact> {

    /**
     * Constructs a cache of all contacts in the database
     * @param conn An open database connection, which it leaves open
     * @throws DatabaseException on database error
     */
    public ContactCache(Connection conn) throws DatabaseException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM contacts")) {
            var results = ps.executeQuery();
            while (results.next()) {
                var contact = new Contact(
                        results.getLong("Contact_ID"),
                        results.getString("Contact_Name"),
                        results.getString("Email")
                );
                idMap.put(contact.id, contact);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing contacts", e);
        }
    }
}
