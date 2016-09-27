package cn.claresun.cpush;

import cn.claresun.cpush.exception.NotConnectedException;
import cn.claresun.cpush.model.APNSNotification;
import cn.claresun.cpush.model.APNSNotificationResponse;
import cn.claresun.cpush.util.Constant;
import cn.claresun.cpush.util.SslUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by claresun on 16-8-11.
 */
public class APNSClient {
    private static final Logger log = LoggerFactory.getLogger(APNSClient.class);

    private final Bootstrap bootstrap;
    private final boolean shouldShutDownEventLoopGroup;

    private long writeTimeoutMillis = Constant.DEFAULT_WRITE_TIMEOUT_MILLIS;

    private Long gracefulShutdownTimeoutMillis;

    private volatile ChannelPromise connectionReadyPromise;
    private volatile ChannelPromise reconnectionPromise;
    private long reconnectDelaySeconds = Constant.INITIAL_RECONNECT_DELAY_SECONDS;
    private final int connectTimeOut = Constant.CONNECT_TIMEOUT_MILLIS;

    private OnDataReceived onDataReceived;

    public void onDataReceived(final OnDataReceived onDataReceived) {
        this.onDataReceived = onDataReceived;
    }

    public APNSClient(final File p12File, final String password) throws IOException {
        this(p12File, password, Constant.DEVELOPMENT_APNS_HOST, Constant.DEFAULT_APNS_PORT);
    }

    public APNSClient(final File p12File, final String password, final String host, final int port) throws IOException {
        this(SslUtil.getSslContextWithP12File(p12File, password), new InetSocketAddress(host, port), null);
    }

