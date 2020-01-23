package vest.doctor.aop;

import vest.doctor.BeanProvider;
import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;

import java.util.function.BiFunction;

public class AspectWrappingProvider<T> extends DoctorProviderWrapper<T> {

    private final BeanProvider beanProvider;
    private final BiFunction<T, BeanProvider, T> wrapper;

    public AspectWrappingProvider(DoctorProvider<T> delegate, BeanProvider beanProvider, BiFunction<T, BeanProvider, T> wrapper) {
        super(delegate);
        this.beanProvider = beanProvider;
        this.wrapper = wrapper;
    }

    @Override
    public T get() {
        T t = super.get();
        return wrapper.apply(t, beanProvider);
    }
}
