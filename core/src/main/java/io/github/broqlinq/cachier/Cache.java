package io.github.broqlinq.cachier;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface Cache<V> {

    void put(String key, V value, long timeout, TimeUnit unit);

    Optional<V> get(String key);

    void delete(String key);

    void clear();
}
