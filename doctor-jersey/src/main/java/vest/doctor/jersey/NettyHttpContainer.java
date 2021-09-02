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

final class NettyHttpContainer implements Container {
    private volatile ApplicationHandler appHandler;

    public NettyHttpContainer(Application application) {
        this.appHandler = new ApplicationHandler(application);
        this.appHandler.onStartup(this);
    }

    @Override
    public ResourceConfig getConfiguration() {
        return this.appHandler.getConfiguration();
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return this.appHandler;
    }

    @Override
    public void reload() {
        reload(appHandler.getConfiguration());
    }

    @Override
    public void reload(ResourceConfig configuration) {
        appHandler.onShutdown(this);
        appHandler = new ApplicationHandler(configuration);
        appHandler.onReload(this);
        appHandler.onStartup(this);
    }

    public void handle(ContainerRequest requestContext) {
        // TODO: why can't this work with any other executor?
        getExecutorService().execute(() -> appHandler.handle(requestContext));
    }

    ExecutorService getExecutorService() {
        return appHandler.getInjectionManager().getInstance(ExecutorServiceProvider.class).getExecutorService();
    }

    ScheduledExecutorService getScheduledExecutorService() {
        return appHandler.getInjectionManager().getInstance(ScheduledExecutorServiceProvider.class).getExecutorService();
    }
}
