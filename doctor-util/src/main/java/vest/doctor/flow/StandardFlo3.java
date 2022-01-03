package vest.doctor.flow;

import vest.doctor.tuple.Tuple3;

import java.util.concurrent.Flow;

public record StandardFlo3<I, O1, O2, O3>(Flo<I, Tuple3<O1, O2, O3>> flow) implements Flo3<I, O1, O2, O3> {

    @Override
    public <N> Flo<I, N> chain(Flow.Processor<Tuple3<O1, O2, O3>, N> processor) {
        return flow.chain(processor);
    }

    @Override
    public SubscriptionHandle<I, Tuple3<O1, O2, O3>> subscribe() {
        return flow.subscribe();
    }

    @Override
    public SubscriptionHandle<I, Tuple3<O1, O2, O3>> subscribe(long initialRequest) {
        return flow.subscribe(initialRequest);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Tuple3<O1, O2, O3>> subscriber) {
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
