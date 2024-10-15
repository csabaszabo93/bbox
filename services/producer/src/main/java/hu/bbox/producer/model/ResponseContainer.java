package hu.bbox.producer.model;

import java.util.Optional;

/**
 * Data class to contain the result of a request handling, by intention it either has a response value, or
 * an exception, which prevented to get the response value
 *
 * @param <T> type of response
 */
public class ResponseContainer<T> {
    private final T response;
    private final Throwable exception;

    public ResponseContainer(T response, Throwable exception) {
        this.response = response;
        this.exception = exception;
    }

    public ResponseContainer(T response) {
        this(response, null);
    }

    public ResponseContainer(Throwable exception) {
        this(null, exception);
    }

    public Optional<T> response() {
        return Optional.ofNullable(response);
    }

    public Optional<Throwable> exception() {
        return Optional.ofNullable(exception);
    }
}
