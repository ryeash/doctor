package vest.doctor;

import javax.inject.Provider;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class EventManagerImpl implements EventManager {

    private final List<EventConsumer> consumers = new LinkedList<>();
    private final List<EventConsumer> asyncConsumers = new LinkedList<>();

    private ExecutorService executor;

    @Override
    public void initialize(ProviderRegistry providerRegistry) {
        this.executor = providerRegistry.getInstance(ExecutorService.class, BuiltInAppLoader.DEFAULT_EXECUTOR_NAME);
        providerRegistry.getProviders(EventConsumer.class)
                .map(Provider::get)
                .forEach(this::register);
    }

    @Override
    public void register(EventConsumer eventConsumer) {
        Objects.requireNonNull(eventConsumer);
        if (eventConsumer.async()) {
            asyncConsumers.add(eventConsumer);
        } else {
            consumers.add(eventConsumer);
        }
    }

    @Override
    public void publish(Object event) {
        for (EventConsumer consumer : consumers) {
            if (consumer.isCompatible(event)) {
                consumer.accept(event);
            }
        }
        for (EventConsumer asyncConsumer : asyncConsumers) {
            if (asyncConsumer.isCompatible(event)) {
                executor.submit(() -> asyncConsumer.accept(event));
            }
        }
    }
}
