package vest.doctor.event;

/**
 * An event indicating a specific service has started.
 */
public record ServiceStarted(String name, Object service) {
}
