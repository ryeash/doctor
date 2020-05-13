package vest.doctor.jaxrs;

import vest.doctor.AppLoader;
import vest.doctor.EventProducer;
import vest.doctor.ProviderRegistry;

public class JAXRSLoader implements AppLoader {
    private ProviderRegistry providerRegistry;
    private JAXRSServer server;

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
        server = new JAXRSServer(providerRegistry);
        providerRegistry.getInstance(EventProducer.class).publish(new ServerStarted());
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.close();
        }
        if (providerRegistry != null) {
            providerRegistry.getInstance(EventProducer.class).publish(new ServerStopped());
        }
    }

    @Override
    public int priority() {
        return 100000;
    }
}
