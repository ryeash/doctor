package vest.doctor.event;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class EventBus implements EventProducer {

    private final List<EventConsumer> consumers = new LinkedList<>();

    public void addConsumer(EventConsumer consumer) {
        consumers.add(Objects.requireNonNull(consumer));
    }

    @Override
    public void publish(Object event) {
        for (EventConsumer consumer : consumers) {
            consumer.receive(event);
        }
    }
}
