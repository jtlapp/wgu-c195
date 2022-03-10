package model;

/**
 * POJO representing a contact, containing only information the application needs.
 * @author Joseph T. Lapp
 */

public class Contact {

    public long id;
    public String name;
    public String email;

    public Contact(long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    /**
     * Returns a string containing the contact's name followed by the
     * contact's unique ID in parentheses.
     * @return A string that uniquely names and identifies the contact
     */
    public String getUniqueName() {
        return name +" ("+ id +")";
    }
}
