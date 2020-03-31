package vest.doctor;

/**
 * Interface defining a class that can consume events published using {@link EventProducer#publish(Object)}
 */
public interface EventConsumer {

    /**
     * Determine if this consumer is compatible with the given message instance.
     *
     * @param event the event to check
     * @return true if the event is okay to pass to the {@link #accept(Object)} method
     */
    boolean isCompatible(Object event);

    /**
     * Accept a new message object published to the {@link EventProducer}
     *
     * @param event the event that was published
     */
    void accept(Object event);

    /**
     * Determines if the {@link #accept(Object)} method of this class is called asynchronously.
     *
     * @return true if the accept method should be executed in a background thread, else the accept
     * method is called in the same thread that produced the message
     */
    default boolean async() {
        return false;
    }

}
