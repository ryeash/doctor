package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.List;

public abstract class DoctorProviderWrapper<T> implements DoctorProvider<T> {

    protected final DoctorProvider<T> delegate;

    public DoctorProviderWrapper(DoctorProvider<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Class<T> type() {
        return delegate.type();
    }

    @Override
    public String qualifier() {
        return delegate.qualifier();
    }

    @Override
    public Class<? extends Annotation> scope() {
        return delegate.scope();
    }

    @Override
    public List<Class<?>> allProvidedTypes() {
        return delegate.allProvidedTypes();
    }

    @Override
    public List<Class<? extends Annotation>> allAnnotationTypes() {
        return delegate.allAnnotationTypes();
    }

    @Override
    public List<String> modules() {
        return delegate.modules();
    }

    @Override
    public void validateDependencies(BeanProvider beanProvider) {
        delegate.validateDependencies(beanProvider);
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + '(' + delegate + ')';
    }
}
