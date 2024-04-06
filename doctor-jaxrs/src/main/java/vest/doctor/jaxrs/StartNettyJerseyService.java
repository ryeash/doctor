package vest.doctor.jaxrs;

import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.util.function.BiPredicate;

public class StartNettyJerseyService implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> doctorProvider) {
        return providerRegistry.getInstance(JAXRSConfiguration.class)
                .bindAddress()
                .isPresent();
    }
}
