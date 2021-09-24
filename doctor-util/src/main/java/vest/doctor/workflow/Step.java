package vest.doctor.workflow;

import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple3Consumer;

import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@FunctionalInterface
public interface Step<IN, OUT> {

    void accept(IN item, Flow.Subscription subscription, Emitter<OUT> emitter);

    record Observer1<IN>(Consumer<IN> action) implements Step<IN, IN> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<IN> emitter) {
            action.accept(in);
            emitter.emit(in);
        }
    }

    record Observer2<IN>(BiConsumer<IN, Flow.Subscription> action) implements Step<IN, IN> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<IN> emitter) {
            action.accept(in, subscription);
            emitter.emit(in);
        }
    }

    record Mapper1<IN, OUT>(Function<IN, OUT> mapper) implements Step<IN, OUT> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<OUT> emitter) {
            OUT out = mapper.apply(in);
            emitter.emit(out);
        }
    }

    record Mapper2<IN, OUT>(BiFunction<IN, Flow.Subscription, OUT> mapper) implements Step<IN, OUT> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<OUT> emitter) {
            OUT out = mapper.apply(in, subscription);
            emitter.emit(out);
        }
    }

    record StreamFlatMapper1<IN, OUT>(Function<IN, Stream<OUT>> mapper) implements Step<IN, OUT> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<OUT> emitter) {
            mapper.apply(in).forEach(emitter::emit);
        }
    }

    record StreamFlatMapper2<IN, OUT>(BiFunction<IN, Flow.Subscription, Stream<OUT>> mapper) implements Step<IN, OUT> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<OUT> emitter) {
            mapper.apply(in, subscription).forEach(emitter::emit);
        }
    }

    record Filter1<IN>(Predicate<IN> predicate, boolean keep) implements Step<IN, IN> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<IN> emitter) {
            boolean test = predicate.test(in);
            if (test && keep) {
                emitter.emit(in);
            }
        }
    }

    record Filter2<IN>(BiPredicate<IN, Flow.Subscription> predicate, boolean keep) implements Step<IN, IN> {
        @Override
        public void accept(IN in, Flow.Subscription subscription, Emitter<IN> emitter) {
            boolean test = predicate.test(in, subscription);
            if (test && keep) {
                emitter.emit(in);
            }
        }
    }

    record VarArgs1Step<A, IN, OUT>(A attached1,
                                    Tuple3Consumer<Tuple2<A, IN>, Flow.Subscription, Emitter<OUT>> action) implements Step<IN, OUT> {

        @Override
        public void accept(IN item, Flow.Subscription subscription, Emitter<OUT> emitter) {
            action.accept(Tuple.of(attached1, item), subscription, emitter);
        }
    }

    record VarArgs2Step<A, B, IN, OUT>(A attached1,
                                       B attached2,
                                       Tuple3Consumer<Tuple3<A, B, IN>, Flow.Subscription, Emitter<OUT>> action) implements Step<IN, OUT> {

        @Override
        public void accept(IN item, Flow.Subscription subscription, Emitter<OUT> emitter) {
            action.accept(Tuple.of(attached1, attached2, item), subscription, emitter);
        }
    }
}
