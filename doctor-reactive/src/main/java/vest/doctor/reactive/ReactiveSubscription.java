package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public interface ReactiveSubscription extends Flow.Subscription {
    FlowState state();

    long requested();

    long getAndDecrementRequested();

    void transition(FlowState expected, FlowState next);

    void transition(FlowState next);

    void addStateListener(FlowState state, Consumer<Flow.Subscription> action);
}
