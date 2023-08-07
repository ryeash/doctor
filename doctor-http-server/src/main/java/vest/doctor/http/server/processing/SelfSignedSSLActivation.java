package vest.doctor.http.server.processing;

import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.util.function.BiPredicate;

public class SelfSignedSSLActivation implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> doctorProvider) {
        return providerRegistry.configuration().get("doctor.reactor.http.ssl.selfSigned", false, Boolean::valueOf);
    }
}