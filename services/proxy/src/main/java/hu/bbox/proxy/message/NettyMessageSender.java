package hu.bbox.proxy.message;

import hu.bbox.messaging.message.Message;
import hu.bbox.messaging.parser.MessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Message sender which uses {@link NettyInbound} and {@link NettyOutbound} objects to implement an async communication
 * Uses the tracking id in {@link hu.bbox.messaging.message.Envelope} to synchronize the returned future objects
 * with the received response.
 *
 * @param <T> request type
 * @param <U> response type
 */
public class NettyMessageSender<T extends Message<?>, U extends Message<?>> implements AsyncMessageSender<T, U> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyMessageSender.class);
    private final Map<String, CompletableFuture<U>> futureResponses = new ConcurrentHashMap<>();
    private final NettyOutbound outbound;
    private final NettyInbound inbound;
    private final MessageSerializer<U> inboundParser;
    private final MessageSerializer<T> outboundParser;

    public NettyMessageSender(
            NettyInbound inbound,
            NettyOutbound outbound,
            MessageSerializer<U> inboundParser,
            MessageSerializer<T> outboundParser) {
        this.outbound = outbound;
        this.inbound = inbound;
        this.inboundParser = inboundParser;
        this.outboundParser = outboundParser;
        setUpInbound();
    }

    private void setUpInbound() {
        inbound
                .receive()
                .asByteArray()
                .map(inboundParser::deserialize)
                .subscribe(message -> {
                    CompletableFuture<U> future = futureResponses.get(message.trackingId());
                    if (future != null) {
                        future.complete(message);
                        return;
                    }
                    LOGGER.warn("No future was found for tracking id {}", message.trackingId());
                });
    }

    @Override
    public void send(T message) {
        byte[] messageBytes = outboundParser.serialize(message);
        outbound.sendByteArray(Mono.just(messageBytes)).then().subscribe();
    }

    @Override
    public Future<U> request(T message) {
        String trackingId = message.trackingId();
        CompletableFuture<U> futureResponse = new CompletableFuture<>();
        futureResponses.put(trackingId, futureResponse);
        send(message);
        return futureResponse;
    }
}
