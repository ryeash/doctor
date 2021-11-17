package vest.doctor.flow;

import java.util.concurrent.Flow;

/**
 * A source for values into a processing flow.
 *
 * @param <I> the type of values produced by the source
 */
public interface Source<I> extends Flow.Processor<I, I>, Flow.Subscription {

    /**
     * Trigger execution of the subscription. Called by the processing flow during execution
     * of {@link Flo#subscribe(long)}.
     */
    void startSubscription();
}
