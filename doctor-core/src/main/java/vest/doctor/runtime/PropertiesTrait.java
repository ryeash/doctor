package vest.doctor.runtime;

import jakarta.inject.Provider;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.ReloadConfiguration;

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

/**
 * Internal use.
 */
public abstract class PropertiesTrait {
    protected final ProviderRegistry providerRegistry;
    protected final ConcurrentSkipListMap<String, Object> propertiesCache;

    protected PropertiesTrait(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
        this.propertiesCache = new ConcurrentSkipListMap<>();
        providerRegistry.getProviderOpt(EventBus.class)
                .map(Provider::get)
                .ifPresent(bus -> bus.addConsumer(ReloadConfiguration.class, this::clearCache));
    }

    @SuppressWarnings("unchecked")
    protected <T> T cached(String cacheKey, Supplier<Object> getter) {
        return (T) propertiesCache.computeIfAbsent(cacheKey, (k) -> getter.get());
    }

    public void clearCache(ReloadConfiguration reload) {
        propertiesCache.clear();
    }
}
