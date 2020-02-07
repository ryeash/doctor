package vest.doctor;

/**
 * Manages the {@link EventListener}s and routing of messages.
 */
public interface EventManager extends EventProducer {

    void initialize(ProviderRegistry providerRegistry);

    default void register(EventConsumer eventConsumer) {
        register(eventConsumer, false);
    }

    void register(EventConsumer eventConsumer, boolean async);
}
