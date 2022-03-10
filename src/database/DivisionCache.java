package database;

import model.Division;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Cache of country "first level divisions" pulled from the database. Used for lookups.
 * @author Joseph T. Lapp
 */

public class DivisionCache extends LookupCache<Division> {

    final CountryCache countryCache;

    /**
     * Constructs a cache of all "first level divisions" in the database
     * @param conn An open database connection, which it leaves open
     * @param countryCache A previously-loaded cache of all countries
     * @throws DatabaseException on database error
     */
    public DivisionCache(Connection conn, CountryCache countryCache) throws DatabaseException {
        this.countryCache = countryCache;
        try (PreparedStatement ps =
                     conn.prepareStatement("SELECT * FROM first_level_divisions")) {
            var results = ps.executeQuery();
            while (results.next()) {
                var divisionId = results.getLong("Division_ID");
                var countryId = results.getLong("COUNTRY_ID");
                var country = countryCache.getById(countryId);
                var division = new Division(
                        divisionId, results.getString("Division"), country);
                idMap.put(division.id, division);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error preparing divisions", e);
        }
    }

    /**
     * Returns the division having the given name and found in the given country.
     * The method allows for the possibility that multiple countries have "first
     * level divisions" with identical names.
     * @param countryName Country containing the desired division
     * @param divisionName Name of the desired division
     * @return The division in the given country with the given name
     */
    public Division getByName(String countryName, String divisionName) {
        for (var division : idMap.values()) {
            if (division.name.equals(divisionName) &&
                    division.country.name.equals(countryName)) {
                return division;
            }
        }
        return null;
    }
}
