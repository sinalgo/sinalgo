package sinalgo.exception;

/**
 * An Exception to wrap handled runtime exceptions
 */
public class SinalgoFatalException extends RuntimeException {

    private static final long serialVersionUID = 4262730760936794440L;

    private final String format;

    public String getDefaultFormat() {
        return "%s";
    }

    public final String getFormat() {
        return this.format;
    }

    public SinalgoFatalException(String message) {
        super(message);
        this.format = null;
    }

    SinalgoFatalException(Throwable cause) {
        super(cause);
        this.format = null;
    }

    public SinalgoFatalException(String message, Throwable cause) {
        super(message, cause);
        this.format = null;
    }

    public SinalgoFatalException(String message, String format) {
        super(message);
        this.format = format;
    }

    public SinalgoFatalException(Throwable cause, String format) {
        super(cause);
        this.format = format;
    }

    public SinalgoFatalException(String message, Throwable cause, String format) {
        super(message, cause);
        this.format = format;
    }

}