    protected APNSClient(final SslContext sslContext, final InetSocketAddress address, final EventLoopGroup eventLoopGroup) {
        this.bootstrap = new Bootstrap();

        if (eventLoopGroup != null) {
            this.bootstrap.group(eventLoopGroup);
            this.shouldShutDownEventLoopGroup = false;
        } else {
            this.bootstrap.group(new NioEventLoopGroup(1));
            this.shouldShutDownEventLoopGroup = true;
        }

        this.bootstrap.channel(NioSocketChannel.class);
        this.bootstrap.option(ChannelOption.TCP_NODELAY, true).
                option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).
                option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeOut);
        this.bootstrap.remoteAddress(address);
        this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(final SocketChannel channel) throws Exception {
                final ChannelPipeline pipeline = channel.pipeline();

                if (APNSClient.this.writeTimeoutMillis > 0) {
                    pipeline.addLast(new WriteTimeoutHandler(APNSClient.this.writeTimeoutMillis, TimeUnit.MILLISECONDS));
                }

                pipeline.addLast(sslContext.newHandler(channel.alloc()));
                pipeline.addLast(new ApplicationProtocolNegotiationHandler("") {
                    @Override
                    protected void configurePipeline(final ChannelHandlerContext context, final String protocol) {
                        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                            final APNSClientHandler apnsClientHandler = new APNSClientHandler.APNSClientHandlerBuilder()
                                    .server(false)
                                    .authority(((InetSocketAddress) context.channel().remoteAddress()).getHostName())
                                    .maxUnflushedNotifications(Constant.DEFAULT_MAX_UNFLUSHED_NOTIFICATIONS)
                                    .onDataReceived(APNSClient.this.onDataReceived)
                                    .encoderEnforceMaxConcurrentStreams(true)
                                    .build();

                            synchronized (APNSClient.this.bootstrap) {
                                if (APNSClient.this.gracefulShutdownTimeoutMillis != null) {
                                    apnsClientHandler.gracefulShutdownTimeoutMillis(APNSClient.this.gracefulShutdownTimeoutMillis);
                                }
                            }

                            context.pipeline().addLast(new IdleStateHandler(0, Constant.DEFAULT_FLUSH_AFTER_IDLE_MILLIS, Constant.PING_IDLE_TIME_MILLIS, TimeUnit.MILLISECONDS));
                            context.pipeline().addLast(apnsClientHandler);

                            context.channel().eventLoop().submit(new Runnable() {

                                @Override
                                public void run() {
                                    final ChannelPromise connectionReadyPromise = APNSClient.this.connectionReadyPromise;

                                    if (connectionReadyPromise != null) {
                                        connectionReadyPromise.trySuccess();
                                    }
                                }
                            });

                        } else {
                            log.error("Unexpected protocol: {}", protocol);
                            context.close();
                        }
                    }

                    @Override
                    protected void handshakeFailure(final ChannelHandlerContext context, final Throwable cause) throws Exception {
                        final ChannelPromise connectionReadyPromise = APNSClient.this.connectionReadyPromise;

                        if (connectionReadyPromise != null) {
                            connectionReadyPromise.tryFailure(cause);
                        }

                        super.handshakeFailure(context, cause);
                    }
                });
            }
        });
    }

    public void setGracefulShutdownTimeout(final long timeoutMillis) {
        synchronized (this.bootstrap) {
            this.gracefulShutdownTimeoutMillis = timeoutMillis;

            if (this.connectionReadyPromise != null) {
                @SuppressWarnings("rawtypes")
                final APNSClientHandler handler = this.connectionReadyPromise.channel().pipeline().get(APNSClientHandler.class);

                if (handler != null) {
                    handler.gracefulShutdownTimeoutMillis(timeoutMillis);
                }
            }
        }
    }

    public Future<Void> connect() throws NotConnectedException, InterruptedException {
        final Future<Void> connectionReadyFuture;

        if (this.bootstrap.config().group().isShuttingDown() || this.bootstrap.config().group().isShutdown()) {
            throw new NotConnectedException("Client's bootstrap is shutdown.");
        } else {
            synchronized (this.bootstrap) {

                if (this.connectionReadyPromise == null) {

                    final ChannelFuture connectFuture = this.bootstrap.connect();
                    this.connectionReadyPromise = connectFuture.channel().newPromise();

                    connectFuture.channel().closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {

                        @Override
                        public void operationComplete(final ChannelFuture future) throws Exception {
                            synchronized (APNSClient.this.bootstrap) {
                                if (APNSClient.this.connectionReadyPromise != null) {
                                    APNSClient.this.connectionReadyPromise.tryFailure(
                                            new IllegalStateException("Channel closed before HTTP/2 preface completed."));

                                    APNSClient.this.connectionReadyPromise = null;
                                }

                                if (APNSClient.this.reconnectionPromise != null) {
                                    log.debug("Disconnected. Next automatic reconnection attempt in {} seconds.", APNSClient.this.reconnectDelaySeconds);

                                    future.channel().eventLoop().schedule(new Runnable() {

                                        @Override
                                        public void run() {
                                            log.debug("Attempting to reconnect.");
                                            try {
                                                APNSClient.this.connect();
                                            } catch (Exception e) {
                                                log.error(e.toString());
                                            }
                                        }
                                    }, APNSClient.this.reconnectDelaySeconds, TimeUnit.SECONDS);

                                    APNSClient.this.reconnectDelaySeconds = Math.min(APNSClient.this.reconnectDelaySeconds, Constant.MAX_RECONNECT_DELAY_SECONDS);
                                }
                            }
                        }
                    });

                    this.connectionReadyPromise.addListener(new GenericFutureListener<ChannelFuture>() {

                        @Override
                        public void operationComplete(final ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                synchronized (APNSClient.this.bootstrap) {
                                    if (APNSClient.this.reconnectionPromise != null) {
                                        log.info("Connection to {} restored.", future.channel().remoteAddress());
                                        APNSClient.this.reconnectionPromise.trySuccess();
                                    } else {
                                        log.info("Connected to {}.", future.channel().remoteAddress());
                                    }

                                    APNSClient.this.reconnectDelaySeconds = Constant.INITIAL_RECONNECT_DELAY_SECONDS;
                                    APNSClient.this.reconnectionPromise = future.channel().newPromise();
                                }
                            } else {
                                log.info("Failed to connect.", future.cause());
                            }
                        }
                    });
                }

                connectionReadyFuture = this.connectionReadyPromise;
            }
        }

        return connectionReadyFuture;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Future<Void> disconnect() throws InterruptedException {
        log.info("Disconnecting.");
        final Future<Void> disconnectFuture;

        synchronized (this.bootstrap) {
            this.reconnectionPromise = null;

            final Future<Void> channelCloseFuture;

            if (this.connectionReadyPromise != null) {
                channelCloseFuture = this.connectionReadyPromise.channel().close();
            } else {
                channelCloseFuture = new SucceededFuture<>(GlobalEventExecutor.INSTANCE, null);
            }

            if (this.shouldShutDownEventLoopGroup) {
                channelCloseFuture.addListener(new GenericFutureListener<Future<Void>>() {

                    @Override
                    public void operationComplete(final Future<Void> future) throws Exception {
                        APNSClient.this.bootstrap.config().group().shutdownGracefully();
                    }
                });

                disconnectFuture = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

                this.bootstrap.config().group().terminationFuture().addListener(new GenericFutureListener() {

                    @Override
                    public void operationComplete(final Future future) throws Exception {
                        ((DefaultPromise<Void>) disconnectFuture).trySuccess(null);
                    }
                });
            } else {
                disconnectFuture = channelCloseFuture;
            }
        }

        return disconnectFuture;
    }

    public void sendAsynchronous(final APNSNotification notification, final Callback callback) throws NotConnectedException {

        final ChannelPromise connectionReadyPromise = this.connectionReadyPromise;

        if (connectionReadyPromise == null) {
            throw new NotConnectedException("Client is not ready.");
        }

        if (!connectionReadyPromise.isSuccess() || !connectionReadyPromise.channel().isActive()) {
            this.connectionReadyPromise.channel().close();
            throw new NotConnectedException("Client is not ready.");
        }

        connectionReadyPromise.channel().write(notification).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    log.debug("Failed to write push notification: {}", notification, future.cause());
                    callback.onFailure(new APNSNotificationResponse(
                            notification, false, future.cause() != null ? future.cause().toString() : future.cause().toString(), null
                    ));
                }
            }
        });

    }

    public boolean isConnected() {
        final ChannelPromise connectionReadyPromise = this.connectionReadyPromise;
        return (connectionReadyPromise != null && connectionReadyPromise.isSuccess());
    }

    /*public void sendAsynchronous(final List<APNSNotification> notifications, final Callback callback) throws NotConnectedException {
        final int listSize = notifications.size();

        final ChannelPromise connectionReadyPromise = this.connectionReadyPromise;
        if (connectionReadyPromise != null && connectionReadyPromise.isSuccess() && connectionReadyPromise.channel().isActive()) {
            GenericFutureListener<ChannelFuture> futureListener = new MutliNotificatinsFutureListener(connectionReadyPromise.channel(), listSize, callback);

            for (APNSNotification notification : notifications) {
                connectionReadyPromise.channel().write(notification).addListener(futureListener);
            }
        } else {
            callback.onFailure(null);
        }
    }

    private class MutliNotificatinsFutureListener implements GenericFutureListener<ChannelFuture> {
        private int completeCount;
        private int expertCount;
        private Channel channel;
        private Callback callback;

        public MutliNotificatinsFutureListener(Channel channel, int expertCount, Callback callback) {
            this(channel, expertCount, 1, callback);
        }

        public MutliNotificatinsFutureListener(Channel channel, int expertCount, int completeCount, Callback callback) {
            this.channel = channel;
            this.expertCount = expertCount;
            this.expertCount = expertCount;
            this.callback = callback;
        }


        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            synchronized (this) {

                if (completeCount >= expertCount) {

                    if (future.isSuccess()) {
                        this.callback.onSuccess(null);
                    } else {
                        log.debug("Failed to write push notifications ", future.cause());
                        this.callback.onFailure(null);
                    }

                    completeCount = 1;
                }

                completeCount++;
            }
        }
    }*/
}
