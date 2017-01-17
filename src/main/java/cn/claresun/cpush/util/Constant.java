package cn.claresun.cpush.util;

import io.netty.util.AsciiString;

/**
 * Created by claresun on 16-8-15.
 */
public class Constant {
    public static final String PRODUCTION_APNS_HOST = "api.push.apple.com";
    public static final String DEVELOPMENT_APNS_HOST = "api.development.push.apple.com";
    public static final int DEFAULT_APNS_PORT = 443;

    public static final String APNS_PATH_PREFIX = "/3/device/";
    public static final AsciiString APNS_EXPIRATION_HEADER = new AsciiString("apns-expiration");
    public static final AsciiString APNS_TOPIC_HEADER = new AsciiString("apns-topic");
    public static final AsciiString APNS_PRIORITY_HEADER = new AsciiString("apns-priority");

    public static final int INITIAL_PAYLOAD_BUFFER_CAPACITY = 4096;
    public static final long STREAM_ID_RESET_THRESHOLD = Integer.MAX_VALUE - 1;

    public static final long DEFAULT_FLUSH_AFTER_IDLE_MILLIS = 250; //millis second

    public static final int DEFAULT_MAX_UNFLUSHED_NOTIFICATIONS = 32;

    public static final long DEFAULT_WRITE_TIMEOUT_MILLIS = 10000;  // millis second

    public static final int PING_TIME_OUT = 60; // second
    public static final int PING_IDLE_TIME_MILLIS = 20000; // millis second

    public static final long INITIAL_RECONNECT_DELAY_SECONDS = 3; // second

    public static final int CONNECT_TIMEOUT_MILLIS = 180000;

}
