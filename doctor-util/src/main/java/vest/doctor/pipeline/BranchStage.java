package vest.doctor.pipeline;

import java.util.Objects;

final class BranchStage<IN> extends AbstractStage<IN, IN> {

    private final Stage<IN, ?> branch;

    public BranchStage(Stage<?, IN> upstream, Stage<IN, ?> branch) {
        super(upstream);
        this.branch = Objects.requireNonNull(branch);
    }

    @Override
    public void internalPublish(IN value) {
        branch.executorService().submit(() -> branch.onNext(value));
        publishDownstream(value);
    }

    @Override
    public void onComplete() {
        branch.executorService().submit(branch::onComplete);
        super.onComplete();
    }
}
