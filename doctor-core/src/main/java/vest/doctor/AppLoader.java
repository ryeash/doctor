package vest.doctor;

import java.util.List;

/**
 * Loaded by the {@link AnnotationProcessorContext} implementation to boot the application. All AppLoaders configured
 * (via service loading) will be instantiated and the three stages will be called in groups: preProcess, load, postProcess.
 */
public interface AppLoader extends Prioritized, AutoCloseable {

    /**
     * Execute any code required during pre-processing.
     *
     * @param providerRegistry the common instance of the {@link ProviderRegistry}
     */
    default void preProcess(ProviderRegistry providerRegistry) {
        // no-op
    }

    /**
     * Execute any code required during loading.
     *
     * @param providerRegistry the common instance of {@link ProviderRegistry}
     */
    default void load(ProviderRegistry providerRegistry) {
        // no-op
    }

    /**
     * Exeucte any code required during post-processing.
     *
     * @param providerRegistry the common instance of {@link ProviderRegistry}
     */
    default void postProcess(ProviderRegistry providerRegistry) {
        // no-op
    }

    @Override
    default void close() throws Exception {
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
