package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Used internally for wrapping and altering the behaviors of a {@link DoctorProvider}.
 */
public abstract class DoctorProviderWrapper<T> implements DoctorProvider<T> {
    protected final DoctorProvider<T> delegate;

    public DoctorProviderWrapper(DoctorProvider<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public TypeInfo typeInfo() {
        return delegate.typeInfo();
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
    public List<String> modules() {
        return delegate.modules();
    }

    @Override
    public void validateDependencies(ProviderRegistry providerRegistry) {
        delegate.validateDependencies(providerRegistry);
    }

    @Override
    public T get() {
        return delegate.get();
    }

    @Override
    public void destroy(T instance) throws Exception {
        delegate.destroy(instance);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + delegate + ')';
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
