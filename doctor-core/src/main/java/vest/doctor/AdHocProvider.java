package vest.doctor;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class AdHocProvider<T> implements DoctorProvider<T> {

    private final Class<T> type;
    private final T t;
    private final String qualifier;
    private final List<Class<?>> allProvidedTypes;

    public AdHocProvider(Class<T> type, T t, String qualifier) {
        this(type, t, qualifier, Collections.singletonList(type));
    }

    public AdHocProvider(Class<T> type, T t, String qualifier, List<Class<?>> allProvidedTypes) {
        this.type = type;
        this.t = t;
        this.qualifier = qualifier;
        this.allProvidedTypes = Collections.unmodifiableList(allProvidedTypes);
    }

    @Override
    public Class<T> type() {
        return type;
    }

    @Override
    public String qualifier() {
        return qualifier;
    }

    @Override
    public Class<? extends Annotation> scope() {
        return Singleton.class;
    }

    @Override
    public List<Class<?>> allProvidedTypes() {
        return allProvidedTypes;
    }

    @Override
    public List<Class<? extends Annotation>> allAnnotationTypes() {
        return Collections.emptyList();
    }

    @Override
    public List<String> modules() {
        return Collections.emptyList();
    }

    @Override
    public void validateDependencies(BeanProvider beanProvider) {
        // no-op
    }

    @Override
    public T get() {
        return t;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + type.getSimpleName() + "):" + hashCode();
    }
}
