package doctor.cluster.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import doctor.cluster.serder.SerDer;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class LocalCache {
    private final Map<String, Cache<String, byte[]>> caches;

    public LocalCache() {
        caches = new ConcurrentSkipListMap<>();
    }

    public void put(String cacheName, String key, Object o) {
        Cache<String, byte[]> cache = caches.computeIfAbsent(cacheName, name -> Caffeine.newBuilder().build());
        cache.put(key, SerDer.serialize(o));
    }

    public <T> T get(String cacheName, String key, Class<T> type) {
        Cache<String, byte[]> cache = caches.computeIfAbsent(cacheName, name -> Caffeine.newBuilder().build());
        byte[] bytes = cache.getIfPresent(key);
        return bytes != null ? SerDer.deserialize(bytes, type) : null;
    }
}
