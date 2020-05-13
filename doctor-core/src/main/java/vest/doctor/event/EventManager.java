package vest.doctor.event;

import vest.doctor.ProviderRegistry;

/**
 * Manages the {@link EventListener}s and routing of messages.
 */
public interface EventManager extends EventProducer {

    void initialize(ProviderRegistry providerRegistry);

    void register(EventConsumer eventConsumer);
}