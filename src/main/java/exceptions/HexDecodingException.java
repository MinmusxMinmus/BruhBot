package exceptions;

public class HexDecodingException extends Exception {
    public HexDecodingException(String message) {
        super(message);
    }

    public HexDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
