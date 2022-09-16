package vest.doctor.runtime;

import jakarta.inject.Provider;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventBus;
import vest.doctor.event.ReloadConfiguration;

import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

/**
 * Internal use.
 * <p>
 * A properties/configuration trait that provides caching and reloading of processed property values.
 */
public abstract class PropertiesTrait {
    private static final String NO_VALUE = "<<novalue>>";
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
        Object v = propertiesCache.computeIfAbsent(cacheKey, (k) -> Objects.requireNonNullElse(getter.get(), NO_VALUE));
        if (v == NO_VALUE) {
            return null;
        } else {
            return (T) v;
        }
    }

    private void clearCache(ReloadConfiguration reload) {
        propertiesCache.clear();
    }
}
