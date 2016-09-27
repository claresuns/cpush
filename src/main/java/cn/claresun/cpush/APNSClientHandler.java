package cn.claresun.cpush;

import cn.claresun.cpush.model.APNSNotification;
import cn.claresun.cpush.model.APNSNotificationResponse;
import cn.claresun.cpush.model.ErrorResponse;
import cn.claresun.cpush.util.Constant;
import com.google.gson.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.PromiseCombiner;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by claresun on 16-8-15.
 */
public class APNSClientHandler extends Http2ConnectionHandler {
    private static final Logger log = LoggerFactory.getLogger(APNSClientHandler.class);

    private final AtomicBoolean receivedInitialSettings = new AtomicBoolean(false);
    private long nextStreamId = 1;
    private final String authority;

    private long nextPingId = new Random().nextLong();
    private ScheduledFuture<?> pingTimeoutFuture;

    private final int maxUnflushedNotifications;
    private int unflushedNotifications = 0;

    private OnDataReceived onDataReceived;

    private final Map<Integer, APNSNotification> pushNotificationsByStreamId = new HashMap<>();
    private final Map<Integer, Http2Headers> headersByStreamId = new HashMap<>();

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateAsMillisecondsSinceEpochTypeAdapter())
            .create();

    protected APNSClientHandler(Http2ConnectionDecoder decoder, Http2ConnectionEncoder encoder, Http2Settings initialSettings, final String authority, final int maxUnflushedNotifications, OnDataReceived onDataReceived) {
        super(decoder, encoder, initialSettings);
        this.authority = authority;
        this.maxUnflushedNotifications = maxUnflushedNotifications;
        this.onDataReceived = onDataReceived;

    }

    public static class APNSClientHandlerBuilder extends AbstractHttp2ConnectionHandlerBuilder<APNSClientHandler, APNSClientHandlerBuilder> {

        private String authority;
        private int maxUnflushedNotifications = 0;
        private OnDataReceived onDataReceived;

        public APNSClientHandlerBuilder authority(final String authority) {
            this.authority = authority;
            return this;
        }

        public String authority() {
            return this.authority;
        }

        public APNSClientHandlerBuilder maxUnflushedNotifications(final int maxUnflushedNotifications) {
            this.maxUnflushedNotifications = maxUnflushedNotifications;
            return this;
        }

        public int maxUnflushedNotifications() {
            return this.maxUnflushedNotifications;
        }

        public OnDataReceived onDataReceived() {
            return this.onDataReceived;
        }

        public APNSClientHandlerBuilder onDataReceived(OnDataReceived onDataReceived) {
            this.onDataReceived = onDataReceived;
            return this;
        }

        @Override
        public APNSClientHandlerBuilder server(final boolean isServer) {

            return super.server(false);
        }

        @Override
        public APNSClientHandlerBuilder encoderEnforceMaxConcurrentStreams(final boolean enforceMaxConcurrentStreams) {
            return super.encoderEnforceMaxConcurrentStreams(enforceMaxConcurrentStreams);
        }

        @Override
        public APNSClientHandler build(final Http2ConnectionDecoder decoder, final Http2ConnectionEncoder encoder, final Http2Settings initialSettings) {
            Objects.requireNonNull(this.authority(), "Authority must be set before building an ApnsClientHandler.");

            final APNSClientHandler handler = new APNSClientHandler(decoder, encoder, initialSettings, this.authority(), this.maxUnflushedNotifications(), this.onDataReceived());
            this.frameListener(handler.new APNSClientHandlerFrameAdapter());
            return handler;
        }

        @Override
        public APNSClientHandler build() {
            return super.build();
        }


    }

    private class APNSClientHandlerFrameAdapter extends Http2FrameAdapter {
        @Override
        public void onSettingsRead(final ChannelHandlerContext context, final Http2Settings settings) {
            log.trace("Received settings from APNs gateway: {}", settings);
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public int onDataRead(final ChannelHandlerContext context, final int streamId, final ByteBuf data, final int padding, final boolean endOfStream) throws Http2Exception {
            log.trace("Received data from APNs gateway on stream {}: {}", streamId, data.toString(StandardCharsets.UTF_8));

            final int bytesProcessed = data.readableBytes() + padding;

            if (endOfStream) {
                final Http2Headers headers = APNSClientHandler.this.headersByStreamId.remove(streamId);
                final APNSNotification pushNotification = APNSClientHandler.this.pushNotificationsByStreamId.remove(streamId);

                final boolean success = HttpResponseStatus.OK.equals(HttpResponseStatus.parseLine(headers.status()));
                final ErrorResponse errorResponse = gson.fromJson(data.toString(StandardCharsets.UTF_8), ErrorResponse.class);

                APNSClientHandler.this.onDataReceived.received(new APNSNotificationResponse(
                        pushNotification, success, errorResponse.getReason(), errorResponse.getTimestamp()));
            } else {
                log.error("Gateway sent a DATA frame that was not the end of a stream.");
            }

            return bytesProcessed;
        }

        @Override
        public void onHeadersRead(final ChannelHandlerContext context, final int streamId, final Http2Headers headers, final int streamDependency, final short weight, final boolean exclusive, final int padding, final boolean endOfStream) throws Http2Exception {
            this.onHeadersRead(context, streamId, headers, padding, endOfStream);
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void onHeadersRead(final ChannelHandlerContext context, final int streamId, final Http2Headers headers, final int padding, final boolean endOfStream) throws Http2Exception {
            log.trace("Received headers from APNs gateway on stream {}: {}", streamId, headers);

            final boolean success = HttpResponseStatus.OK.equals(HttpResponseStatus.parseLine(headers.status()));

            if (endOfStream) {
                if (!success) {
                    log.error("Gateway sent an end-of-stream HEADERS frame for an unsuccessful notification.");
                }

            } else {
                APNSClientHandler.this.headersByStreamId.put(streamId, headers);
            }
        }

        @Override
        public void onPingAckRead(final ChannelHandlerContext context, final ByteBuf data) {
            if (APNSClientHandler.this.pingTimeoutFuture != null) {
                log.trace("Received reply to ping.");
                APNSClientHandler.this.pingTimeoutFuture.cancel(false);
            } else {
                log.error("Received PING ACK, but no corresponding outbound PING found.");
            }
        }

        @Override
        public void onGoAwayRead(final ChannelHandlerContext context, final int lastStreamId, final long errorCode, final ByteBuf debugData) throws Http2Exception {
            log.info("code: {} Received GOAWAY from APNs server: {}", errorCode, debugData.toString(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void write(final ChannelHandlerContext context, final Object message, final ChannelPromise writePromise) throws Http2Exception {
        try {
            // We'll catch class cast issues gracefully
            final APNSNotification pushNotification = (APNSNotification) message;

            final int streamId = (int) this.nextStreamId;

            final Http2Headers headers = new DefaultHttp2Headers()
                    .method(HttpMethod.POST.asciiName())
                    .authority(this.authority)
                    .path(Constant.APNS_PATH_PREFIX + pushNotification.getToken())
                    .addInt(Constant.APNS_EXPIRATION_HEADER, pushNotification.getInvalidationTime() == null ? 0 : (int) (pushNotification.getInvalidationTime().getTime() / 1000));

            if (pushNotification.getPriority() != null) {
                headers.addInt(Constant.APNS_PRIORITY_HEADER, pushNotification.getPriority().getCode());
            }

            if (pushNotification.getTopic() != null) {
                headers.add(Constant.APNS_TOPIC_HEADER, pushNotification.getTopic());
            }

            final ChannelPromise headersPromise = context.newPromise();
            this.encoder().writeHeaders(context, streamId, headers, 0, false, headersPromise);
            log.trace("Wrote headers on stream {}: {}", streamId, headers);

            final ByteBuf payloadBuffer = context.alloc().ioBuffer(Constant.INITIAL_PAYLOAD_BUFFER_CAPACITY);
            payloadBuffer.writeBytes(pushNotification.getPayload().getBytes(StandardCharsets.UTF_8));

            final ChannelPromise dataPromise = context.newPromise();
            this.encoder().writeData(context, streamId, payloadBuffer, 0, true, dataPromise);
            log.trace("Wrote payload on stream {}: {}", streamId, pushNotification.getPayload());

            final PromiseCombiner promiseCombiner = new PromiseCombiner();
            promiseCombiner.addAll(headersPromise, dataPromise);
            promiseCombiner.finish(writePromise);

            if (writePromise.isDone()) {
                notifyWriteDone(writePromise, streamId, pushNotification);
            } else {
                writePromise.addListener(new GenericFutureListener<ChannelPromise>() {
                    @Override
                    public void operationComplete(final ChannelPromise future) throws Exception {
                        notifyWriteDone(writePromise, streamId, pushNotification);
                    }
                });
            }

            this.nextStreamId += 2;

            if (++this.unflushedNotifications >= this.maxUnflushedNotifications) {
                this.flush(context);
            }

            if (this.nextStreamId >= Constant.STREAM_ID_RESET_THRESHOLD) {
                context.close();
            }

        } catch (final ClassCastException e) {
            // This should never happen, but in case some foreign debris winds up in the pipeline, just pass it through.
            log.error("Unexpected object in pipeline: {}", message);
            context.write(message, writePromise);
        }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext context, final Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            final IdleStateEvent idleStateEvent = (IdleStateEvent) event;

            if (IdleState.WRITER_IDLE.equals(idleStateEvent.state())) {
                if (this.unflushedNotifications > 0) {
                    this.flush(context);
                }
            } else {

                log.trace("Sending ping due to inactivity.");

                final ByteBuf pingDataBuffer = context.alloc().ioBuffer(8, 8);
                pingDataBuffer.writeLong(this.nextPingId++);

                this.encoder().writePing(context, false, pingDataBuffer, context.newPromise()).addListener(new GenericFutureListener<ChannelFuture>() {

                    @Override
                    public void operationComplete(final ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            APNSClientHandler.this.pingTimeoutFuture = future.channel().eventLoop().schedule(new Runnable() {

                                @Override
                                public void run() {
                                    log.debug("Closing channel due to ping timeout.");
                                    future.channel().close();
                                }
                            }, Constant.PING_TIME_OUT, TimeUnit.SECONDS);
                        } else {
                            log.debug("Failed to write PING frame.", future.cause());
                            future.channel().close();
                        }
                    }
                });

                this.flush(context);
            }
        }

        super.userEventTriggered(context, event);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext context, final Throwable cause) throws Exception {
        if (cause instanceof WriteTimeoutException) {
            log.debug("Closing connection due to write timeout.");
            context.close();
        } else {
            log.warn("APNs client pipeline caught an exception.", cause);
        }
    }

    private void notifyWriteDone(final ChannelFuture future, final int streamId, final APNSNotification pushNotification) {
        if (future.isSuccess()) {
            this.pushNotificationsByStreamId.put(streamId, pushNotification);
        } else {
            log.trace("Failed to write push notification on stream {}.", streamId, future.cause());
        }
    }

    private static class DateAsMillisecondsSinceEpochTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

        @Override
        public Date deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
            final Date date;

            if (json.isJsonPrimitive()) {
                date = new Date(json.getAsLong());
            } else if (json.isJsonNull()) {
                date = null;
            } else {
                throw new JsonParseException("Dates represented as seconds since the epoch must either be numbers or null.");
            }

            return date;
        }

        @Override
        public JsonElement serialize(final Date src, final Type typeOfSrc, final JsonSerializationContext context) {
            final JsonElement element;

            if (src != null) {
                element = new JsonPrimitive(src.getTime());
            } else {
                element = JsonNull.INSTANCE;
            }

            return element;
        }
    }
}
