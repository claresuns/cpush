package cn.claresun.cpush.dns;

import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by claresun on 16-11-8.
 */
public class InetAddressWrapper {
    private InetAddress address;
    private int retryTimes;

    public InetAddressWrapper(InetAddress address) {
        this.address = address;
        this.retryTimes = 0;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public void incrRetryTimes() {
        this.retryTimes += 1;
    }

    public ArrayList<InetAddressWrapper> asWrapperList(InetAddress[] addresses) {
        if (addresses == null || addresses.length <= 0) {
            return null;
        }

        int size = addresses.length;
        ArrayList<InetAddressWrapper> addressList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            addressList.add(new InetAddressWrapper(addresses[i]));
        }

        return addressList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InetAddressWrapper that = (InetAddressWrapper) o;

        return address.getHostName().equals(that.address.getHostName());

    }

    @Override
    public int hashCode() {
        return address.getHostName().hashCode();
    }
}
