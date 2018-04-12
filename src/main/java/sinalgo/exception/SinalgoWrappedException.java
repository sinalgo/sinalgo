package sinalgo.exception;

/**
 * A class to wrap unhandled yet expected exceptions
 * (basically those which used to call Main.fatalError(Throwable t))
 */
public class SinalgoWrappedException extends RuntimeException {

    private static final long serialVersionUID = -5947479644861503464L;

    public SinalgoWrappedException(Throwable cause) {
        super(cause);
    }

}
