package sinalgo.exception;

public class PositionFileException extends WrongConfigurationException { // needs not be caught

    private static final long serialVersionUID = -2584122313897498869L;

    public PositionFileException(String msg) {
        super(msg);
    }
}
