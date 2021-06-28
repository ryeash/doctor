package vest.doctor.event;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Primary class for supporting event publish and delivery.
 */
public final class EventBus implements EventProducer {

    private final Collection<ConsumerHolder<?>> consumers = new ConcurrentLinkedDeque<>();

    /**
     * Add a new consumer to notify with published events.
     *
     * @param eventType the event type the consumer listens for
     * @param consumer  the action
     */
    public <T> void addConsumer(Class<T> eventType, EventConsumer<T> consumer) {
        consumers.add(new ConsumerHolder<>(eventType, consumer));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(Object event) {
        for (ConsumerHolder eventConsumer : consumers) {
            if (eventConsumer.type.isInstance(event)) {
                eventConsumer.consumer.receive(event);
            }
        }
    }

    @Override
    public String toString() {
        return "EventBus{consumers: [" +
                consumers.stream()
                        .sorted(Comparator.comparing(h -> h.type.getName()))
                        .map(holder -> holder.type.getName() + ": " + holder.consumer)
                        .collect(Collectors.joining("\n")) +
                "]}";
    }

    private static final class ConsumerHolder<T> {
        private final Class<T> type;
        private final EventConsumer<?> consumer;

        private ConsumerHolder(Class<T> type, EventConsumer<T> consumer) {
            this.type = type;
            this.consumer = consumer;
        }
    }
}
