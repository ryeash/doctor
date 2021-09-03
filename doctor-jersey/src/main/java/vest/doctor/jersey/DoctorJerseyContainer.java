package vest.doctor.jersey;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.spi.ExecutorServiceProvider;
import org.glassfish.jersey.spi.ScheduledExecutorServiceProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

final class DoctorJerseyContainer implements Container {
    private final ApplicationHandler applicationHandler;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    public DoctorJerseyContainer(Application application) {
        this.applicationHandler = new ApplicationHandler(application);
        this.executorService = applicationHandler.getInjectionManager().getInstance(ExecutorServiceProvider.class).getExecutorService();
        this.scheduledExecutorService = applicationHandler.getInjectionManager().getInstance(ScheduledExecutorServiceProvider.class).getExecutorService();
        this.applicationHandler.onStartup(this);
    }

    @Override
    public ResourceConfig getConfiguration() {
        return applicationHandler.getConfiguration();
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return applicationHandler;
    }

    @Override
    public void reload() {
        reload(applicationHandler.getConfiguration());
    }

    @Override
    public void reload(ResourceConfig configuration) {
        throw new UnsupportedOperationException();
    }

    public void handle(ContainerRequest requestContext) {
        // TODO: why can't this work with any other executor?
        executorService.execute(() -> applicationHandler.handle(requestContext));
    }

    ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
}
