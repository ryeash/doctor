package vest.doctor.test;

import vest.doctor.ConfigurationFacade;
import vest.doctor.InjectionException;
import vest.doctor.runtime.Doctor;
import vest.doctor.runtime.StructuredConfigurationSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
        ConfigurationFacade configurationFacade;
        try {
            configurationFacade = doctorConfig.configurationBuilder().getConstructor().newInstance().get();
            for (String location : doctorConfig.propertyFiles()) {
                configurationFacade.addSource(new StructuredConfigurationSource(location));
            }
        } catch (Throwable t) {
            throw new InjectionException("failed to create configuration facade for test", t);
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
}
