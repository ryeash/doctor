package vest.doctor.event;

public class ServiceStopped {
    private final String name;
    private final Object service;

    public ServiceStopped(String name, Object service) {
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
        return "ServiceStopped{" +
                "name='" + name + '\'' +
                ", service=" + service +
                '}';
    }
}
