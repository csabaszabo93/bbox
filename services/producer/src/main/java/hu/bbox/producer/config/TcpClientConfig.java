package hu.bbox.producer.config;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.parser.KryoStringEnvelopeSerializer;
import hu.bbox.messaging.parser.MessageSerializer;
import hu.bbox.producer.handlers.EnvelopeHandler;
import hu.bbox.producer.handlers.LoggingRegistrationHandler;
import hu.bbox.producer.handlers.SimpleSoapRequestHandler;
import hu.bbox.producer.model.ResponseContainer;
import hu.bbox.producer.repositories.RandomProductVatStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpClient;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Configuration
public class TcpClientConfig {
    @Bean
    public MessageSerializer<Envelope<String>> messageSerializer() {
        return new KryoStringEnvelopeSerializer();
    }

    @Bean
    public Consumer<String> registrationHandler() {
        return new LoggingRegistrationHandler();
    }

    @Bean
    public UnaryOperator<String> productStore() {
        return new RandomProductVatStore();
    }

    @Bean
    public Function<String, ResponseContainer<String>> requestHandler(UnaryOperator<String> productStore) {
        return new SimpleSoapRequestHandler(productStore);
    }

    @Bean
    public BiConsumer<Envelope<String>, NettyOutbound> envelopeHandler(
            Consumer<String> registrationHandler,
            Function<String, ResponseContainer<String>> requestHandler,
            MessageSerializer<Envelope<String>> messageSerializer) {
        return new EnvelopeHandler(registrationHandler, requestHandler, messageSerializer);
    }

    @Bean
    public TcpClient tcpClient(
            @Value("${hu.bbox.proxy.producer.port:45820}") int port,
            @Value("${hu.bbox.proxy.host:localhost}") String proxyHost,
            MessageSerializer<Envelope<String>> messageSerializer,
            BiConsumer<Envelope<String>, NettyOutbound> envelopeHandler) {
        return TcpClient
                .create()
                .host(proxyHost)
                .port(port)
                .handle((in, out) -> {
                    in.receive()
                            .asByteArray()
                            .map(messageSerializer::deserialize)
                            .subscribe(envelope -> envelopeHandler.accept(envelope, out));
                    return Mono.never();
                });
    }
}
