package cn.claresun.cpush.dns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by claresun on 16-11-8.
 */
public class DnsCache implements Serializable {
    private static final long serialVersionUID = -8875846792723609243L;

    private final List<DnsCacheEntry> cache;
    private final List<DnsCacheEntry> negativeCache;

    public DnsCache(List<DnsCacheEntry> cache, List<DnsCacheEntry> negativeCache) {
        this.cache = cache;
        this.negativeCache = negativeCache;
    }

    public List<DnsCacheEntry> getCache() {
        // defensive copy
        return new ArrayList<DnsCacheEntry>(cache);
    }

    public List<DnsCacheEntry> getNegativeCache() {
        // defensive copy
        return new ArrayList<DnsCacheEntry>(negativeCache);
    }

    @Override
    public String toString() {
        return "DnsCache{" +
                "cache=" + cache +
                ", negativeCache=" + negativeCache +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DnsCache dnsCache = (DnsCache) o;

        if (cache != null ? !cache.equals(dnsCache.cache) : dnsCache.cache != null)
            return false;
        return !(negativeCache != null ? !negativeCache.equals(dnsCache.negativeCache) : dnsCache.negativeCache != null);
    }

    @Override
    public int hashCode() {
        int result = cache != null ? cache.hashCode() : 0;
        result = 31 * result + (negativeCache != null ? negativeCache.hashCode() : 0);
        return result;
    }
}
