package cn.claresun.cpush.exception;

/**
 * Created by claresun on 16-8-18.
 */
public class NotConnectedException extends Exception {
    public NotConnectedException(String message) {
        super(message);
    }

    public NotConnectedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
