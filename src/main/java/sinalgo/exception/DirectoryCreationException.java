package sinalgo.exception;

public class DirectoryCreationException extends SinalgoFatalException {

    private static final long serialVersionUID = -8747206874245076820L;

    @Override
    public String getDefaultFormat() {
        return "Could not create directory '%s'";
    }

    public DirectoryCreationException(String message) {
        super(message);
    }

    public DirectoryCreationException(Throwable cause) {
        super(cause);
    }

    public DirectoryCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectoryCreationException(String message, String format) {
        super(message, format);
    }

    public DirectoryCreationException(Throwable cause, String format) {
        super(cause, format);
    }

    public DirectoryCreationException(String message, Throwable cause, String format) {
        super(message, cause, format);
    }


}
