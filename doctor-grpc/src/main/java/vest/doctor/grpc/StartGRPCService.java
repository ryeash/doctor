package vest.doctor.grpc;

import io.grpc.Server;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.util.function.BiPredicate;

/**
 * Activation predicate that controls the startup of the {@link Server} in {@link GrpcFactory}.
 */
public class StartGRPCService implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> doctorProvider) {
        return providerRegistry.getInstance(GrpcConfig.class)
                .port()
                .isPresent();
    }
}
