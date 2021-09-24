package vest.doctor.workflow;

import java.util.concurrent.Flow;

final class CombinedProcessor<IN, OUT> implements Flow.Processor<IN, OUT> {

    private final Flow.Subscriber<IN> source;
    private final Flow.Publisher<OUT> last;

    CombinedProcessor(Flow.Subscriber<IN> source, Flow.Publisher<OUT> last) {
        this.source = source;
        this.last = last;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        source.onSubscribe(subscription);
    }

    @Override
    public void onNext(IN item) {
        source.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        source.onError(throwable);
    }

    @Override
    public void onComplete() {
        source.onComplete();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super OUT> subscriber) {
        last.subscribe(subscriber);
    }
}
