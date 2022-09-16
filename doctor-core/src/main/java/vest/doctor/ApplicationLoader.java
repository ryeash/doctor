package vest.doctor;

/**
 * Loaded by the {@link ProviderRegistry} to boot the application. All ApplicationLoaders configured
 * (via service loading) will be instantiated and the stages will be called in groups, in order; e.g.
 * all <code>stage1</code> methods will be called first in priority order, then all <code>stage2</code>
 * methods, etc.
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
}
