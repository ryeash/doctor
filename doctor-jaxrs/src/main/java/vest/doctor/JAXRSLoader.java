package vest.doctor;

public class JAXRSLoader implements AppLoader {
    private JAXRSServer server;

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.close();
        }
    }

    @Override
    public void load(BeanProvider beanProvider) {
    }

    @Override
    public void postProcess(BeanProvider beanProvider) {
        server = new JAXRSServer(beanProvider);
    }

    @Override
    public int priority() {
        return 100000;
    }
}
