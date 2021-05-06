package vest.doctor.pipeline;

class AdhocSource<IN> extends AbstractSource<IN> {

    private final Class<IN> type;

    public AdhocSource(Class<IN> type) {
        this.type = type;
    }

    @Override
    public void internalPublish(IN value) {
        if (!type.isInstance(value)) {
            throw new ClassCastException();
        }
        requested.replaceAll((id, n) -> {
            if (n <= 0) {
                return 0L;
            }
            executorService.submit(() -> {
                downstream.get(id).onNext(value);
            });
            return n - 1;
        });
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO
        throw new RuntimeException("error in pipeline", throwable);
    }
}
