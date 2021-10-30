package vest.doctor.workflow;

final class ErrorSource<IN> extends AbstractSource<IN> {

    private final Throwable error;

    public ErrorSource(Throwable error) {
        this.error = error;
    }

    @Override
    public void onNext(IN value) {
        throw new UnsupportedOperationException("error sources do not accept additional items");
    }

    @Override
    public void request(long n) {
        executorService.submit(() -> onError(error));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComplete() {
        throw new UnsupportedOperationException("error sources on not complete-able");
    }
}
