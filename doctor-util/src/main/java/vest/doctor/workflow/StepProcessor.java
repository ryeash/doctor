package vest.doctor.workflow;

final class StepProcessor<IN, OUT> extends AbstractProcessor<IN, OUT> {
    private final Step<IN, OUT> step;

    StepProcessor(Step<IN, OUT> step) {
        this.step = step;
    }

    @Override
    public void onNext(IN item) {
        try {
            step.accept(item, subscription, this::publishDownstream);
        } catch (Throwable t) {
            onError(t);
        }
    }

    @Override
    public String toString() {
        return "Step(" + step.getClass().getSimpleName() + ")->" + (subscriber != null ? subscriber : "end");
    }
}
