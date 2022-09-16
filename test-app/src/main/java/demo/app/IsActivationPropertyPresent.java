package demo.app;

import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.util.function.BiPredicate;

public class IsActivationPropertyPresent implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> doctorProvider) {
        return providerRegistry.configuration().get("activateOptionals", false, Boolean::parseBoolean);
    }
}
