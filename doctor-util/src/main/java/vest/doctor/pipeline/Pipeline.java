package vest.doctor.pipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface Pipeline<IN, OUT> extends Flow.Subscription, Flow.Processor<IN, OUT> {

    int id();

    Pipeline<IN, OUT> publish(IN value);

    default Pipeline<OUT, OUT> observe(Pipeline<OUT, OUT> pipeline) {
        return observe(pipeline::publish);
    }

    default Pipeline<OUT, OUT> observe(Consumer<OUT> observer) {
        return observe((pipe, input) -> observer.accept(input));
    }

    default Pipeline<OUT, OUT> observe(BiConsumer<Pipeline<OUT, OUT>, OUT> observer) {
        return map((pipe, input) -> {
            observer.accept(pipe, input);
            return input;
        });
    }

    default <R> Pipeline<OUT, R> map(Function<OUT, R> function) {
        return map((pipe, input) -> function.apply(input));
    }

    <R> Pipeline<OUT, R> map(BiFunction<Pipeline<OUT, R>, OUT, R> function);

    default <R> Pipeline<OUT, R> flatStream(Function<OUT, Stream<R>> function) {
        return flatStream((pipe, input) -> function.apply(input));
    }

    default <R> Pipeline<OUT, R> flatStream(BiFunction<Pipeline<OUT, R>, OUT, Stream<R>> function) {
        BiFunction<Pipeline<OUT, R>, OUT, Iterable<R>> f = function.andThen(s -> s::iterator);
        return flatMap(f);
    }

    default <R> Pipeline<OUT, R> flatMap(Function<OUT, Iterable<R>> function) {
        return flatMap((pipe, input) -> function.apply(input));
    }

    <R> Pipeline<OUT, R> flatMap(BiFunction<Pipeline<OUT, R>, OUT, Iterable<R>> function);

    default Pipeline<OUT, OUT> filter(Predicate<OUT> predicate) {
        return filter((pipe, value) -> predicate.test(value));
    }

    Pipeline<OUT, OUT> filter(BiPredicate<Pipeline<OUT, OUT>, OUT> predicate);

    default <R> Pipeline<R, R> aggregate(Function<OUT, R> function) {
        return aggregate((pipe, value) -> function.apply(value));
    }

    default <R> Pipeline<R, R> aggregate(BiFunction<Pipeline<OUT, R>, OUT, R> function) {
        return map(function).filter((pipe, value) -> value != null);
    }

    <R, A> Pipeline<OUT, R> collect(Collector<OUT, A, R> collector);

    default Pipeline<IN, OUT> forward(Consumer<Pipeline<IN, OUT>> action) {
        action.accept(this);
        return this;
    }

    void unsubscribe();

    Pipeline<IN, OUT> async(ExecutorService executorService);

    CompletableFuture<Void> completionFuture();
}
