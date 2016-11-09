package cn.claresun.cpush.exception;

/**
 * Created by claresun on 16-11-8.
 */
public class PoolNotReadyException extends Exception{
    public PoolNotReadyException() {
    }

    public PoolNotReadyException(String host) {
        super(host);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
