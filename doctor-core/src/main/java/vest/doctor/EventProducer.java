package vest.doctor;

/**
 * Handle to the event system. Used to publish events to {@link EventListener} methods.
 */
public interface EventProducer {

    void publish(Object event);
}
