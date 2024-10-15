package hu.bbox.producer.handlers;

import hu.bbox.producer.model.ResponseContainer;
import jakarta.xml.soap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Handles the soap xml string received in a request envelope, with o simplified exception handling
 */
public class SimpleSoapRequestHandler implements Function<String, ResponseContainer<String>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSoapRequestHandler.class);
    private final UnaryOperator<String> productVatStore;
    private final MessageFactory soapMessageFactory;

    public SimpleSoapRequestHandler(UnaryOperator<String> productVatStore) {
        this.productVatStore = productVatStore;
        try {
            this.soapMessageFactory = MessageFactory.newInstance();
        } catch (SOAPException e) {
            LOGGER.debug("Failed to create soap message factory");
            throw new RuntimeException(e);
        }
    }

    @Override
    public ResponseContainer<String> apply(String soapEnvelopeIn) {
        ResponseContainer<String> responseContainer;
        try {
            InputStream inputStream = new ByteArrayInputStream(soapEnvelopeIn.getBytes());
            String product = getProductFromEnvelope(inputStream);
            String productVAT = productVatStore.apply(product);
            LOGGER.info("Received product's vat from store");
            LOGGER.trace("VAT is {}", productVAT);
            SOAPMessage responseMessage = buildResponse(productVAT);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            responseMessage.writeTo(outputStream);
            responseContainer = new ResponseContainer<>(outputStream.toString());
        } catch (Exception e) {
            LOGGER.error("Failed to process soap request");
            responseContainer = new ResponseContainer<>(e);
        }
        return responseContainer;
    }

    private SOAPMessage buildResponse(String productVAT) throws SOAPException {
        LOGGER.info("Creating response soap message");
        SOAPMessage responseMessage = soapMessageFactory.createMessage();
        responseMessage.getSOAPPart().getEnvelope()
                .addNamespaceDeclaration("bbox", "http://www.bbox.hu/soap");
        responseMessage
                .getSOAPBody()
                .addChildElement("bbox:ProductVAT")
                .setValue(productVAT);
        LOGGER.info("Response soap message created");
        return responseMessage;
    }

    private String getProductFromEnvelope(InputStream inputStream) throws IOException, SOAPException {
        LOGGER.info("Parsing request soap message");
        SOAPMessage requestMessage = soapMessageFactory.createMessage(new MimeHeaders(), inputStream);
        SOAPBody soapBody = requestMessage.getSOAPBody();
        NodeList nodes = soapBody.getElementsByTagName("bbox:RequestVATOfProduct");
        String product = nodes.item(0).getFirstChild().getNodeValue().strip();
        LOGGER.trace("Extracted from soap request: {}", product);
        return product;
    }
}
