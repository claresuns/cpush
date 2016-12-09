package cn.claresun.cpush;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.junit.Assert;
import org.junit.Test;

import java.net.SocketAddress;
import java.util.zip.CRC32;

/**
 * Created by claresun on 16-12-8.
 */
public class TrafficTest {
    @Test
    public void testTrafficShaping() throws Exception {
        testTrafficShapingHandler(100, false);
        System.out.println("OK without ChannelTrafficShapingHandler");

        for (int i = 1; i <= 2; i++) {
            long sleep = i * 100;
            System.out.print("Test with " + sleep + "ms sleep..");
            testTrafficShapingHandler(sleep, true);
            System.out.println(" OK");
        }

    }

    public void testTrafficShapingHandler(long sleepWhenNotWritable, final boolean useTrafficShaping) throws Exception {

        final Crc32Sink sink = new Crc32Sink();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.channel(NioServerSocketChannel.class).group(new NioEventLoopGroup()).localAddress("localhost", 0);

        serverBootstrap.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel channel) throws Exception {
                if (useTrafficShaping) {
                    channel.pipeline().addLast(new ChannelTrafficShapingHandler(0, 128 * 1024));
                }
                channel.pipeline().addLast(sink);
            }
        });

        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.channel(NioSocketChannel.class).group(new NioEventLoopGroup());
        clientBootstrap.handler(new ChannelInboundHandlerAdapter());

        try {

            ChannelFuture cf = serverBootstrap.bind().sync();
            SocketAddress serverAddress = cf.channel().localAddress();

            System.out.println("Server address = " + serverAddress);

            Channel clientChannel = clientBootstrap.connect(serverAddress).sync().channel();

            byte[] data = new byte[512 * 1024];

            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (i % 31);
            }

            CRC32 crc32 = new CRC32();
            for (int i = 0; i < data.length; i++) {
                crc32.update(data[i]);
            }

            for (int i = 0; i < 256; i++) {
                ByteBuf slice = Unpooled.wrappedBuffer(data, 2048 * i, 2048);
                while (!clientChannel.isWritable()) {
                    Thread.sleep(sleepWhenNotWritable);
                }
                clientChannel.writeAndFlush(slice);
            }

            while (sink.getByteCount() < data.length) {
                Thread.sleep(100);
            }

            clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            sink.getChannel().closeFuture().sync();

            Assert.assertEquals(data.length, sink.getByteCount());
            Assert.assertEquals(crc32.getValue(), sink.getCrc32());

        } finally {
            clientBootstrap.group().shutdownGracefully();
            serverBootstrap.group().shutdownGracefully();
        }

    }

    private static class Crc32Sink extends ChannelInboundHandlerAdapter {

        private final CRC32 crc32 = new CRC32();

        private volatile long byteCount = 0;

        private Channel channel;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            channel = ctx.channel();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf buf = (ByteBuf) msg;
            byteCount += buf.readableBytes();
            for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {
                crc32.update(buf.readByte());
            }

            buf.release();
        }

        long getCrc32() {
            return crc32.getValue();
        }

        long getByteCount() {
            return byteCount;
        }

        Channel getChannel() {
            return channel;
        }

    }
}
