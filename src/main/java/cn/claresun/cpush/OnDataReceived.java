package cn.claresun.cpush;

/**
 * Created by claresun on 16-9-6.
 */
public interface OnDataReceived<T> {
    void received(T data);
}
