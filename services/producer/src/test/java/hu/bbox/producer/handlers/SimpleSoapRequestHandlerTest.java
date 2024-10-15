package hu.bbox.producer.handlers;

import hu.bbox.producer.model.ResponseContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimpleSoapRequestHandlerTest {
    private final UnaryOperator<String> productVatStoreMock = mock(UnaryOperator.class);
    private final SimpleSoapRequestHandler handler = new SimpleSoapRequestHandler(productVatStoreMock);

    @Test
    void testEnvelopeHandling() {
        String soap = """
        <?xml version="1.0" encoding="UTF-8" standalone="no"?>
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                          xmlns:bbox="http://www.bbox.hu/soap">
            <soapenv:Body>
                <bbox:RequestVATOfProduct>
                    product
                </bbox:RequestVATOfProduct>
            </soapenv:Body>
        </soapenv:Envelope>
        """;
        String expectedSoapResponse = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:bbox=\"http://www.bbox.hu/soap\"><SOAP-ENV:Header/><SOAP-ENV:Body><bbox:ProductVAT>15</bbox:ProductVAT></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        when(productVatStoreMock.apply("product")).thenReturn("15");
        ResponseContainer<String> container = handler.apply(soap);
        Optional<String> optionalResponse = container.response();
        assertTrue(optionalResponse.isPresent());
        assertEquals(expectedSoapResponse, optionalResponse.get());
    }

    @Test
    void testErrorHandling() {
        String soap = "INTENTIONALLY_WRONG";
        ResponseContainer<String> container = handler.apply(soap);
        Optional<Throwable> optionalError = container.exception();
        assertTrue(optionalError.isPresent());
    }
}