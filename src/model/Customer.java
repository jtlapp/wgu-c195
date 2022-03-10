package model;

import database.DivisionCache;
import support.InvalidArgException;

import java.time.LocalDateTime;

/**
 * Class representing an customer, containing information about the customer
 * from the database or prepared for storage in the database. The class includes a
 * set() method for simultaneously setting all modifiable properties, allowing the
 * class to provide a single error message reporting all problems in the data. The
 * class also implements some derived values for rendering in tables.
 * @author Joseph T. Lapp
 */

public class Customer implements WithID {

    /**
     * The customer's unique ID; 0 until stored in the database.
     */
    private long id; // 0 => not yet stored in DB

    private String name;

    private String address;

    private String postalCode;

    private String phone;

    private String createdBy;

    private LocalDateTime lastUpdate;

    private String lastUpdatedBy;

    private long divisionId;

    /**
     * "First level division" as derived from divisionId
     */
    private Division division; // derived

    /**
     * "First level division" name as derived from divisionId
     */
    private String divisionName; // derived

    /**
     * Country name as derived from divisionId
     */
    private String countryName; // derived

    /**
     * Constructs a customer from user-provided information in preparation
     * for storage in the database. The customer's ID will be given its
     * database-assigned value once it is stored in the database.
     * @param name Customer name
     * @param address Customer address
     * @param postalCode Customer postal code
     * @param phone Customer phone
     * @param createdBy Name of user that created the customer
     * @param divisionId ID of "first level division" of customer
     * @param divisionCache Cache of "first level divisions" loaded from the database
     * @throws InvalidArgException when one or more of the provided arguments is
     *      invalid, in which case the error message lists all the errors
     *      in all of the arguments, delimited by semicolons
     */
    public Customer(String name, String address, String postalCode,
                    String phone, String createdBy, long divisionId,
                    DivisionCache divisionCache)
            throws InvalidArgException
    {
        set(name, address, postalCode, phone, createdBy, divisionId);
        setDerivedValues(divisionCache);
    }

    /**
     * Constructs a customer from database-provided information.
     * @param id Customer's unique ID
     * @param name Customer name
     * @param address Customer address
     * @param postalCode Customer postal code
     * @param phone Customer phone
     * @param createdBy Name of user that created the customer
     * @param lastUpdate Date/time at which customer was last updated
     * @param lastUpdatedBy Name of user that last updated the customer
     * @param divisionId ID of "first level division" of customer
     * @param divisionCache Cache of "first level divisions" loaded from the database
     */
    public Customer(long id, String name, String address, String postalCode,
                    String phone, String createdBy,
                    LocalDateTime lastUpdate, String lastUpdatedBy,
                    long divisionId, DivisionCache divisionCache)
    {
        this.id = id;
        this.name = name;
        this.address = address;
        this.postalCode = postalCode;
        this.phone = phone;
        this.createdBy = createdBy;
        this.lastUpdate = lastUpdate;
        this.lastUpdatedBy = lastUpdatedBy;
        this.divisionId = divisionId;
        setDerivedValues(divisionCache);
    }

    public long getId() { return id; }

    public String getName() { return name; }

    public String getAddress() { return address; }

    public String getPostalCode() { return postalCode; };

    public String getPhone() { return phone; }

    public String getCreatedBy() { return createdBy; }

    public LocalDateTime getLastUpdate() { return lastUpdate; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }

    public long getDivisionId() { return divisionId; }

    public String getDivisionName() {
        return divisionName;
    }

    public String getCountryName() {
        return countryName;
    }

    public Division getDivision() {
        return division;
    }

    /**
     * Returns a string containing the customer's name followed by the
     * customer's unique ID in parentheses.
     * @return A string that uniquely names and identifies the customer
     */
    public String getUniqueName() { return name +" ("+ id +")"; }

    /**
     * Modifies a customer with the provided values all at once, so that the
     * method can throw a single exception reporting all of the problems with the
     * values, should one or more of the arguments be invalid.
     * @param name Customer name
     * @param address Customer address
     * @param postalCode Customer postal code
     * @param phone Customer phone
     * @param updatedBy Name of user that last updated the customer
     * @param divisionId ID of "first level division" of customer
     * @throws InvalidArgException when one or more of the provided arguments is
     *      invalid, in which case the error message lists all the errors
     *      in all of the arguments, delimited by semicolons
     */
    public void set(String name, String address, String postalCode,
                    String phone, String updatedBy, long divisionId)
            throws InvalidArgException
    {
        String errors = "";
        name = name.trim();
        if (name.isEmpty())
            errors += "; name is empty";
        address = address.trim();
        if (address.isEmpty())
            errors += "; address is empty";
        postalCode = postalCode.trim();
        if (postalCode.isEmpty())
            errors += "; postal code is empty";
        phone = phone.trim();
        if (phone.isEmpty())
            errors += "; phone is empty";
        updatedBy = updatedBy.trim();
        if (updatedBy.isEmpty())
            errors += "; user not specified";
        if (divisionId <= 0)
            errors += "; division not specified";

        if (!errors.isEmpty()) {
            throw new InvalidArgException(errors.substring(2));
        }

        this.name = name;
        this.address = address;
        this.postalCode = postalCode;
        this.phone = phone;
        if (createdBy == null)
            createdBy = updatedBy;
        lastUpdatedBy = updatedBy;
        this.divisionId = divisionId;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * Set all values that are derived from externally-provided values.
     * @param divisionCache Cache of "first level divisions" loaded from the database
     */
    private void setDerivedValues(DivisionCache divisionCache) {
        division = divisionCache.getById(divisionId);
        divisionName = division.name;
        countryName = division.country.name;
    }
}
