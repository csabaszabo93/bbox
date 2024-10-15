package hu.bbox.proxy;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.parser.KryoStringEnvelopeSerializer;
import hu.bbox.proxy.handlers.ConnectionHandler;
import hu.bbox.proxy.handlers.NettyConnectionHandler;
import hu.bbox.proxy.message.AsyncMessageSender;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;

import java.io.IOException;
import java.io.InputStream;
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

	@Bean(destroyMethod = "disposeNow")
	public DisposableServer producerTcpServer(
			@Value("${hu.bbox.proxy.host:0.0.0.0}") String host,
			@Value("${hu.bbox.proxy.producer.port:45820}") int port,
			@Value("${hu.bbox.proxy.ssl:true}") boolean useSsl,
			ConnectionHandler connectionHandler) throws IOException {
		TcpServer tcpServer = TcpServer.create();
		if (useSsl) {
			ClassPathResource certResource = new ClassPathResource("ssl/bbox.proxy.crt");
			InputStream certStream = new InputStreamResource(certResource).getInputStream();
			ClassPathResource keyResource = new ClassPathResource("ssl/bbox.proxy.key");
			InputStream keyStream = new InputStreamResource(keyResource).getInputStream();
			SslContext sslcontext = SslContextBuilder.forServer(certStream, keyStream).build();
			tcpServer.secure(spec -> spec.sslContext(sslcontext));
		}
		return tcpServer
				.host(host)
				.port(port)
				.childObserve(connectionHandler)
				.doOnConnection(connectionHandler)
				.handle(connectionHandler)
				.bindNow();
	}

}
