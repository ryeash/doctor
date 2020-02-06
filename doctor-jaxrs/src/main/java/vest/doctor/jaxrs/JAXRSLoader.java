package vest.doctor.jaxrs;

import vest.doctor.AppLoader;
import vest.doctor.ProviderRegistry;

public class JAXRSLoader implements AppLoader {
    private JAXRSServer server;

    @Override
    public void postProcess(ProviderRegistry providerRegistry) {
        server = new JAXRSServer(providerRegistry);
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @Override
    public int priority() {
        return 100000;
    }
}
