package hu.bbox.messaging.message;

public interface Message<T> {
    String trackingId();
    T message();
    MessageType messageType();
}
