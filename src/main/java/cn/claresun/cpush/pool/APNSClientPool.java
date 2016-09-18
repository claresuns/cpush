package cn.claresun.cpush.pool;

import cn.claresun.cpush.APNSClient;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.io.File;

/**
 * Created by claresun on 16-9-7.
 */
public class APNSClientPool {
    private GenericObjectPool<APNSClient> internalPool;

    public APNSClientPool(final APNSClientPoolConfig config, final File p12File, final String password, String host, int port) {

    }
}
