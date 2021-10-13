package vest.doctor.test;

import vest.doctor.ConfigurationFacade;
import vest.doctor.InjectionException;
import vest.doctor.runtime.Doctor;
import vest.doctor.runtime.StructuredConfigurationSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DoctorInstanceManager implements AutoCloseable {

    private static final class DoctorInstanceManagerSingletonHolder {
        private static final DoctorInstanceManager INSTANCE = new DoctorInstanceManager();
    }

    public static DoctorInstanceManager singleton() {
        return DoctorInstanceManagerSingletonHolder.INSTANCE;
    }

    private final Map<TestConfiguration, Doctor> instances = new ConcurrentHashMap<>(4, 1, 1);

    public Doctor getOrCreate(TestConfiguration doctorConfig) {
        return instances.computeIfAbsent(doctorConfig, this::create);
    }

    private Doctor create(TestConfiguration doctorConfig) {
        try {
            ConfigurationFacade configurationFacade = doctorConfig.configurationBuilder().getConstructor().newInstance().get();
            for (String location : doctorConfig.propertyFiles()) {
                configurationFacade.addSource(new StructuredConfigurationSource(location));
            }
            return Doctor.load(configurationFacade, doctorConfig.modules());
        } catch (Throwable t) {
            throw new InjectionException("failed to create configuration facade for test", t);
        }
    }

    public int size() {
        return instances.size();
    }

    @Override
    public void close() {
        instances.values().forEach(Doctor::close);
        instances.clear();
    }
}
