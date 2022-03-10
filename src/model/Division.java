package model;

/**
 * POJO representing a "first level division" of a country, containing only
 * information the application needs.
 * @author Joseph T. Lapp
 */

public class Division {

    public long id;
    public String name;
    public Country country;

    public Division(long id, String name, Country country) {
        this.id = id;
        this.name = name;
        this.country = country;
    }
}
