package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface BeanProvider {
    List<String> getActiveModules();

    void register(DoctorProvider<?> provider);

    <T> T getInstance(Class<T> type);

    <T> T getInstance(Class<T> type, String qualifier);

    <T> DoctorProvider<T> getProvider(Class<T> type);

    <T> DoctorProvider<T> getProvider(Class<T> type, String qualifier);

    default <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type) {
        return getProviderOpt(type, null);
    }

    <T> Optional<DoctorProvider<T>> getProviderOpt(Class<T> type, String qualifier);

    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type);

    <T> Stream<DoctorProvider<T>> getProviders(Class<T> type, String qualifier);

    Stream<DoctorProvider<?>> getProvidersWithAnnotation(Class<? extends Annotation> annotationType);

    boolean hasProvider(Class<?> type);

    boolean hasProvider(Class<?> type, String qualifier);

    ConfigurationFacade configuration();

    String resolvePlaceholders(String string);

}
