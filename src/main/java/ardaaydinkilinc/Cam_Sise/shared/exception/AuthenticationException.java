package ardaaydinkilinc.Cam_Sise.shared.exception;

/**
 * Exception thrown when authentication fails
 * (invalid username, password, or inactive user)
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
