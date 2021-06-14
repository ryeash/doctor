package vest.doctor.event;

/**
 * An event indicating a specific service has stopped.
 */
public final class ServiceStopped {
    private final String name;
    private final Object service;

    public ServiceStopped(String name, Object service) {
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
     * The service that was stopped.
     */
    public Object getService() {
        return service;
    }

    @Override
    public String toString() {
        return "ServiceStopped{" +
                "name='" + name + '\'' +
                ", service=" + service +
                '}';
    }
}
