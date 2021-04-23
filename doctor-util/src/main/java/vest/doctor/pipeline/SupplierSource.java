package vest.doctor.pipeline;

import java.util.function.Supplier;

public class SupplierSource<IN> extends AbstractPipeline<IN, IN> {

    private final Supplier<IN> source;

    public SupplierSource(Supplier<IN> source) {
        super(null);
        this.source = source;
    }

    @Override
    public void internalPublish(IN value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsubscribe() {
        // no-op
    }

    @Override
    public void request(long n) {
        // TODO
    }

    @Override
    protected void requestInternal(long n, Pipeline<IN, ?> requester) {
        for (long i = 0; i < n; i++) {
            requester.publish(source.get());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO
        throw new RuntimeException("error in pipeline", throwable);
    }
}
