package sinalgo.exception;

/**
 * An exception to wrap handled runtime exceptions
 */
public class SinalgoFatalException extends RuntimeException {

    private static final long serialVersionUID = 4262730760936794440L;

    public SinalgoFatalException(String message) {
        super(message);
    }

    public SinalgoFatalException(String message, Throwable cause) {
        super(message, cause);
    }

}
