package support;

/**
 * Exception reporting that a method was provided with an invalid argument.
 * Used to validate arguments that might have been sourced by a user,
 * allowing the application to catch and present the error to the user.
 * @author Joseph T. Lapp
 */

public class InvalidArgException extends Exception {

    /**
     * Constructs an invalid argument exception
     * @param message Description of the problem(s) with the argument(s)
     */
    public InvalidArgException(String message) {
        super(message);
    }
}
