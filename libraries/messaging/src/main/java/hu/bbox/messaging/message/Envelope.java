package hu.bbox.messaging.message;

import java.util.UUID;

public record Envelope<T>(String trackingId, MessageType messageType, T message) implements Message<T> {
    public Envelope(MessageType messageType) {
        this(UUID.randomUUID().toString(), messageType, null);
    }
    public Envelope(MessageType messageType, T message) {
        this(UUID.randomUUID().toString(), messageType, message);
    }
}
