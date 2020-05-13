package vest.doctor.event;

/**
 * Handle to the event system. Used to publish events to {@link EventListener} methods.
 */
public interface EventProducer {

    /**
     * Publish an event. Events will be consumed by compatible {@link EventConsumer}s.
     *
     * @param event the event to publish
     */
    void publish(Object event);
}
