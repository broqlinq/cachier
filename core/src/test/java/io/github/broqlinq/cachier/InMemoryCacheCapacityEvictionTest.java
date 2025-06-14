package io.github.broqlinq.cachier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryCacheCapacityEvictionTest {

    private static final int TEST_CAPACITY = 3;

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

    @Test
    @DisplayName("When call to put() causes cache size to exceed capacity, least recently item is removed ")
    public void put_should_evictLruItem_when_capacityIsExceeded() {
        // Fill cache to max capacity
        cache.put("key A", "value A", 1, TimeUnit.MINUTES);
        cache.put("key B", "value B", 1, TimeUnit.MINUTES);
        cache.put("key C", "value C", 1, TimeUnit.MINUTES);

        // Insert a new key-value
        cache.put("key D", "value C", 1, TimeUnit.MINUTES);

        assertThat(cache.get("key A")).as("key A should've been evicted").isEmpty();
        assertThat(cache.get("key B")).as("key B should still be present").isPresent();
        assertThat(cache.get("key C")).as("key C should still be present").isPresent();
        assertThat(cache.get("key D")).as("key D should've been added").isPresent();
    }

    @Test
    @DisplayName("When get() is called on present key, associated value becomes most recently used")
    public void get_should_promoteAnItemAsMostRecentlyUsed_when_thatItemIsRetrieved() {
        // Fill cache to max capacity
        cache.put("key A", "value A", 1, TimeUnit.MINUTES);
        cache.put("key B", "value B", 1, TimeUnit.MINUTES);
        cache.put("key C", "value C", 1, TimeUnit.MINUTES);

        // Retrieve LRU item
        cache.get("key A");

        // Insert a new key-value
        cache.put("key D", "value C", 1, TimeUnit.MINUTES);

        assertThat(cache.get("key A")).as("key A should still be present").isPresent();
        assertThat(cache.get("key B")).as("key B should've been evicted").isEmpty();
        assertThat(cache.get("key C")).as("key C should still be present").isPresent();
        assertThat(cache.get("key D")).as("key D should've been added").isPresent();
    }

    @Test
    @DisplayName("When put() is called on already present key, updated value becomes most recently used")
    public void put_should_promoteAnItemAsMostRecentlyUsed_when_thatItemIsUpdated() {
        // Fill cache to max capacity
        cache.put("key A", "value A", 1, TimeUnit.MINUTES);
        cache.put("key B", "value B", 1, TimeUnit.MINUTES);
        cache.put("key C", "value C", 1, TimeUnit.MINUTES);

        // Update the value of the least recently used item
        cache.put("key A", "new value A", 1, TimeUnit.MINUTES);

        // Insert a new key-value
        cache.put("key D", "value C", 1, TimeUnit.MINUTES);

        assertThat(cache.get("key A")).as("key A should still be present").isPresent();
        assertThat(cache.get("key B")).as("key B should've been evicted").isEmpty();
        assertThat(cache.get("key C")).as("key C should still be present").isPresent();
        assertThat(cache.get("key D")).as("key D should've been added").isPresent();
    }
}
