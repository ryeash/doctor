package vest.doctor.flow;

import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A basic step in a workflow. Does not deal with any control signals for the processing flow,
 * just items in and out.
 *
 * @param <IN>  the input type into the step
 * @param <OUT> the output type from the step
 */
@FunctionalInterface
public interface Step<IN, OUT> {

    /**
     * Accept and process an item.
     *
     * @param item         the item
     * @param subscription the {@link Flow.Subscription}
     * @param emitter      the emitter for outputting values to downstream {@link Flow.Subscriber subscribers}
     */
    void accept(IN item, Flow.Subscription subscription, Consumer<OUT> emitter) throws Exception;

    record Observer<IN>(Consumer<IN> action) implements Step<IN, IN> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Consumer<IN> emitter) {
            action.accept(in);
            emitter.accept(in);
        }
    }

    record Mapper<IN, OUT>(Function<IN, OUT> mapper) implements Step<IN, OUT> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Consumer<OUT> emitter) {
            OUT out = mapper.apply(in);
            emitter.accept(out);
        }
    }

    record Filter<IN>(Predicate<IN> predicate, boolean keep) implements Step<IN, IN> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Consumer<IN> emitter) {
            boolean test = predicate.test(in);
            if (test == keep) {
                emitter.accept(in);
            }
        }
    }
}
