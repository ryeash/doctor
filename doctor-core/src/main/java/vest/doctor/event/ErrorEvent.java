package vest.doctor.event;

/**
 * An event that can be published to indicate an exception occurred.
 */
public record ErrorEvent(Throwable error) {
}
