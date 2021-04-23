package vest.doctor.pipeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IterableSource<IN> extends Source<IN> {

    private final Iterable<IN> source;
    private final Map<Integer, Iterator<IN>> iterators;

    public IterableSource(Iterable<IN> source) {
        super();
        this.source = source;
        this.iterators = new HashMap<>();
    }

    @Override
    public void internalPublish(IN value) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void requestInternal(long n, Pipeline<IN, ?> requester) {
        Iterator<IN> it = iterators.computeIfAbsent(requester.id(), r -> source.iterator());
        for (long i = 0; i < n; i++) {
            if (it.hasNext()) {
                IN value = it.next();
                if (executorService != null) {
                    executorService.submit(() -> requester.publish(value));
                } else {
                    requester.publish(value);
                }
            } else {
                requester.onComplete();
                iterators.remove(requester.id());
                break;
            }
        }
    }
}
