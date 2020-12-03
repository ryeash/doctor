package vest.doctor.event;

/**
 * Defines an object that can consume events produced via {@link EventProducer#publish(Object)}.
 */
public interface EventConsumer {

    /**
     * Receive a published even.
     *
     * @param event the event
     */
    void receive(Object event);
}
