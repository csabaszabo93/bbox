package hu.bbox.producer.handlers;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.message.MessageType;
import hu.bbox.messaging.parser.MessageSerializer;
import hu.bbox.producer.model.ResponseContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Handles the incoming {@link Envelope<String>} instance, forwards the message from the envelope to the configured
 * underlying message handler.
 */
public class EnvelopeHandler implements BiConsumer<Envelope<String>, NettyOutbound> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvelopeHandler.class);
    private final Consumer<String> registrationHandler;
    private final Function<String, ResponseContainer<String>> requestHandler;
    private final MessageSerializer<Envelope<String>> messageSerializer;

    public EnvelopeHandler(
            Consumer<String> registrationHandler,
            Function<String, ResponseContainer<String>> requestHandler,
            MessageSerializer<Envelope<String>> messageSerializer) {
        this.registrationHandler = registrationHandler;
        this.requestHandler = requestHandler;
        this.messageSerializer = messageSerializer;
    }

    @Override
    public void accept(Envelope<String> envelope, NettyOutbound out) {
        LOGGER.info("Starting to handle incoming envelope");
        String message = envelope.message();
        LOGGER.trace("Received message: {}", message);
        switch (envelope.messageType()) {
            case REGISTRATION -> registrationHandler.accept(message);
            case REQUEST -> {
                ResponseContainer<String> container = requestHandler.apply(message);
                container.response().ifPresentOrElse(
                        response -> sendResponse(envelope.trackingId(), response, out),
                        () -> container.exception().ifPresentOrElse(
                                exception -> sendFailure(envelope.trackingId(), exception.getMessage(), out),
                                () -> sendFailure(envelope.trackingId(), out)));
            }
            default -> {
                LOGGER.warn("Unsupported message type, dropping message");
                LOGGER.trace("{}", envelope);
            }
        }
    }

    private void sendFailure(String trackingId, NettyOutbound out) {
        sendFailure(trackingId, "", out);
    }

    private void sendFailure(String trackingId, String message, NettyOutbound out) {
        LOGGER.info("Sending failure");
        Envelope<String> responseEnvelope =
                new Envelope<>(trackingId, MessageType.FAILURE, message);
        sendEnvelope(responseEnvelope, out);
    }

    private void sendResponse(String trackingId, String response, NettyOutbound out) {
        LOGGER.info("Sending response");
        Envelope<String> responseEnvelope =
                new Envelope<>(trackingId, MessageType.RESPONSE, response);
        sendEnvelope(responseEnvelope, out);
    }

    private void sendEnvelope(Envelope<String> envelope, NettyOutbound out) {
        LOGGER.trace("Outgoing envelope {}", envelope);
        byte[] bytes = messageSerializer.serialize(envelope);
        out.sendByteArray(Mono.just(bytes)).then().subscribe();
    }
}
