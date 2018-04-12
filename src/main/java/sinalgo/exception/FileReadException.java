package sinalgo.exception;

public class FileReadException extends SinalgoFatalException {

    private static final long serialVersionUID = 21316082303620502L;

    @Override
    public String getDefaultFormat() {
        return "Could not read file '%s'";
    }

    public FileReadException(String message) {
        super(message);
    }

    public FileReadException(Throwable cause) {
        super(cause);
    }

    public FileReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileReadException(String message, String format) {
        super(message, format);
    }

    public FileReadException(Throwable cause, String format) {
        super(cause, format);
    }

    public FileReadException(String message, Throwable cause, String format) {
        super(message, cause, format);
    }


}
