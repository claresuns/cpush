package cn.claresun.cpush.pool;

import cn.claresun.cpush.APNSClient;
import cn.claresun.cpush.exception.NotConnectedException;
import cn.claresun.cpush.util.Constant;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.File;

/**
 * Created by claresun on 16-9-8.
 */
public class APNSClientFactory implements PooledObjectFactory<APNSClient> {
    @Override
    public PooledObject<APNSClient> makeObject() throws Exception {
        APNSClient apnsClient = new APNSClient(new File("/home/claresun/cert/autohome.p12"), "111111", Constant.PRODUCTION_APNS_HOST, Constant.DEFAULT_APNS_PORT);
        try {
            apnsClient.connect();
        } catch (Exception ex) {
            if (!(ex instanceof NotConnectedException)) {
                apnsClient.disconnect();
            }

            throw ex;
        }
        return new DefaultPooledObject<APNSClient>(apnsClient) {
        };
    }

    @Override
    public void destroyObject(PooledObject<APNSClient> p) throws Exception {

    }

    @Override
    public boolean validateObject(PooledObject<APNSClient> p) {
        return false;
    }

    @Override
    public void activateObject(PooledObject<APNSClient> p) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<APNSClient> p) throws Exception {

    }
}
