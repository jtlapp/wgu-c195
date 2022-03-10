package database;

import model.Country;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Cache of countries pulled from the database. Used for lookups.
 * @author Joseph T. Lapp
 */

public class CountryCache extends LookupCache<Country> {

    /**
     * Constructs a cache of all countries in the database
     * @param conn An open database connection, which it leaves open
     * @throws DatabaseException on database error
     */
    public CountryCache(Connection conn) throws DatabaseException {
        try (PreparedStatement ps =
                     conn.prepareStatement("SELECT Country_ID, Country FROM countries")) {
            var results = ps.executeQuery();
            while (results.next()) {
                var country = new Country(
                        results.getLong("Country_ID"),
                        results.getString("Country")
                );
                idMap.put(country.id, country);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing countries", e);
        }
    }
}
