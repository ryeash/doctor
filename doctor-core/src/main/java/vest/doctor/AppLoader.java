package vest.doctor;

import java.util.List;

public interface AppLoader extends Prioritized, AutoCloseable {

    default void preProcess(ProviderRegistry providerRegistry) {
        // no-op
    }

    default void load(ProviderRegistry providerRegistry) {
        // no-op
    }

    default void postProcess(ProviderRegistry providerRegistry) {
        // no-op
    }

    default boolean isActive(ProviderRegistry providerRegistry, List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return true;
        }
        for (String activeModule : providerRegistry.getActiveModules()) {
            if (modules.contains(activeModule)) {
                return true;
            }
        }
        return false;
    }
}
