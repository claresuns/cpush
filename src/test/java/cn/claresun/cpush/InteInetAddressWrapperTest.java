package cn.claresun.cpush;

import cn.claresun.cpush.dns.DnsCache;
import cn.claresun.cpush.dns.InetAddressPool;
import cn.claresun.cpush.dns.InetAddressWrapper;
import cn.claresun.cpush.exception.PoolNotReadyException;
import cn.claresun.cpush.util.Constant;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;

/**
 * Created by claresun on 16-11-8.
 */
public class InteInetAddressWrapperTest {

    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private InetAddressPool inetAddressPool;

    @Before
    public void before() throws PoolNotReadyException {
        inetAddressPool=InetAddressPool.getInstance();
    }

    @Test
    public void testGetByName() throws PoolNotReadyException {

        inetAddressPool.init(Constant.PRODUCTION_APNS_HOST);
        for (int i = 0; i < 12 ; i++) {
            System.out.println(inetAddressPool.next());
        }
    }
}
