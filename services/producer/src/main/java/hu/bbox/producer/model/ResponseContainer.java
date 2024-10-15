package hu.bbox.producer.model;

import java.util.Optional;

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
