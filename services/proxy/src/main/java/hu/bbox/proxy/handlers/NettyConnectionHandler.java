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

/**
 * Class to handle some lifecycle events of a connection, when a new connection is created, a new producer id
 * will be created and mapped to the connection and the connection's inbound object for later usage.
 * When the connection is fully initialized the initial registration envelope is sent to the producer, and
 * the {@link Proxy} instance gets a new {@link NettyMessageSender} instance to be able to delegate the
 * soap requests to the producer.
 * When the connection is closed the message sender is removed from the proxy instance
 */
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

    /**
     * Method to execute when the connection is completely initialized
     *
     * @param inbound object to receive messages on
     * @param outbound object to send messages on
     */
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

    /**
     * Method to execute when a new {@link Connection} is created, but not initialized yet
     *
     * @param connection the connection reference
     */
    @Override
    public void accept(Connection connection) {
        NettyInbound inbound = connection.inbound();
        String id = idGenerator.get();
        inboundIds.put(inbound, id);
        connectionIds.put(connection, id);
    }

    /**
     * Method to execute when the connection's state is changing, only reacts to the disconnected new state
     *
     * @param connection the connection reference
     * @param newState the new State
     */
    @Override
    public void onStateChange(@NonNull Connection connection, @NonNull State newState) {
        if (newState == State.DISCONNECTING) {
            String id = connectionIds.remove(connection);
            proxy.removeDelegate(id);
        }
    }
}
