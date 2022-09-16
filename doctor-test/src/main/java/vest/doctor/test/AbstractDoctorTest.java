package vest.doctor.test;


import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import vest.doctor.InjectionException;
import vest.doctor.ProviderRegistry;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.runtime.Doctor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base test for testing {@link Doctor} applications . Automatically configures
 * a doctor instance using the {@link TestConfiguration} annotation. The annotation is required
 * and the test will fail to start with an {@link IllegalStateException} if missing.
 */
@Listeners(TestOrderRandomizer.class)
public abstract class AbstractDoctorTest extends Assert {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    private Doctor doctor;

    @BeforeClass(alwaysRun = true)
    public final void setupDoctor() {
        TestConfiguration testConfiguration = getClass().getAnnotation(TestConfiguration.class);
        if (testConfiguration == null) {
            throw new IllegalStateException("test classes must have a @TestConfiguration annotation");
        }
        this.doctor = DoctorInstanceManager.singleton().getOrCreate(testConfiguration);
        try {
            reflectiveInject(doctor, this);
        } catch (Throwable t) {
            log.error("error injecting test class {}", getClass().getName(), t);
            log.error("Current doctor state\n{}", doctor);
            throw t;
        }
    }

    @AfterSuite(alwaysRun = true)
    public final void tearDownDoctor() {
        DoctorInstanceManager.singleton().close();
    }

    /**
     * Get the {@link ProviderRegistry} that was configured via the {@link TestConfiguration}
     * marked for this class.
     */
    public ProviderRegistry providerRegistry() {
        return Objects.requireNonNull(doctor, "this test was not properly initialized");
    }

    /**
     * Alias for <code>providerRegistry().configuration()</code>
     */
    public ConfigurationFacade configuration() {
        return providerRegistry().configuration();
    }

    private static void reflectiveInject(ProviderRegistry providerRegistry, Object instance) {
        for (Field declaredField : instance.getClass().getDeclaredFields()) {
            if (declaredField.isAnnotationPresent(Inject.class)) {
                Object value = getInjectValue(providerRegistry, declaredField.getGenericType(), qualifier(declaredField));
                Object accessCheckInstance = Modifier.isStatic(declaredField.getModifiers()) ? null : instance;
                if (!declaredField.canAccess(accessCheckInstance)) {
                    declaredField.setAccessible(true);
                }
                try {
                    declaredField.set(accessCheckInstance, value);
                } catch (IllegalAccessException e) {
                    throw new InjectionException("failed to reflectively set field value: " + declaredField, e);
                }
            }
        }

        for (Method declaredMethod : instance.getClass().getDeclaredMethods()) {
            if (declaredMethod.isAnnotationPresent(Inject.class)) {
                Parameter[] parameters = declaredMethod.getParameters();
                Object[] params = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    Parameter param = parameters[i];
                    params[i] = getInjectValue(providerRegistry, param.getType(), qualifier(param));
                }
                Object accessCheckInstance = Modifier.isStatic(declaredMethod.getModifiers()) ? null : instance;
                if (!declaredMethod.canAccess(accessCheckInstance)) {
                    declaredMethod.setAccessible(true);
                }
                try {
                    declaredMethod.invoke(accessCheckInstance, params);
                } catch (Throwable e) {
                    throw new InjectionException("failed to reflectively call inject method: " + declaredMethod, e);
                }
            }
        }
    }

    private static Object getInjectValue(ProviderRegistry providerRegistry, Type type, String qualifier) {
        if (type instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            Class<?> paramType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
            if (Collection.class.isAssignableFrom(rawType)) {
                Stream<?> stream = providerRegistry.getProviders(paramType, qualifier).map(Provider::get);
                if (Set.class.isAssignableFrom(rawType)) {
                    return stream.collect(Collectors.toSet());
                } else {
                    return stream.collect(Collectors.toList());
                }
            } else if (Stream.class.isAssignableFrom(rawType)) {
                return providerRegistry.getProviders(paramType, qualifier).map(Provider::get);
            } else if (Optional.class.isAssignableFrom(rawType)) {
                return providerRegistry.getProviderOpt(paramType, qualifier).map(Provider::get);
            } else if (Provider.class.isAssignableFrom(rawType)) {
                return providerRegistry.getProvider(paramType, qualifier);
            }
            throw new UnsupportedOperationException("unable to inject value for " + type);
        } else if (type instanceof Class<?> clazz) {
            return providerRegistry.getInstance(clazz, qualifier);
        } else {
            throw new UnsupportedOperationException("unable to inject value for " + type);
        }
    }

    private static String qualifier(AnnotatedElement annotatedElement) {
        return qualifierOpt(annotatedElement)
                .map(a -> {
                    if (a instanceof Named named) {
                        return named.value();
                    } else {
                        return annotationString(a);
                    }
                })
                .map(String::valueOf)
                .orElse(null);
    }

    private static Optional<Annotation> qualifierOpt(AnnotatedElement annotatedElement) {
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            if (annotation.annotationType() == Qualifier.class) {
                return Optional.of(annotation);
            }
            for (Annotation extendsAnnotations : annotation.annotationType().getAnnotations()) {
                if (extendsAnnotations.annotationType() == Qualifier.class) {
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }

    private static String annotationString(Annotation annotation) {
        if (annotation == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(annotation.annotationType().getCanonicalName());
        Map<String, String> methodToValue = new HashMap<>();
        for (Method declaredMethod : annotation.annotationType().getDeclaredMethods()) {
            try {
                Object value = declaredMethod.invoke(annotation);
                methodToValue.put(declaredMethod.getName(), String.valueOf(value));
            } catch (Throwable e) {
                throw new RuntimeException("failed to build annotation string", e);
            }
        }
        String valuesString = methodToValue.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ", "(", ")"));
        sb.append(valuesString);
        return sb.toString();
    }
}
