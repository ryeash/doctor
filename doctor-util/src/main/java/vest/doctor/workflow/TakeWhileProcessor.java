package vest.doctor.workflow;

import java.util.function.Predicate;

public class TakeWhileProcessor<IN> extends AbstractProcessor<IN, IN> {

    private final Predicate<IN> takeWhileTrue;
    private final boolean includeLast;

    public TakeWhileProcessor(Predicate<IN> takeWhileTrue, boolean includeLast) {
        this.takeWhileTrue = takeWhileTrue;
        this.includeLast = includeLast;
    }

    @Override
    public void onNext(IN item) {
        if (takeWhileTrue.test(item)) {
            publishDownstream(item);
        } else {
            if (includeLast) {
                publishDownstream(item);
            }
            subscription.cancel();
        }
    }
}
