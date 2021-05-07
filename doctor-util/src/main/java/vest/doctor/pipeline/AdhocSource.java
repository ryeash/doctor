package vest.doctor.pipeline;

class AdhocSource<IN> extends AbstractSource<IN> {

    @Override
    public void internalPublish(IN value) {
        if (downstream != null) {
            if (requested.get() > 0) {
                requested.decrementAndGet();
                downstream.onNext(value);
            } else {
                throw new StageException(this, "downstream stages have not requested any data");
            }
        }
    }
}
