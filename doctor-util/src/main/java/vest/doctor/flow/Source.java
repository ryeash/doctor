package vest.doctor.flow;

import java.util.concurrent.Flow;

public interface Source<I> extends Flow.Processor<I, I>, Flow.Subscription {
    void startSubscription();
}
