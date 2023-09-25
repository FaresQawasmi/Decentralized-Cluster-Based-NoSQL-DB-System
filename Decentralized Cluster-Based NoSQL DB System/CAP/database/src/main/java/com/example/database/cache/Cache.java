package com.example.database.cache;

import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class Cache<K, V> {

    private final int MAX_SIZE = 5;
    private final Map<K, V> cache;
    private final ScheduledExecutorService cleanerThread;

    public Cache() {
        this.cache = new LinkedHashMap<>(5, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > MAX_SIZE;
            }
        };

        this.cleanerThread = Executors.newSingleThreadScheduledExecutor();
    }

    public void add(K key, V value, long periodInMillis) {
        if (key == null) {
            return;
        }
        if (value == null) {
            cache.remove(key);
        } else {
            cache.put(key, value);
        }

        if (periodInMillis > 0) {
            cleanerThread.schedule(() -> cache.remove(key), periodInMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void clear() {
        cache.clear();
    }

    public long size() {
        return cache.size();
    }
}
