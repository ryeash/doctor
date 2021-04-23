package vest.doctor.pipeline;

public class AdhocSource<IN> extends Source<IN> {

    @Override
    public void internalPublish(IN value) {
        for (Pipeline<IN, ?> p : downstream) {
            if (executorService != null) {
                executorService.submit(() -> p.publish(value));
            } else {
                p.publish(value);
            }
        }
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
        // no-op
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO
        throw new RuntimeException("error in pipeline", throwable);
    }
}
