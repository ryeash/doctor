package vest.doctor.pipeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class IterableSource<IN> extends AbstractSource<IN> {

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
    protected void requestInternal(long n, Stage<IN, ?> requester) {
        executorService.submit(() -> iterateInternal(n, requester));
    }

    private void iterateInternal(long n, Stage<IN, ?> requester) {
        Iterator<IN> it = iterators.computeIfAbsent(requester.id(), r -> source.iterator());
        for (; n > 0; n--) {
            if (it.hasNext()) {
                IN value = it.next();
                requester.onNext(value);
            } else {
                requester.onComplete();
                iterators.remove(requester.id());
                break;
            }
        }
    }
}
