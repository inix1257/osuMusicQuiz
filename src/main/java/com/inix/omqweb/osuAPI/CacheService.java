package com.inix.omqweb.osuAPI;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheService<K, V> {
    private final Map<K, CacheObject<V>> cache = new ConcurrentHashMap<>();
    private final long ttl = 1000 * 60 * 60; // 1 hour

    public V get(K key) {
        CacheObject<V> cacheObject = cache.get(key);
        if (cacheObject == null || cacheObject.isExpired()) {
            cache.remove(key);
            return null;
        }
        return cacheObject.getValue();
    }

    public void put(K key, V value) {
        cache.put(key, new CacheObject<>(value, ttl));
    }

    private static class CacheObject<V> {
        @Getter
        private final V value;
        private final long expiryTime;

        public CacheObject(V value, long ttl) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}