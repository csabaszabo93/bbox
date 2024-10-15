package hu.bbox.proxy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to register the delegate objects
 *
 * @param <T> type of delegate object
 */
public class SimpleProxy<T> implements Proxy<T>{
    private final Map<String, T> delegateMap = new ConcurrentHashMap<>();

    @Override
    public Optional<T> getDelegate(String delegateId) {
        return Optional.ofNullable(delegateMap.get(delegateId));
    }

    @Override
    public void registerDelegate(String delegateId, T delegate) {
        delegateMap.put(delegateId, delegate);
    }

    @Override
    public void removeDelegate(String id) {
        delegateMap.remove(id);
    }
}
