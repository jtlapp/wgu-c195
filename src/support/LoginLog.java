package support;

import model.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Class representing the log file of user logins. It merely appends
 * login records to the file, creating the file first if necessary.
 * @author Joseph T. Lapp
 */

public class LoginLog {

    /**
     * File in which to store the login logs
     */
    public final static String LOGIN_FILE = "login_activity.txt";

    /**
     * Appends a record for a login to the log file.
     * @param username Username used in the attempted login
     * @param succeeded Whether the login succeeded
     * @throws IOException on failure to append to the log file
     */
    public static void append(String username, boolean succeeded)
            throws IOException
    {
        var now = LocalDateTime.now();
        var logLine = "Login on " + now.format(User.DATE_FORMAT) +
                " at "+ now.format(User.TIME_FORMAT) +
                " by username '"+ username +"' - " +
                (succeeded ? "succeeded" : "failed");

        var bufWriter = Files.newBufferedWriter(Paths.get(LOGIN_FILE),
                StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        var printer = new PrintWriter(bufWriter, true);
        printer.println(logLine);
    }
}
