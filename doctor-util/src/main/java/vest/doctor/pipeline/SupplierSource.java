package vest.doctor.pipeline;

import java.util.function.Supplier;

class SupplierSource<IN> extends AbstractSource<IN> {

    private final Supplier<IN> source;

    public SupplierSource(Supplier<IN> source) {
        this.source = source;
    }

    @Override
    protected void handleItem(IN value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void request(long n) {
        super.request(n);
        executorService.submit(this::consume);
    }

    protected void consume() {
        for (; state() == PipelineState.SUBSCRIBED && requested.get() > 0; requested.decrementAndGet()) {
            downstream.onNext(source.get());
        }
    }
}
