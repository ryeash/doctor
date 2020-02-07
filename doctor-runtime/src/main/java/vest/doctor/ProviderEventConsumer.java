package vest.doctor;

import java.util.function.BiConsumer;

public final class ProviderEventConsumer<T, E> implements EventConsumer {

    private final Class<E> eventType;
    private final DoctorProvider<T> provider;
    private final BiConsumer<T, ? super E> methodCall;

    public ProviderEventConsumer(Class<E> eventType, DoctorProvider<T> provider, BiConsumer<T, E> methodCall) {
        this.eventType = eventType;
        this.provider = provider;
        this.methodCall = methodCall;
    }

    @Override
    public boolean isCompatible(Object event) {
        return eventType.isInstance(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void accept(Object event) {
        methodCall.accept(provider.get(), (E) event);
    }
}
