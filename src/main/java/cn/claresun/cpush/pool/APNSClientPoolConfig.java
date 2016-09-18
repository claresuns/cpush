package cn.claresun.cpush.pool;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Created by claresun on 16-9-7.
 */
public class APNSClientPoolConfig extends GenericObjectPoolConfig {
    public APNSClientPoolConfig(){
        setTestWhileIdle(true);
        setMinEvictableIdleTimeMillis(60000);
        setTimeBetweenEvictionRunsMillis(30000);
        setNumTestsPerEvictionRun(-1);
    }
}
