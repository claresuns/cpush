package cn.claresun.cpush;

/**
 * Created by claresun on 16-9-1.
 */
public interface Callback<T> {
    public void onSuccess(T result);
    public void onFailure(T result);
}
