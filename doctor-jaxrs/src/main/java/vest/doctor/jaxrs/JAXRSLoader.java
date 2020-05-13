package vest.doctor.jaxrs;

import vest.doctor.AppLoader;
import vest.doctor.ProviderRegistry;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ServiceStarted;
import vest.doctor.event.ServiceStopped;

public class JAXRSLoader implements AppLoader {
    private ProviderRegistry providerRegistry;
    private JAXRSServer server;

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
        server = new JAXRSServer(providerRegistry);
        providerRegistry.getInstance(EventProducer.class).publish(new ServiceStarted("jetty-jaxrs", server));
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.close();
        }
        if (providerRegistry != null) {
            providerRegistry.getInstance(EventProducer.class).publish(new ServiceStopped("jetty-jaxrs", server));
        }
    }

    @Override
    public int priority() {
        return 100000;
    }
}
