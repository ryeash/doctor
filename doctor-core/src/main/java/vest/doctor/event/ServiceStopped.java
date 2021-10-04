package vest.doctor.event;

/**
 * An event indicating a specific service has stopped.
 */
public record ServiceStopped(String name, Object service) {
}
