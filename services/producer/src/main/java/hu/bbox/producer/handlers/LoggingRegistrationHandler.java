package hu.bbox.producer.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Handler to log the message, primarily meant to log the producer id received in the initial registration envelope.
 */
public class LoggingRegistrationHandler implements Consumer<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingRegistrationHandler.class);

    @Override
    public void accept(String producerId) {
        LOGGER.info("Id assigned by proxy: {}", producerId);
    }
}
