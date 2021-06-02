package vest.doctor.pipeline;

import java.util.concurrent.CompletionStage;

final class CompletableSource<IN> extends AbstractSource<IN> {

    private final CompletionStage<IN> future;

    public CompletableSource(CompletionStage<IN> future) {
        this.future = future;
    }

    @Override
    protected void internalPublish(IN value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void request(long n) {
        if (n > 0) {
            future.whenCompleteAsync((value, error) -> {
                if (error != null) {
                    onError(error);
                } else {
                    publishDownstream(value);
                }
            }, executorService()).thenRun(this::onComplete);
        }
    }
}
