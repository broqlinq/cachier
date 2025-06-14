package io.github.broqlinq.cachier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryCacheCoreFunctionalityTest {

    private static final int TEST_CAPACITY = 10;

    private InMemoryCache<String> cache;

    @BeforeEach
    void setup() {
        cache = new InMemoryCache<>(TEST_CAPACITY);
    }

    @AfterEach
    void cleanup() {
        try {
            cache.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* Core Functionality Tests */

    @Test
    @DisplayName("Call to get() should return non-empty Optional when key is present cache")
    public void get_should_returnNonEmptyOptional_when_keyIsPresent() {

        String key = "key";
        String value = "expected value";

        cache.put(key, value, 1, TimeUnit.MINUTES);

        var retrievedValue = cache.get(key);

        assertThat(retrievedValue).isPresent().contains(value);
    }

    @Test
    @DisplayName("Call to get() should return Optional.empty() when key is not present in cache")
    public void get_should_returnEmptyOptional_when_keyIsNotPresent() {

        var value = cache.get("key");

        assertThat(value).isEmpty();
    }

    @Test
    @DisplayName("Call to put() should overwrite old value when key is already present in cache")
    public void put_should_overwriteOldValue_when_keyIsPresent() {
        var key = "key";
        var oldValue = "old value";
        cache.put(key, oldValue, 1, TimeUnit.MINUTES);

        var newValue = "new value";
        cache.put(key, newValue, 1, TimeUnit.MINUTES);

        var retrievedValue = cache.get(key);

        assertThat(retrievedValue).isPresent().contains(newValue);
    }

    @Test
    @DisplayName("Call to get() should return Optional.empty() when key was previously deleted")
    public void get_should_returnEmptyOptional_when_keyIsDeleted() {
        var key = "key";
        var value = "value";
        cache.put(key, value, 1, TimeUnit.MINUTES);
        cache.delete(key);

        var retrievedValue = cache.get(key);

        assertThat(retrievedValue).isEmpty();
    }

    @Test
    @DisplayName("Call to get() should return Optional.empty() for all previously present keys after calling clear()")
    public void get_should_returnEmptyOptional_when_cacheIsCleared() {
        record KeyValuePair(String key, String value) {
        }

        var keyValuePairs = IntStream.rangeClosed(1, 5)
                .mapToObj(n -> new KeyValuePair("key " + n, "value " + n))
                .toList();

        for (var pair : keyValuePairs) {
            cache.put(pair.key, pair.value, 1, TimeUnit.MINUTES);
        }

        cache.clear();

        for (var pair : keyValuePairs) {
            var value = cache.get(pair.key);
            assertThat(value).isEmpty();
        }
    }

}
