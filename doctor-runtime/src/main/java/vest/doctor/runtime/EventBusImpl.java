package vest.doctor.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.event.ErrorEvent;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventConsumer;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

final class EventBusImpl implements EventBus, EventConsumer<ErrorEvent> {

    private static final Logger log = LoggerFactory.getLogger(EventBusImpl.class);
    private final Collection<ConsumerHolder> consumers = new ConcurrentLinkedDeque<>();

    EventBusImpl() {
        addConsumer(ErrorEvent.class, this);
    }

    @Override
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

    @Override
    public void accept(ErrorEvent event) {
        log.error("error published to event bus", event.error());
    }

    private record ConsumerHolder(Class<?> type, EventConsumer<Object> consumer) {
    }
}
