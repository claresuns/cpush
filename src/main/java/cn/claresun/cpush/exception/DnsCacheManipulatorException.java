package cn.claresun.cpush.exception;

/**
 * Created by claresun on 16-11-8.
 */
public class DnsCacheManipulatorException extends RuntimeException{
    public DnsCacheManipulatorException(String message) {
        super(message);
    }

    public DnsCacheManipulatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
