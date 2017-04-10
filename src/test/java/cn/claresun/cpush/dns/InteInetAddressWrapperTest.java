package cn.claresun.cpush.dns;

import cn.claresun.cpush.exception.PoolNotReadyException;
import cn.claresun.cpush.util.Constant;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;

/**
 * Created by claresun on 16-11-8.
 */
public class InteInetAddressWrapperTest {

    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private InetAddressPool inetAddressPool;

    @Before
    public void before() throws PoolNotReadyException {
        inetAddressPool = InetAddressPool.getInstance();
    }

    @Test
    public void testGetByName() throws PoolNotReadyException {

        inetAddressPool.init(Constant.PRODUCTION_APNS_HOST);
        for (int i = 0; i < 12; i++) {
            System.out.println(inetAddressPool.next());
        }
    }

    @Test
    public void testRemoveCache() throws UnknownHostException, NoSuchFieldException, IllegalAccessException {
        printDnsEntry(InetAddress.getAllByName(Constant.PRODUCTION_APNS_HOST));
        System.out.println("******");
        InetAddressCacheUtil.removeAddressCache(Constant.PRODUCTION_APNS_HOST);

        printDnsEntry(InetAddress.getAllByName(Constant.PRODUCTION_APNS_HOST));
        System.out.println("******");
        InetAddressCacheUtil.removeAddressCache(Constant.PRODUCTION_APNS_HOST);

        printDnsEntry(InetAddress.getAllByName(Constant.PRODUCTION_APNS_HOST));
        System.out.println("******");
        InetAddressCacheUtil.removeAddressCache(Constant.PRODUCTION_APNS_HOST);

        printDnsEntry(InetAddress.getAllByName(Constant.PRODUCTION_APNS_HOST));
        System.out.println("******");
        InetAddressCacheUtil.removeAddressCache(Constant.PRODUCTION_APNS_HOST);
    }

    private void printDnsEntry(InetAddress[] inetAddresses) {
        for (InetAddress item : inetAddresses) {
            System.out.println(item);
        }
    }
}
