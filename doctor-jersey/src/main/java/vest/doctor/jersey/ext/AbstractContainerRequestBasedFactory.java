package vest.doctor.jersey.ext;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;

public abstract class AbstractContainerRequestBasedFactory<T> implements Factory<T> {
    private final Factory<ContainerRequest> containerRequestFactory;

    protected AbstractContainerRequestBasedFactory(Factory<ContainerRequest> containerRequestFactory) {
        this.containerRequestFactory = containerRequestFactory;
    }

    @Override
    public T provide() {
        return provide(containerRequestFactory.provide());
    }

    @Override
    public void dispose(T instance) {
    }

    protected abstract T provide(ContainerRequest containerRequest);
}
