package vest.doctor.event;

/**
 * An event indicating a specific service has started.
 */
public class ServiceStarted {
    private final String name;
    private final Object service;

    public ServiceStarted(String name, Object service) {
        this.name = name;
        this.service = service;
    }

    public String getName() {
        return name;
    }

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
