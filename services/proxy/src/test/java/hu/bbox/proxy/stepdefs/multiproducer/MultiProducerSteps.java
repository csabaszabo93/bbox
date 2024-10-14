package hu.bbox.proxy.stepdefs.multiproducer;

import hu.bbox.messaging.message.Envelope;
import hu.bbox.messaging.message.MessageType;
import hu.bbox.messaging.parser.KryoStringEnvelopeSerializer;
import hu.bbox.messaging.parser.MessageSerializer;
import hu.bbox.proxy.config.IntegrationConfig;
import hu.bbox.proxy.utils.IntegrationIdGenerator;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CucumberContextConfiguration
@SpringBootTest(classes = IntegrationConfig.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
public class MultiProducerSteps {
    private final MessageSerializer<Envelope<String>> messageSerializer = new KryoStringEnvelopeSerializer();
    private final Map<String, Connection> connections = new HashMap<>();
    private final Map<String, CompletableFuture<Integer>> futureStatusCodes = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> futureResponses = new ConcurrentHashMap<>();
    private TcpClient tcpClient;
    private HttpClient httpClient;
    private String request;
    @Autowired private IntegrationIdGenerator idGenerator;
    @Autowired private Environment environment;

    @Before
    public void setup() {
        tcpClient = TcpClient.create()
                .host("localhost")
                .port(environment.getProperty("hu.bbox.proxy.producer.port", Integer.class));

        this.httpClient = HttpClient
                .create()
                .host("localhost")
                .port(environment.getProperty("server.port", Integer.class))
                .headers(builder -> builder.add(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_XML));
    }

    @Given("^request content (.*)$")
    public void setRequest(String request) {
        this.request = request;
        httpClient.headers(builder -> builder.add(HttpHeaders.CONTENT_LENGTH, request.length()));
    }

    @Given("^non existing producer id (.*)$")
    public void registerFuturesOfId(String id) {
        futureStatusCodes.put(id, new CompletableFuture<>());
        futureResponses.put(id, new CompletableFuture<>());
    }

    @Given("^producer disconnects (.*)$")
    public void producerDisconnects(String id) {
        connections.remove(id).disposeNow();
    }

    @Given("^connected producer with id (.*) and responding (.*)$")
    public void createProducer(String id, String response) {
        idGenerator.setId(id);
        Connection connection = tcpClient.connectNow(Duration.ofSeconds(10));
        connection.inbound().receive().asByteArray().subscribe(bytes -> {
            Envelope<String> envelope = messageSerializer.deserialize(bytes);
            if (envelope.messageType() == MessageType.REQUEST) {
                if (request.equals(envelope.message())) {
                    Envelope<String> responseEnvelope =
                            new Envelope<>(envelope.trackingId(),MessageType.RESPONSE, response);
                    connection.outbound()
                            .sendByteArray(Mono.just(messageSerializer.serialize(responseEnvelope)))
                            .then()
                            .subscribe();
                }
            }
        });
        connections.put(id, connection);
        registerFuturesOfId(id);
    }

    @When("^request is sent to (.*)$")
    public void sendRequest(String producerId) {
        CompletableFuture<String> futureResponse = futureResponses.get(producerId);
        httpClient
                .headers(builder -> builder.add("X-Producer-Id", producerId))
                .post()
                .send(ByteBufFlux.fromString(Mono.just(request)))
                .response((response, bytes) -> {
                    int statusCode = response.status().code();
                    futureStatusCodes.get(producerId).complete(statusCode);
                    return bytes.aggregate().asString();//.subscribe(futureResponse::complete);
                })
                .subscribe(futureResponse::complete);
    }

    @Then("^received response using (.*) is (.*)$")
    public void receivesExpectedResponse(String producerId, String expectedResponse)
            throws ExecutionException, InterruptedException, TimeoutException {
        String response = futureResponses.get(producerId).get(10, TimeUnit.SECONDS);
        assertEquals(expectedResponse, response);
    }

    @Then("^received response status using (.*) is (.*)$")
    public void receivesExpectedStatusCode(String producerId, int expectedStatusCode)
            throws ExecutionException, InterruptedException, TimeoutException {
        int statusCode = futureStatusCodes.get(producerId).get(10, TimeUnit.SECONDS);
        assertEquals(expectedStatusCode, statusCode);
    }

    @After()
    public void tearDown() {
        connections.values().forEach(Connection::disposeNow);
    }
}
