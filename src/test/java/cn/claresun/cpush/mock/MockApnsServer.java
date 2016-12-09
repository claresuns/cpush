package cn.claresun.cpush.mock;

import cn.claresun.cpush.util.SSLUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.codec.http2.Http2Settings;
import io.netty.handler.ssl.*;

import javax.net.ssl.SSLSession;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by claresun on 16-12-8.
 */
public class MockApnsServer {
    private final ServerBootstrap bootstrap;

    final Map<String, Map<String, Date>> tokenExpirationsByTopic = new HashMap<>();

    private ChannelGroup allChannels;

    private static final String CA_CERTIFICATE_FILENAME = "/ca.pem";
    private static final String SERVER_KEYSTORE = "/server.p12";
    private static final String SERVER_KEYSTORE_PASSWORD = "pushy-test";

    public MockApnsServer(final EventLoopGroup eventLoopGroup) {
        final SslContext sslContext;
        try (final InputStream caInputStream = this.getClass().getResourceAsStream(CA_CERTIFICATE_FILENAME)) {
            final KeyStore.PrivateKeyEntry privateKeyEntry = SSLUtil.getFirstPrivateKeyEntryFromP12InputStream(
                    MockApnsServer.class.getResourceAsStream(SERVER_KEYSTORE), SERVER_KEYSTORE_PASSWORD);

            sslContext = SslContextBuilder.forServer(privateKeyEntry.getPrivateKey(), (X509Certificate) privateKeyEntry.getCertificate())
                    .sslProvider(OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK)
                    .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                    .trustManager(caInputStream)
                    .clientAuth(ClientAuth.REQUIRE)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2))
                    .build();
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create SSL context for mock APNs server.", e);
        }

        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(eventLoopGroup);
        this.bootstrap.channel(NioServerSocketChannel.class);
        this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(final SocketChannel channel) throws Exception {
                final
                SslHandler sslHandler = sslContext.newHandler(channel.alloc());
                channel.pipeline().addLast(sslHandler);
                channel.pipeline().addLast(new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {

                    @Override
                    protected void configurePipeline(final ChannelHandlerContext context, final String protocol) throws Exception {
                        if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
                            final Set<String> topics = new HashSet<>();
                            {
                                final SSLSession sslSession = sslHandler.engine().getSession();

                                for (final Certificate certificate : sslSession.getPeerCertificates()) {
                                    topics.addAll(TopicUtil.extractApnsTopicsFromCertificate(certificate));
                                }
                            }

                            context.pipeline().addLast(new MockApnsServerHandler.MockApnsServerHandlerBuilder()
                                    .apnsServer(MockApnsServer.this)
                                    .topics(topics)
                                    .initialSettings(new Http2Settings().maxConcurrentStreams(8))
                                    .build());

                            MockApnsServer.this.allChannels.add(context.channel());
                        } else {
                            throw new IllegalStateException("Unexpected protocol: " + protocol);
                        }
                    }
                });
            }
        });
    }

    public ChannelFuture start(final int port) {
        final ChannelFuture channelFuture = this.bootstrap.bind(port);

        this.allChannels = new DefaultChannelGroup(channelFuture.channel().eventLoop(), true);
        this.allChannels.add(channelFuture.channel());

        return channelFuture;
    }

    public void registerToken(final String topic, final String token) {
        this.registerToken(topic, token, null);
    }

    public void registerToken(final String topic, final String token, final Date expiration) {
        Objects.requireNonNull(topic);
        Objects.requireNonNull(token);

        if (!this.tokenExpirationsByTopic.containsKey(topic)) {
            this.tokenExpirationsByTopic.put(topic, new HashMap<String, Date>());
        }

        this.tokenExpirationsByTopic.get(topic).put(token, expiration);
    }

    protected boolean isTokenRegisteredForTopic(final String token, final String topic) {
        final Map<String, Date> tokensWithinTopic = this.tokenExpirationsByTopic.get(topic);

        return tokensWithinTopic != null && tokensWithinTopic.containsKey(token);
    }

    protected Date getExpirationTimestampForTokenInTopic(final String token, final String topic) {
        final Map<String, Date> tokensWithinTopic = this.tokenExpirationsByTopic.get(topic);

        return tokensWithinTopic != null ? tokensWithinTopic.get(token) : null;
    }

    public ChannelGroupFuture shutdown() {
        return this.allChannels.close();
    }
}
