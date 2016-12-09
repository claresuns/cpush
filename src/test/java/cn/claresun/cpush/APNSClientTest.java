package cn.claresun.cpush;


import cn.claresun.cpush.handler.APNSNotification;
import cn.claresun.cpush.handler.APNSNotificationResponse;
import cn.claresun.cpush.handler.APNSPayloadBuilder;
import cn.claresun.cpush.util.Constant;
import cn.claresun.cpush.util.SSLUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.Semaphore;

/**
 * APNSClientTest Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>九月 1, 2016</pre>
 */
public class APNSClientTest {

    private APNSClient apnsClient;
    final Semaphore semp = new Semaphore(1);

    @Before
    public void before() throws Exception {
        semp.acquire();
        apnsClient = new APNSClient(new File("/home/claresun/cert/autohome.p12"), "111111", Constant.PRODUCTION_APNS_HOST, Constant.DEFAULT_APNS_PORT);
        apnsClient.setWriteLimitBytes(50);
        apnsClient.onDataReceived(new OnDataReceived<APNSNotificationResponse>() {
            @Override
            public void received(APNSNotificationResponse data) {
                System.out.println(data);
                semp.release();
            }
        });
    }

    @Test
    public void testConnect() throws InterruptedException {

        try {
            this.apnsClient.connect().await();
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    @Test
    public void testDisConnect() throws InterruptedException {
        this.apnsClient.disconnect();
    }

    @Test
    public void testSend() throws Exception {
        testConnect();
        final APNSNotification pushNotification;

        {
            final APNSPayloadBuilder payloadBuilder = new APNSPayloadBuilder();
            payloadBuilder.setAlertBody("autohome");

            final String payload = payloadBuilder.buildWithDefaultMaximumLength();
            final String token = SSLUtil.sanitizeTokenString("d7939eac21d9182100711cf45a7a117ccf58685fc00047ad0ff9a0f494504a1a1");

            pushNotification = new APNSNotification(token, "com.autohome", payload);
        }
        try {
            this.apnsClient.send(pushNotification).await().addListener(new GenericFutureListener<Future<? super Result>>() {
                @Override
                public void operationComplete(Future<? super Result> future) throws Exception {
                    if (future.isSuccess()) {

                    } else {
                        System.out.println(future.cause());
                    }
                }
            });

        } catch (Exception ex) {

        }

        semp.acquire();

        testDisConnect();
    }

    @After
    public void after() throws Exception {
    }
} 
