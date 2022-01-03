package vest.doctor.aop;

import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;
import vest.doctor.ProviderRegistry;

import java.util.function.BiFunction;

/**
 * Internally used to bring together aspects and providers.
 */
public final class AspectWrappingProvider<T> extends DoctorProviderWrapper<T> {

    private final ProviderRegistry providerRegistry;
    private final BiFunction<T, ProviderRegistry, T> wrapper;

    public AspectWrappingProvider(DoctorProvider<T> delegate, ProviderRegistry providerRegistry, BiFunction<T, ProviderRegistry, T> wrapper) {
        super(delegate);
        this.providerRegistry = providerRegistry;
        this.wrapper = wrapper;
    }

    @Override
    public T get() {
        T t = super.get();
        return wrapper.apply(t, providerRegistry);
    }
}
