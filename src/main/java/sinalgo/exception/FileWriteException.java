package sinalgo.exception;

public class FileWriteException extends SinalgoFatalException {

    private static final long serialVersionUID = -7836998205915193798L;

    @Override
    public String getDefaultFormat() {
        return "Could not write file '%s'";
    }

    public FileWriteException(String message) {
        super(message);
    }

    public FileWriteException(Throwable cause) {
        super(cause);
    }

    public FileWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileWriteException(String message, String format) {
        super(message, format);
    }

    public FileWriteException(Throwable cause, String format) {
        super(cause, format);
    }

    public FileWriteException(String message, Throwable cause, String format) {
        super(message, cause, format);
    }


}
