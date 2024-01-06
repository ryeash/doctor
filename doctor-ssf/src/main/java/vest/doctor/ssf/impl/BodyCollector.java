package vest.doctor.ssf.impl;

import vest.doctor.rx.AbstractProcessor;
import vest.doctor.ssf.HttpData;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.stream.Collector;

public class BodyCollector<A, O> extends AbstractProcessor<HttpData, O> {

    public static final Collector<HttpData, List<HttpData>, byte[]> TO_BYTES = Collector.of(
            LinkedList::new,
            List::add,
            (a, b) -> b,
            list -> {
                int total = list.stream().mapToInt(d -> d.bytes().remaining()).sum();
                byte[] bytes = new byte[total];
                int i = 0;
                for (HttpData httpData : list) {
                    int remaining = httpData.bytes().remaining();
                    httpData.bytes().get(bytes, i, remaining);
                    i += remaining;
                }
                return bytes;
            }
    );

    private final Collector<? super HttpData, A, ? extends O> collector;
    private final A container;
    private final boolean concurrent;

    public BodyCollector(Collector<? super HttpData, A, ? extends O> collector) {
        this.collector = Objects.requireNonNull(collector);
        this.container = collector.supplier().get();
        this.concurrent = collector.characteristics().contains(Collector.Characteristics.CONCURRENT);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.println("BODY COLL SUBSCRIBE");
        try {
            super.onSubscribe(subscription);
            subscription.request(Long.MAX_VALUE);
        }catch(Throwable t){
            t.printStackTrace();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(HttpData item) {
        System.out.println("NEXT BODY");
        if (concurrent) {
            collector.accumulator().accept(container, item);
        } else {
            synchronized (container) {
                collector.accumulator().accept(container, item);
            }
        }

        if (item.last()) {
            System.out.println("LAST BODY");
            O collected;
            if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                collected = (O) container;
            } else {
                collected = collector.finisher().apply(container);
            }
            if (subscription != null) {
                subscriber.onNext(collected);
            }
            super.onComplete();
        }
    }

    @Override
    public void onComplete() {
        // ignored, trust the 'last' flag on the HttpData
    }
}