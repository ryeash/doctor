package vest.doctor.workflow;

final class StepProcessor<IN, OUT> extends AbstractProcessor<IN, OUT> {
    private final Step<IN, OUT> action;

    StepProcessor(Step<IN, OUT> action) {
        this.action = action;
    }

    @Override
    public void onNext(IN item) {
        try {
            action.accept(item, subscription, this::publishDownstream);
        } catch (Throwable t) {
            onError(t);
        }
    }

    @Override
    public String toString() {
        return "Step(" + action.getClass().getSimpleName() + ")->" + (subscriber != null ? subscriber : "end");
    }
}
