package sinalgo.exception;

public class ResourceReadException extends SinalgoFatalException {

    private static final long serialVersionUID = 7977130059296417754L;

    @Override
    public String getDefaultFormat() {
        return "Could not read resource '%s'";
    }

    public ResourceReadException(String message) {
        super(message);
    }

    public ResourceReadException(Throwable cause) {
        super(cause);
    }

    public ResourceReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceReadException(String message, String format) {
        super(message, format);
    }

    public ResourceReadException(Throwable cause, String format) {
        super(cause, format);
    }

    public ResourceReadException(String message, Throwable cause, String format) {
        super(message, cause, format);
    }

}
