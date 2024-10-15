package hu.bbox.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.netty.Connection;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;

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
