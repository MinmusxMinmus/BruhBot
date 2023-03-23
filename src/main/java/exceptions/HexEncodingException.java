package exceptions;

public class HexEncodingException extends Exception {

    public HexEncodingException(String message) {
        super(message);
    }

    public HexEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
