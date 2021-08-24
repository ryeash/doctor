package vest.doctor.pipeline;

import java.util.Iterator;

class IterableSource<IN> extends AbstractSource<IN> {

    private final Iterator<IN> source;

    public IterableSource(Iterable<IN> source) {
        super();
        this.source = source.iterator();
    }

    @Override
    protected void handleItem(IN value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void request(long n) {
        executorService.submit(() -> iterateInternal(n));
    }

    private void iterateInternal(long n) {
        for (; state() == PipelineState.SUBSCRIBED && n > 0; n--) {
            if (source.hasNext()) {
                IN value = source.next();
                downstream.onNext(value);
            } else {
                onComplete();
                break;
            }
        }
    }
}
