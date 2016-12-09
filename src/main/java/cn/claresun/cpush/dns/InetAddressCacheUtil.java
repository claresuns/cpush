package cn.claresun.cpush.dns;

import cn.claresun.cpush.util.IpParserUtil;

import javax.annotation.concurrent.GuardedBy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by claresun on 16-11-8.
 */
public class InetAddressCacheUtil {

    static void setInetAddressCache(String host, String[] ips, long expiration)
            throws NoSuchMethodException, UnknownHostException,
            IllegalAccessException, InstantiationException, InvocationTargetException,
            ClassNotFoundException, NoSuchFieldException {
        host = host.toLowerCase();
        Object entry = newCacheEntry(host, ips, expiration);

        synchronized (getAddressCacheFieldOfInetAddress()) {
            getCacheFiledOfAddressCacheFiledOfInetAddress().put(host, entry);
            getCacheFiledOfNegativeCacheFiledOfInetAddress().remove(host);
        }
    }

    @GuardedBy("getAddressCacheFieldOfInetAddress()")
    static Map<String, Object> getCacheFiledOfAddressCacheFiledOfInetAddress()
            throws NoSuchFieldException, IllegalAccessException {
        return getCacheFiledOfInetAddress$Cache0(getAddressCacheFieldOfInetAddress());
    }

    @GuardedBy("getAddressCacheFieldOfInetAddress()")
    static Map<String, Object> getCacheFiledOfNegativeCacheFiledOfInetAddress()
            throws NoSuchFieldException, IllegalAccessException {
        return getCacheFiledOfInetAddress$Cache0(getNegativeCacheFieldOfInetAddress());
    }

    static Object getNegativeCacheFieldOfInetAddress()
            throws NoSuchFieldException, IllegalAccessException {
        return getAddressCacheFieldsOfInetAddress0()[1];
    }

    static DnsCache listInetAddressCache()
            throws NoSuchFieldException, IllegalAccessException {

        final Map<String, Object> cache;
        final Map<String, Object> negativeCache;
        synchronized (getAddressCacheFieldOfInetAddress()) {
            cache = new HashMap<String, Object>(getCacheFiledOfAddressCacheFiledOfInetAddress());
            negativeCache = new HashMap<String, Object>(getCacheFiledOfNegativeCacheFiledOfInetAddress());
        }

        List<DnsCacheEntry> retCache = new ArrayList<DnsCacheEntry>();
        for (Map.Entry<String, Object> entry : cache.entrySet()) {
            final String host = entry.getKey();

            if (isDnsCacheEntryExpired(host)) { // exclude expired entries!
                continue;
            }
            retCache.add(inetAddress$CacheEntry2DnsCacheEntry(host, entry.getValue()));
        }
        List<DnsCacheEntry> retNegativeCache = new ArrayList<DnsCacheEntry>();
        for (Map.Entry<String, Object> entry : negativeCache.entrySet()) {
            final String host = entry.getKey();
            retNegativeCache.add(inetAddress$CacheEntry2DnsCacheEntry(host, entry.getValue()));
        }
        return new DnsCache(retCache, retNegativeCache);
    }

    static boolean isDnsCacheEntryExpired(String host) {
        return null == host || "0.0.0.0".equals(host);
    }

    static volatile Field expirationFieldOfInetAddress$CacheEntry = null;
    static volatile Field addressesFieldOfInetAddress$CacheEntry = null;

