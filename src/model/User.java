package model;

import java.time.format.DateTimeFormatter;

/**
 * POJO representing a user and providing date and time formatting
 * constants, which might be user-configurable in a more elaborate
 * solution. It only contains information the application needs.
 * @author Joseph T. Lapp
 */

public class User {

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public long id;
    public String name;

    public User(long id, String name) {
        this.id = id;
        this.name = name;
    }
}
