package server.exception;

public class WebConfigDuplicatedException extends Exception {
    public WebConfigDuplicatedException(String msg) {
        super(msg);
    }
}