package model;

/**
 * POJO representing a country, containing only information the application needs.
 * @author Joseph T. Lapp
 */

public class Country {

    public long id;
    public String name;

    public Country(long id, String name) {
        this.id = id;
        this.name = name;
    }
}
