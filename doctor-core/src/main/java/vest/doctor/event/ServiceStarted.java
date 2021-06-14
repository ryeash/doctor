package vest.doctor.event;

/**
 * An event indicating a specific service has started.
 */
public final class ServiceStarted {
    private final String name;
    private final Object service;

    public ServiceStarted(String name, Object service) {
        this.name = name;
        this.service = service;
    }

    /**
     * The name of the service.
     */
    public String getName() {
        return name;
    }

    /**
     * The service that was started.
     */
    public Object getService() {
        return service;
    }

    @Override
    public String toString() {
        return "ServiceStarted{" +
                "name='" + name + '\'' +
                ", service=" + service +
                '}';
    }
}
