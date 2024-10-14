package hu.bbox.proxy.message;

import hu.bbox.messaging.message.Message;

import java.util.concurrent.Future;

public interface AsyncMessageSender<T extends Message<?>, U extends Message<?>> {
    void send(T message);
    Future<U> request(T message);
}
