package vest.doctor.event;

/**
 * Handle to the event system. Used to publish events to {@link EventConsumer}s.
 */
public interface EventProducer {

    /**
     * Publish an event. Events will be consumed by compatible {@link EventConsumer}s.
     *
     * @param event the event to publish
     */
    void publish(Object event);
}
