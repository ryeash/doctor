package vest.doctor.event;

/**
 * Defines an object that can consume events produced via {@link EventBus#publish(Object)}.
 */
public interface EventConsumer<T> {

    /**
     * Receive a published even.
     *
     * @param event the event
     */
    void accept(T event);
}
