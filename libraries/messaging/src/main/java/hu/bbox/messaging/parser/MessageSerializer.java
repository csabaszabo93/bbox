package hu.bbox.messaging.parser;

public interface MessageSerializer<T> {

    T deserialize(byte[] message);

    byte[] serialize(T message);
}
