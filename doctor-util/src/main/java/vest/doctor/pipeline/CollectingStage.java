package vest.doctor.pipeline;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collector;

class CollectingStage<IN, A, C> extends AbstractStage<IN, C> {

    private final Collector<IN, A, C> collector;
    private final List<A> allA = new LinkedList<>();
    private final ThreadLocal<A> localA;

    public CollectingStage(Stage<?, IN> upstream, Collector<IN, A, C> collector) {
        super(upstream);
        this.collector = collector;
        this.localA = ThreadLocal.withInitial(this::getIntermediate);
        future().thenAccept(this::finish);
    }

    @Override
    protected void internalPublish(IN value) {
        collector.accumulator().accept(localA.get(), value);
    }

    @Override
    public void onComplete() {
        future().complete(null);
    }

    private A getIntermediate() {
        A a = collector.supplier().get();
        synchronized (allA) {
            allA.add(a);
        }
        return a;
    }

    private void finish(Void nothing) {
        A a = allA.stream().reduce(collector.combiner()).orElseGet(collector.supplier());
        C apply = collector.finisher().apply(a);
        allA.clear();
        localA.remove();
        publishDownstream(apply);
        if (downstream != null) {
            downstream.onComplete();
        }
    }
}