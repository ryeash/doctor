package vest.doctor.event;

import jakarta.inject.Provider;

/**
 * Handle to the event system. Used to publish events to {@link EventConsumer EventConsumers},
 * and add new event consumers via {@link #addConsumer(Class, EventConsumer)}.
 */
public interface EventBus {

    /**
     * Publish an event. Events will be consumed by compatible {@link EventConsumer EventConsumers}.
     *
     * @param event the event object to publish
     */
    void publish(Object event);

    /**
     * Add a new event consumer that will be notified when type compatible events are published.
     *
     * @param eventType the event type the consumer listens for
     * @param consumer  the event consumer
     */
    <T> void addConsumer(Class<T> eventType, EventConsumer<? super T> consumer);

    /**
     * Add a new event consumer that will be notified when type compatible events are published.
     *
     * @param eventType the event type the consumer listens for
     * @param provider  the provider for the event consumer
     */
    <T> void addConsumer(Class<T> eventType, Provider<? extends EventConsumer<? super T>> provider);
}
