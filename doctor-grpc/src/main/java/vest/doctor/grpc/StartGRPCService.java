package vest.doctor.grpc;

import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.util.function.BiPredicate;

public class StartGRPCService implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> doctorProvider) {
        return providerRegistry.getInstance(GrpcConfig.class)
                .port()
                .isPresent();
    }
}
