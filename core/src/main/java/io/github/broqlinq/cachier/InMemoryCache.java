package io.github.broqlinq.cachier;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryCache<V> implements Cache<V>, AutoCloseable {

    private final ConcurrentMap<String, EvictableNode> cache = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();

    private final EvictableNode head;
    private final EvictableNode tail;

    private final int capacity;

    private final ScheduledExecutorService executor;

    public InMemoryCache(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("expected positive cache capacity: " + capacity);

        this.capacity = capacity;

        head = new EvictableNode(null, null, -1);
        tail = new EvictableNode(null, null, -1);
        head.next = tail;
        tail.prev = head;

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(this::evictExpired, 5, 5, TimeUnit.SECONDS);
    }

    public void evictExpired() {
        lock.lock();
        try {
            for (var node = tail.prev; node != head;) {
                var prev = node.prev;
                if (System.currentTimeMillis() > node.expiresAt) {
                    cache.remove(node.key);
                    detachFully(node);
                }
                node = prev;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(String key, V value, long timeout, TimeUnit unit) {
        if (timeout <= 0)
            throw new IllegalArgumentException("expected positive timeout value: " + timeout);

        final long validUntil = System.currentTimeMillis() + unit.toMillis(timeout);
        var newNode = new EvictableNode(key, value, validUntil);

        lock.lock();
        try {
            var oldNode = cache.put(key, newNode);
            if (oldNode != null) {
                detachFully(oldNode);
            }
            attachToHead(newNode);

            if (cache.size() > capacity) {
                var lru = tail.prev;
                if (lru != head) {
                    cache.remove(lru.key);
                    detach(lru);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<V> get(String key) {
        var node = cache.get(key);
        if (node == null) {
            return Optional.empty();
        }

        lock.lock();
        try {
            moveToFront(node);
        } finally {
            lock.unlock();
        }

        return Optional.of(node.value);
    }

    private void moveToFront(EvictableNode node) {
        if (head.next == node) {
            return;
        }

        detach(node);
        attachToHead(node);
    }

    // Called only on nodes that are actually already part of the LRU list
    private void detach(EvictableNode node) {
        final var prev = node.prev;
        final var next = node.next;
        prev.next = next;
        next.prev = prev;
    }

    // Called only on nodes that are actually already part of the LRU list,
    // this method clears references to previous/next nodes
    private void detachFully(EvictableNode node) {
        detach(node);
        node.prev = null;
        node.next = null;
    }

    // Called only on previously detached/newly created nodes
    private void attachToHead(EvictableNode node) {
        final var prev = head;
        final var next = head.next;
        prev.next = node;
        node.next = next;
        next.prev = node;
        node.prev = prev;
    }

    public void delete(String key) {
        var node = cache.remove(key);
        if (node != null) {
            lock.lock();
            try {
                detachFully(node);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            head.next = tail;
            tail.prev = head;
            cache.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        clear();
    }

    private final class EvictableNode {

        final String key;
        final V value;
        final long expiresAt;

        EvictableNode prev;
        EvictableNode next;

        EvictableNode(String key, V value, long expiresAt) {
            this.key = key;
            this.value = value;
            this.expiresAt = expiresAt;
        }

    }

}
