package cn.claresun.cpush.dns;

import cn.claresun.cpush.exception.PoolNotReadyException;
import cn.claresun.cpush.util.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by claresun on 16-11-8.
 */
public class InetAddressPool {
    private static final Logger log = LoggerFactory.getLogger(InetAddressPool.class);

    private static final int DEFAULT_ADDRESSES_SIZE = 64;
    private int capacity;

    ArrayList<InetAddressWrapper> addressList = new ArrayList<>(DEFAULT_ADDRESSES_SIZE);
    private int index;

    private ScheduledExecutorService updaterExecutorService = Executors.newScheduledThreadPool(1);

    public synchronized InetAddress next() {
        checkIndexRange();
        return this.addressList.get(this.index++).getAddress();
    }

    private void checkIndexRange() {
        if (this.index >= this.capacity) {
            this.index = 0;
        }
    }

    private void add(InetAddressWrapper addressWrapper) {
        this.addressList.add(addressWrapper);
    }

    public InetAddressPool init(String hostName) throws PoolNotReadyException {
        if (hostName == null || hostName.isEmpty()) {
            hostName = Constant.PRODUCTION_APNS_HOST;
        }

        initializePool(hostName);
        checkPoolCapacity();
        return this;
    }

    private void checkPoolCapacity() throws PoolNotReadyException {
        if (this.capacity < 4) {
            log.error("The inetAddress pool's capacity is less than 4.");
            throw new PoolNotReadyException();
        }
    }

    private void initializePool(String hostName) throws PoolNotReadyException {

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(hostName);
        } catch (UnknownHostException e) {
            log.error("The InetAddress pool initialize failured.");
            throw new PoolNotReadyException();
        }

        for (InetAddress item : addresses) {
            if (item == null) {
                return;
            }
            this.add(new InetAddressWrapper(item));
            this.capacity++;
        }
    }

    private void initializeUpdater() {
        updaterExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {

            }
        }, 1, 30, TimeUnit.MINUTES);
    }

    //Initialization
    private static class SingletonHolder {
        private static InetAddressPool instance = new InetAddressPool();
    }

    private InetAddressPool() {
    }

    public static InetAddressPool getInstance() {
        return SingletonHolder.instance;
    }
}
