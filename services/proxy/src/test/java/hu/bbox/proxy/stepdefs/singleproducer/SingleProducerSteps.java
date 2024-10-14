package hu.bbox.proxy.stepdefs.singleproducer;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CucumberContextConfiguration
@SpringBootTest(classes = IntegrationConfig.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations="classpath:test.properties")
public class SingleProducerSteps {
    private Connection connection;
    private TcpClient tcpClient;
    private String exampleId;
    private String payload;
    private String response;
    private final MessageSerializer<Envelope<String>> messageSerializer = new KryoStringEnvelopeSerializer();
    private final CompletableFuture<Envelope<String>> futureEnvelope = new CompletableFuture<>();
    private final CompletableFuture<String> futureResponse = new CompletableFuture<>();

    @Autowired private IntegrationIdGenerator idGenerator;
    @Autowired private Environment environment;

    @Before
    public void setup() {
        tcpClient = TcpClient.create()
                .host("localhost")
                .port(environment.getProperty("hu.bbox.proxy.producer.port", Integer.class));
    }

    @Given("^example id (.*)$")
    public void setExampleId(String id) {
        exampleId = id;
        idGenerator.setId(exampleId);
    }

    @Given("^request payload string (.*)$")
    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Given("^connected producer$")
    public void givenConnectedProducer () throws Throwable {
        connection = tcpClient.connectNow(Duration.ofMinutes(2));
        connection.inbound().receive().asByteArray().subscribe(bytes -> {
            Envelope<String> envelope = messageSerializer.deserialize(bytes);
            if (envelope.messageType() == MessageType.REQUEST) {
                futureEnvelope.complete(envelope);
                if (payload.equals(envelope.message())) {
                    Envelope<String> responseEnvelope =
                            new Envelope<>(envelope.trackingId(), MessageType.RESPONSE, response);
                    connection.outbound()
                            .sendByteArray(Mono.just(messageSerializer.serialize(responseEnvelope)))
                            .then()
                            .subscribe();
                }
            }
        });
    }

    @Given("^response string (.*)")
    public void setResponse (String response) throws Throwable {
        this.response = response;
    }

    @When("^http request sent with payload$")
    public void httpRequestSentWithPayload () throws Throwable {
        HttpClient
                .create()
                .host("localhost")
                .port(8080)
                .headers(builder -> builder
                        .add("X-Producer-Id", exampleId)
                        .add(HttpHeaders.CONTENT_LENGTH, payload.length())
                        .add(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_XML))
                .post()
                .send(ByteBufFlux.fromString(Mono.just(payload)))
                .responseContent()
                .aggregate()
                .asString()
                .subscribe(futureResponse::complete);
    }

    @Then("^http response received with response$")
    public void getsBackExpectedResponse() throws ExecutionException, InterruptedException, TimeoutException {
        String response = futureResponse.get(10, TimeUnit.SECONDS);
        assertEquals(this.response, response);
    }

    @After
    public void tearDown() {
        connection.disposeNow();
    }
}

