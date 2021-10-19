package vest.doctor.jersey;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;

final class DoctorJerseyContainer implements Container {
    private final ApplicationHandler applicationHandler;

    public DoctorJerseyContainer(Application application) {
        this.applicationHandler = new ApplicationHandler(application);
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
}
