package vest.doctor.pipeline;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collector;

public class CollectingAsyncStageConsumer<IN, A, C> implements AsyncStageConsumer<IN, C> {

    private final Collector<IN, A, C> collector;
    private final List<A> allA = new LinkedList<>();
    private final ThreadLocal<A> localA;

    public CollectingAsyncStageConsumer(Collector<IN, A, C> collector) {
        this.collector = collector;
        this.localA = ThreadLocal.withInitial(this::getIntermediate);
    }

    @Override
    public void accept(Stage<IN, C> currentStage, IN value, Emitter<C> emitter) {
        collector.accumulator().accept(localA.get(), value);
    }

    @Override
    public void onComplete(Stage<IN, C> currentStage, Emitter<C> emitter) {
        A a = allA.stream().reduce(collector.combiner()).orElseGet(collector.supplier());
        C apply = collector.finisher().apply(a);
        allA.clear();
        localA.remove();
        emitter.emit(apply);
    }

    private A getIntermediate() {
        A a = collector.supplier().get();
        synchronized (allA) {
            allA.add(a);
        }
        return a;
    }
}
