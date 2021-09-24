package vest.doctor.workflow;

import java.util.concurrent.Flow;

@FunctionalInterface
public interface ErrorHandler<T> {
    void handle(Throwable t, Flow.Subscription subscription, Emitter<T> emitter);
}
