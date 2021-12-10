package vest.doctor.event;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Primary class for supporting event publish and delivery.
 */
public final class EventBus implements EventProducer {

    private final Collection<ConsumerHolder> consumers = new ConcurrentLinkedDeque<>();

    /**
     * Add a new consumer to notify with published events.
     *
     * @param eventType the event type the consumer listens for
     * @param consumer  the action
     */
    @SuppressWarnings("unchecked")
    public <T> void addConsumer(Class<T> eventType, EventConsumer<? super T> consumer) {
        consumers.add(new ConsumerHolder(eventType, (EventConsumer<Object>) consumer));
    }

    @Override
    public void publish(Object event) {
        for (ConsumerHolder eventConsumer : consumers) {
            if (eventConsumer.type.isInstance(event)) {
                eventConsumer.consumer.accept(event);
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

    private record ConsumerHolder(Class<?> type, EventConsumer<Object> consumer) {
    }
}
