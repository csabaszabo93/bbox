package hu.bbox.proxy;


import java.util.Optional;

public interface Proxy<T> {
    Optional<T> getDelegate(String delegateId);

    void registerDelegate(String delegateId, T delegate);

    void removeDelegate(String id);
}
