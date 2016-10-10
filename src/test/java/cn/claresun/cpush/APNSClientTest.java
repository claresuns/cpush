package cn.claresun.cpush;

import cn.claresun.cpush.model.APNSNotification;
import cn.claresun.cpush.model.APNSNotificationResponse;
import cn.claresun.cpush.model.APNSPayloadBuilder;
import cn.claresun.cpush.util.Constant;
import cn.claresun.cpush.util.SslUtil;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.File;

/**
 * APNSClientTest Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>九月 1, 2016</pre>
 */
public class APNSClientTest {

    private APNSClient apnsClient;

    @Before
    public void before() throws Exception {
        apnsClient = new APNSClient(new File("/home/claresun/cert/autohome.p12"), "111111", Constant.PRODUCTION_APNS_HOST, Constant.DEFAULT_APNS_PORT);
        apnsClient.onDataReceived(new OnDataReceived<APNSNotificationResponse>() {
            @Override
            public void received(APNSNotificationResponse data) {
                System.out.println(data);
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
    public void testSendAsync() throws Exception {

        testConnect();

        final APNSNotification pushNotification;

        {
            final APNSPayloadBuilder payloadBuilder = new APNSPayloadBuilder();
            payloadBuilder.setAlertBody("autohome");

            final String payload = payloadBuilder.buildWithDefaultMaximumLength();
            final String token = SslUtil.sanitizeTokenString("d7939eac21d9182100711cf45a7a117ccf58685fc00047ad0ff9a0f494504a1a");

            pushNotification = new APNSNotification(token, "com.autohome", payload);
        }
        try {
            this.apnsClient.sendAsynchronous(pushNotification, new Callback<APNSNotificationResponse>() {
                @Override
                public void onSuccess(APNSNotificationResponse result) {
                    System.out.println("Write success.");
                }

                @Override
                public void onFailure(APNSNotificationResponse result) {
                    System.out.println(result);
                }
            });


        } catch (Exception ex) {

        }


        for (; ; ) {
            Thread.sleep(100000);
        }

        //testDisConnect();
    }

    @Test
    public void testSend() throws Exception {
        testConnect();
        final APNSNotification pushNotification;

        {
            final APNSPayloadBuilder payloadBuilder = new APNSPayloadBuilder();
            payloadBuilder.setAlertBody("autohome");

            final String payload = payloadBuilder.buildWithDefaultMaximumLength();
            final String token = SslUtil.sanitizeTokenString("d7939eac21d9182100711cf45a7a117ccf58685fc00047ad0ff9a0f494504a1a");

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

        testDisConnect();
    }

    @Test
    public void testMutliSend() throws Exception {
        testConnect();

        Runnable r = new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                final APNSNotification pushNotification;

                {
                    final APNSPayloadBuilder payloadBuilder = new APNSPayloadBuilder();
                    payloadBuilder.setAlertBody("autohome");

                    final String payload = payloadBuilder.buildWithDefaultMaximumLength();
                    final String token = SslUtil.sanitizeTokenString("d7939eac21d9182100711cf45a7a117ccf58685fc00047ad0ff9a0f494504a1a");

                    pushNotification = new APNSNotification(token, "com.autohome", payload);
                }
                try {
                    APNSClientTest.this.apnsClient.send(pushNotification).await().addListener(new GenericFutureListener<Future<? super Result>>() {
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

                System.out.println("end");

            }
        };

        for (int i = 0; i <20 ; i++) {
            Thread t1=new Thread(r);
            t1.start();
        }

        while (true){
            Thread.sleep(10000);
        }
    }

    @After
    public void after() throws Exception {
    }
} 