    static DnsCacheEntry inetAddress$CacheEntry2DnsCacheEntry(String host, Object entry)
            throws NoSuchFieldException, IllegalAccessException {
        if (expirationFieldOfInetAddress$CacheEntry == null || addressesFieldOfInetAddress$CacheEntry == null) {
            synchronized (InetAddressCacheUtil.class) {
                if (expirationFieldOfInetAddress$CacheEntry == null) {
                    Class<?> cacheEntryClass = entry.getClass();
                    // InetAddress.CacheEntry has 2 filed:
                    // - for jdk 6, address and expiration
                    // - for jdk 7+, addresses(*renamed* from 6!) and expiration
                    // code in jdk 6:
                    //   http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b27/java/net/InetAddress.java#InetAddress.CacheEntry
                    // code in jdk 7:
                    //   http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/java/net/InetAddress.java#InetAddress.CacheEntry
                    // code in jdk 8:
                    //   http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8-b132/java/net/InetAddress.java#InetAddress.CacheEntry
                    final Field[] fields = cacheEntryClass.getDeclaredFields();
                    for (Field field : fields) {
                        final String name = field.getName();
                        if (name.equals("expiration")) {
                            field.setAccessible(true);
                            expirationFieldOfInetAddress$CacheEntry = field;
                        } else if (name.startsWith("address")) { // use startWith so works for jdk 6 and jdk 7+
                            field.setAccessible(true);
                            addressesFieldOfInetAddress$CacheEntry = field;
                        } else {
                            throw new IllegalStateException("JDK add new Field " + name +
                                    " for class InetAddress.CacheEntry, report bug for dns-cache-manipulator lib!");
                        }
                    }
                }
            }
        }

        long expiration = (Long) expirationFieldOfInetAddress$CacheEntry.get(entry);
        InetAddress[] addresses = (InetAddress[]) addressesFieldOfInetAddress$CacheEntry.get(entry);

        String[] ips = new String[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            ips[i] = addresses[i].getHostAddress();
        }
        return new DnsCacheEntry(host, ips, new Date(expiration));
    }

    static volatile Object[] ADDRESS_CACHE_AND_NEGATIVE_CACHE = null;

    static Object[] getAddressCacheFieldsOfInetAddress0()
            throws NoSuchFieldException, IllegalAccessException {
        if (ADDRESS_CACHE_AND_NEGATIVE_CACHE == null) {
            synchronized (InetAddressCacheUtil.class) {
                if (ADDRESS_CACHE_AND_NEGATIVE_CACHE == null) {
                    final Field cacheField = InetAddress.class.getDeclaredField("addressCache");
                    cacheField.setAccessible(true);

                    final Field negativeCacheField = InetAddress.class.getDeclaredField("negativeCache");
                    negativeCacheField.setAccessible(true);

                    ADDRESS_CACHE_AND_NEGATIVE_CACHE = new Object[]{
                            cacheField.get(InetAddress.class),
                            negativeCacheField.get(InetAddress.class)
                    };
                }
            }
        }
        return ADDRESS_CACHE_AND_NEGATIVE_CACHE;
    }

    static Map<String, Object> getCacheFiledOfInetAddress$Cache0(Object inetAddressCache)
            throws NoSuchFieldException, IllegalAccessException {
        Class clazz = inetAddressCache.getClass();

        final Field cacheMapField = clazz.getDeclaredField("cache");
        cacheMapField.setAccessible(true);
        return (Map<String, Object>) cacheMapField.get(inetAddressCache);
    }

    static Object getAddressCacheFieldOfInetAddress()
            throws NoSuchFieldException, IllegalAccessException {
        return getAddressCacheFieldsOfInetAddress0()[0];
    }

    static Object newCacheEntry(String host, String[] ips, long expiration)
            throws UnknownHostException, ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        String className = "java.net.InetAddress$CacheEntry";
        Class<?> clazz = Class.forName(className);

        // InetAddress.CacheEntry has only a constructor:
        // - for jdk 6, constructor signature is CacheEntry(Object address, long expiration)
        // - for jdk 7+, constructor signature is CacheEntry(InetAddress[] addresses, long expiration)
        // code in jdk 6:
        //   http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b27/java/net/InetAddress.java#InetAddress.CacheEntry
        // code in jdk 7:
        //   http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/7-b147/java/net/InetAddress.java#InetAddress.CacheEntry
        // code in jdk 8:
        //   http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8-b132/java/net/InetAddress.java#InetAddress.CacheEntry
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance(toInetAddressArray(host, ips), expiration);
    }

    static InetAddress[] toInetAddressArray(String host, String[] ips) throws UnknownHostException {
        InetAddress[] addresses = new InetAddress[ips.length];
        for (int i = 0; i < addresses.length; i++) {
            addresses[i] = InetAddress.getByAddress(host, IpParserUtil.ip2ByteArray(ips[i]));
        }

        return addresses;
    }
}
