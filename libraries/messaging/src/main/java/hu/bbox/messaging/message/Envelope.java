package hu.bbox.messaging.message;

import java.util.UUID;

/**
 * Data class to encapsulate data shared between the proxy server and soap producer
 *
 * @param trackingId to synchronise responses with requests
 * @param messageType
 * @param message
 * @param <T> type of message
 */
public record Envelope<T>(String trackingId, MessageType messageType, T message) implements Message<T> {
    public Envelope(MessageType messageType) {
        this(UUID.randomUUID().toString(), messageType, null);
    }
    public Envelope(MessageType messageType, T message) {
        this(UUID.randomUUID().toString(), messageType, message);
    }
}
