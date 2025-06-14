package io.github.broqlinq.cachier;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class InMemoryCacheExpiredEvictionTest {

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
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    @DisplayName("Call to get() on an a key that has expired should return Optional.empty()")
    public void get_should_returnEmptyOptional_when_anItemHasExpired() throws InterruptedException {
        cache.put("key A", "value B", 100, TimeUnit.MILLISECONDS);
        Thread.sleep(200);
        cache.evictExpired();

        assertThat(cache.get("key A")).as("Expired key should've been evicted").isEmpty();
    }

    @Test
    @DisplayName("Call to get() on a key that has not expired should return non-empty Optional with its value")
    public void get_should_returnNonEmptyOptional_when_anItemHasNotExpired() throws InterruptedException {
        cache.put("key B", "value B", 5, TimeUnit.SECONDS);
        Thread.sleep(1000);

        assertThat(cache.get("key B")).as("Non-expired key should still be present").isPresent();
    }

    @Test
    @DisplayName("Call to get() after updating a value for a key should still return non-empty Optional with updated value")
    public void get_should_returnNonEmptyOptional_when_anItemIsUpdated() throws InterruptedException {
        cache.put("key", "value", 100, TimeUnit.MILLISECONDS);
        Thread.sleep(50);

        cache.put("key", "value", 100, TimeUnit.MILLISECONDS);
        Thread.sleep(80);

        assertThat(cache.get("key")).as("Updated value for existing should still be present").isPresent();
    }
}
