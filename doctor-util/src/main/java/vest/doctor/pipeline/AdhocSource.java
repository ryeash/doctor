package vest.doctor.pipeline;

class AdhocSource<IN> extends AbstractSource<IN> {


    @Override
    protected void handleItem(IN value) {
        stateCheck(PipelineState.SUBSCRIBED);
        if (downstream != null) {
            if (requested.get() > 0) {
                requested.decrementAndGet();
                downstream.onNext(value);
            } else {
                throw new StageException(this, null, "downstream stages have not requested any data");
            }
        }
    }
}
