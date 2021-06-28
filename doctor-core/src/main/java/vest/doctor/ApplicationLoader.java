package vest.doctor;

import java.util.List;

/**
 * Loaded by the {@link ProviderRegistry} to boot the application. All ApplicationLoaders configured
 * (via service loading) will be instantiated and the stages will be called in groups, in order; e.g.
 * all <code>stage1</code> methods will be called first in priority order, then <code>stage2</code>, etc.
 */
public interface ApplicationLoader extends Prioritized {

    /**
     * Execute stage 1 loading.
     *
     * @param providerRegistry the common instance of the {@link ProviderRegistry}
     */
    default void stage1(ProviderRegistry providerRegistry) {
        // no-op
    }

    /**
     * Execute stage 2 loading.
     *
     * @param providerRegistry the common instance of the {@link ProviderRegistry}
     */
    default void stage2(ProviderRegistry providerRegistry) {
        // no-op
    }

    /**
     * Execute stage 3 loading.
     *
     * @param providerRegistry the common instance of the {@link ProviderRegistry}
     */
    default void stage3(ProviderRegistry providerRegistry) {
        // no-op
    }

    /**
     * Execute stage 4 loading.
     *
     * @param providerRegistry the common instance of the {@link ProviderRegistry}
     */
    default void stage4(ProviderRegistry providerRegistry) {
        // no-op
    }

    /**
     * Execute stage 5 loading.
     *
     * @param providerRegistry the common instance of the {@link ProviderRegistry}
     */
    default void stage5(ProviderRegistry providerRegistry) {
        // no-op
    }

    /**
     * Determine if any of the given modules are in the list of active modules.
     *
     * @param providerRegistry an instance of the {@link ProviderRegistry}
     * @param modules          the modules to check
     * @return true if any of the given modules are in the list of active modules OR if the given list
     * of modules is empty
     */
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
