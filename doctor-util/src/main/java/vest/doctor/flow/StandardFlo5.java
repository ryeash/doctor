package vest.doctor.flow;

import vest.doctor.tuple.Tuple5;

import java.util.concurrent.Flow;

public record StandardFlo5<I, O1, O2, O3, O4, O5>(
        Flo<I, Tuple5<O1, O2, O3, O4, O5>> flow) implements Flo5<I, O1, O2, O3, O4, O5> {

    @Override
    public <N> Flo<I, N> chain(Flow.Processor<Tuple5<O1, O2, O3, O4, O5>, N> processor) {
        return flow.chain(processor);
    }

    @Override
    public SubscriptionHandle<I, Tuple5<O1, O2, O3, O4, O5>> subscribe() {
        return flow.subscribe();
    }

    @Override
    public SubscriptionHandle<I, Tuple5<O1, O2, O3, O4, O5>> subscribe(long initialRequest) {
        return flow.subscribe(initialRequest);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Tuple5<O1, O2, O3, O4, O5>> subscriber) {
        flow.subscribe(subscriber);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        flow.onSubscribe(subscription);
    }

    @Override
    public void onNext(I item) {
        flow.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        flow.onError(throwable);
    }

    @Override
    public void onComplete() {
        flow.onComplete();
    }
}
