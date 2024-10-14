package hu.bbox.proxy.handlers;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.message.MessageType;
import hu.bbox.messaging.parser.MessageSerializer;
import hu.bbox.proxy.Proxy;
import hu.bbox.proxy.message.AsyncMessageSender;
import hu.bbox.proxy.message.NettyMessageSender;
import org.reactivestreams.Publisher;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class NettyConnectionHandler implements ConnectionHandler {
    private final Map<NettyInbound, String> inboundIds = new ConcurrentHashMap<>();
    private final Map<Connection, String> connectionIds = new ConcurrentHashMap<>();
    private final MessageSerializer<Envelope<String>> messageSerializer;
    private final Proxy<AsyncMessageSender<Envelope<String>, Envelope<String>>> proxy;
    private final Supplier<String> idGenerator;

    public NettyConnectionHandler(
            Supplier<String> idGenerator,
            MessageSerializer<Envelope<String>> messageSerializer,
            Proxy<AsyncMessageSender<Envelope<String>, Envelope<String>>> proxy) {
        this.idGenerator = idGenerator;
        this.messageSerializer = messageSerializer;
        this.proxy = proxy;
    }

    @Override
    public Publisher<Void> apply(NettyInbound inbound, NettyOutbound outbound) {
        String providerId = inboundIds.remove(inbound);
        Objects.requireNonNull(providerId);
        Envelope<String> registrationEnvelope = new Envelope<>(MessageType.REGISTRATION, providerId);
        AsyncMessageSender<Envelope<String>, Envelope<String>> messageSender =
                new NettyMessageSender<>(inbound, outbound, messageSerializer, messageSerializer);
        proxy.registerDelegate(providerId, messageSender);
        messageSender.send(registrationEnvelope);
        return Mono.never();
    }

    @Override
    public void accept(Connection connection) {
        NettyInbound inbound = connection.inbound();
        String id = idGenerator.get();
        inboundIds.put(inbound, id);
        connectionIds.put(connection, id);
    }

    @Override
    public void onStateChange(@NonNull Connection connection, @NonNull State newState) {
        if (newState == State.DISCONNECTING) {
            String id = connectionIds.remove(connection);
            proxy.removeDelegate(id);
        }
    }
}
