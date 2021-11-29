package vest.doctor.flow;

import vest.doctor.tuple.Tuple2;

import java.util.concurrent.Flow;

public record StandardFlo2<I, O1, O2>(Flo<I, Tuple2<O1, O2>> flow) implements Flo2<I, O1, O2> {

    @Override
    public <NEXT> Flo<I, NEXT> chain(Flow.Processor<Tuple2<O1, O2>, NEXT> processor) {
        return flow.chain(processor);
    }

    @Override
    public SubscriptionHandle<I, Tuple2<O1, O2>> subscribe() {
        return flow.subscribe();
    }

    @Override
    public SubscriptionHandle<I, Tuple2<O1, O2>> subscribe(long initialRequest) {
        return flow.subscribe(initialRequest);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Tuple2<O1, O2>> subscriber) {
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
