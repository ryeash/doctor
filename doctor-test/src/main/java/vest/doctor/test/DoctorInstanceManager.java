package vest.doctor.test;

import vest.doctor.ConfigurationFacade;
import vest.doctor.InjectionException;
import vest.doctor.runtime.Doctor;
import vest.doctor.runtime.StructuredConfigurationSource;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

final class DoctorInstanceManager implements AutoCloseable {

    private static final class DoctorInstanceManagerSingletonHolder {
        private static final DoctorInstanceManager INSTANCE = new DoctorInstanceManager();
    }

    public static DoctorInstanceManager singleton() {
        return DoctorInstanceManagerSingletonHolder.INSTANCE;
    }

    private final Map<TestConfiguration, Doctor> instances = new ConcurrentHashMap<>(4, 1, 2);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public Doctor getOrCreate(TestConfiguration doctorConfig) {
        if (closed.get()) {
            throw new InjectionException("this manager has already been shutdown");
        }
        return instances.computeIfAbsent(doctorConfig, this::create);
    }

    private Doctor create(TestConfiguration doctorConfig) {
        ConfigurationFacade configurationFacade = buildConfig(doctorConfig);
        for (String location : doctorConfig.propertyFiles()) {
            configurationFacade.addSource(new StructuredConfigurationSource(location));
        }
        return Doctor.load(configurationFacade, doctorConfig.modules());
    }

    public int size() {
        return instances.size();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            instances.values().forEach(Doctor::close);
            instances.clear();
        }
    }

    private static ConfigurationFacade buildConfig(TestConfiguration config) {
        Constructor<? extends Supplier<? extends ConfigurationFacade>> constructor;
        try {
            constructor = config.configurationBuilder().getConstructor();
        } catch (NoSuchMethodException e) {
            throw new InjectionException("invalid configuration builder used in " + config + ", no zero-arg, accessible constructor found", e);
        }
        try {
            return constructor.newInstance().get();
        } catch (Throwable e) {
            throw new InjectionException("failed to create new instance of the configuration builder", e);
        }
    }
}
