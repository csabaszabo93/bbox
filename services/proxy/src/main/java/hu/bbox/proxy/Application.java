package hu.bbox.proxy;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.parser.KryoStringEnvelopeSerializer;
import hu.bbox.proxy.handlers.ConnectionHandler;
import hu.bbox.proxy.handlers.NettyConnectionHandler;
import hu.bbox.proxy.message.AsyncMessageSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpServer;

import java.util.UUID;
import java.util.function.Supplier;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public Proxy<AsyncMessageSender<Envelope<String>, Envelope<String>>> proxy() {
		return new SimpleProxy<>();
	}

	@Bean
	public Supplier<String> idGenerator() {
		return () -> UUID.randomUUID().toString();
	}

	@Bean
	public ConnectionHandler connectionHandler(
			Supplier<String> idGenerator,
			Proxy<AsyncMessageSender<Envelope<String>, Envelope<String>>> proxy) {
		return new NettyConnectionHandler(idGenerator, new KryoStringEnvelopeSerializer(), proxy);
	}

	@Bean
	public DisposableServer producerTcpServer(
			@Value("${hu.bbox.proxy.producer.host:0.0.0.0}") String host,
			@Value("${hu.bbox.proxy.producer.port:45820}") int port,
			ConnectionHandler connectionHandler) {
		return TcpServer
				.create()
				.host(host)
				.port(port)
				.childObserve(connectionHandler)
				.doOnConnection(connectionHandler)
				.handle(connectionHandler)
				//.secure()
				.bindNow();
	}

}
