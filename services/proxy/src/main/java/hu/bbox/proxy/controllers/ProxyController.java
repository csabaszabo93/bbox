package hu.bbox.proxy.controllers;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.message.MessageType;
import hu.bbox.proxy.Proxy;
import hu.bbox.proxy.message.AsyncMessageSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Controller
public class ProxyController {
    private final Proxy<AsyncMessageSender<Envelope<String>, Envelope<String>>> proxy;
    private final int delegateTimeout;
    private final TimeUnit delegateTimeoutUnit;

    public ProxyController(
            Proxy<AsyncMessageSender<Envelope<String>, Envelope<String>>> proxy,
            @Value("${hu.bbox.proxy.timeout:1}") int delegateTimeout,
            @Value("${hu.bbox.proxy.timeout.unit:MINUTES}")TimeUnit delegateTimeoutUnit) {
        this.proxy = proxy;
        this.delegateTimeout = delegateTimeout;
        this.delegateTimeoutUnit = delegateTimeoutUnit;
    }

    @PostMapping("/")
    public ResponseEntity<String> forwardMessage(
            @RequestHeader(name = "X-Producer-Id") String producerId,
            @RequestBody String message) {
        return proxy.getDelegate(producerId)
                .map(delegate -> getResponseEntity(delegate, producerId, message))
                .orElse(ResponseEntity.status(404).body("No producer was find with id: " + producerId));
    }

    private ResponseEntity<String> getResponseEntity(
            AsyncMessageSender<Envelope<String>, Envelope<String>> delegate,
            String producerId,
            String message) {
        ResponseEntity<String> responseEntity;
        try {
            Envelope<String> requestEnvelope = new Envelope<>(MessageType.REQUEST, message);
            Future<Envelope<String>> futureEnvelope = delegate.request(requestEnvelope);
            Envelope<String> envelope = futureEnvelope.get(delegateTimeout, delegateTimeoutUnit);
            responseEntity = ResponseEntity.ok(envelope.message());
        } catch (ExecutionException e) {
            responseEntity = getErrorEntity(producerId);
        } catch (TimeoutException e) {
            responseEntity = ResponseEntity
                    .status(504)
                    .body("Timed out while waiting for producer response: " + producerId);;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            responseEntity = getErrorEntity(producerId);
        }
        return responseEntity;
    }

    private static ResponseEntity<String> getErrorEntity(String producerId) {
        return ResponseEntity
                .status(500)
                .body("Failed to get response from producer: " + producerId);
    }
}
