package vest.doctor.workflow;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collector;


public class CollectingProcessor<IN, A, C> extends AbstractProcessor<IN, C> {
    private final Collector<IN, A, C> collector;
    private final List<A> allA = new LinkedList<>();
    private final ThreadLocal<A> localA;

    public CollectingProcessor(Collector<IN, A, C> collector) {
        this.collector = collector;
        this.localA = ThreadLocal.withInitial(this::getIntermediate);
    }

    @Override
    public void onNext(IN item) {
        collector.accumulator().accept(localA.get(), item);
    }

    @Override
    public void onComplete() {
        A a = allA.stream().reduce(collector.combiner()).orElseGet(collector.supplier());
        C aggregateCollected = collector.finisher().apply(a);
        allA.clear();
        localA.remove();
        publishDownstream(aggregateCollected);
        super.onComplete();
    }

    private A getIntermediate() {
        A a = collector.supplier().get();
        synchronized (allA) {
            allA.add(a);
        }
        return a;
    }
}
