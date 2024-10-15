package hu.bbox.producer;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.parser.KryoStringEnvelopeSerializer;
import hu.bbox.messaging.parser.MessageSerializer;
import hu.bbox.producer.handlers.EnvelopeHandler;
import hu.bbox.producer.handlers.LoggingRegistrationHandler;
import hu.bbox.producer.handlers.SimpleSoapRequestHandler;
import hu.bbox.producer.model.ResponseContainer;
import hu.bbox.producer.repositories.RandomProductVatStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@SpringBootApplication
public class Application {

	private TcpClient tcpClient;
	private Connection connection;

	public static void main(String[] args) {
		SpringApplication.run(hu.bbox.producer.Application.class, args);
	}

	@Autowired
	public void setTcpClient(TcpClient tcpClient) {
		this.tcpClient = tcpClient;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void run() {
		connection = tcpClient.connectNow(Duration.ofMinutes(2));
		connection.onDispose().block();
	}

	public void close() {
		connection.disposeNow();
	}

}
