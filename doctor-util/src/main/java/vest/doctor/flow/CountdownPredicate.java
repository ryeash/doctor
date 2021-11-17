package vest.doctor.flow;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

final class CountdownPredicate<IN> implements Predicate<IN> {

    private final AtomicLong counter;

    public CountdownPredicate(long n) {
        this.counter = new AtomicLong(n);
    }

    @Override
    public boolean test(IN in) {
        return counter.decrementAndGet() >= 0;
    }
}