package vest.doctor.flow;

import vest.doctor.tuple.Tuple4;

import java.util.concurrent.Flow;

public record StandardFlo4<I, O1, O2, O3, O4>(Flo<I, Tuple4<O1, O2, O3, O4>> flow) implements Flo4<I, O1, O2, O3, O4> {

    @Override
    public <N> Flo<I, N> chain(Flow.Processor<Tuple4<O1, O2, O3, O4>, N> processor) {
        return flow.chain(processor);
    }

    @Override
    public SubscriptionHandle<I, Tuple4<O1, O2, O3, O4>> subscribe() {
        return flow.subscribe();
    }

    @Override
    public SubscriptionHandle<I, Tuple4<O1, O2, O3, O4>> subscribe(long initialRequest) {
        return flow.subscribe(initialRequest);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Tuple4<O1, O2, O3, O4>> subscriber) {
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
