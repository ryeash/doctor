package vest.doctor;

import vest.doctor.scheduled.Interval;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Used internally to support running scheduled methods.
 */
public final class ScheduledTaskWrapper<T> implements Runnable {
    private final ProviderRegistry providerRegistry;
    private final WeakReference<T> ref;
    private final AtomicInteger executionLimit;
    private final ScheduledFuture<?> future;
    private final BiConsumer<ProviderRegistry, T> execute;

    public ScheduledTaskWrapper(ProviderRegistry providerRegistry, T val, int executions, Interval interval, ScheduledExecutorService ses, boolean fixedRate, BiConsumer<ProviderRegistry, T> execute) {
        this.providerRegistry = providerRegistry;
        this.ref = new WeakReference<>(val);
        this.execute = execute;
        this.executionLimit = executions > 0 ? new AtomicInteger(executions) : null;
        if (fixedRate) {
            this.future = ses.scheduleAtFixedRate(this, interval.getMagnitude(), interval.getMagnitude(), interval.getUnit());
        } else {
            this.future = ses.scheduleWithFixedDelay(this, interval.getMagnitude(), interval.getMagnitude(), interval.getUnit());
        }
    }

    @Override
    public void run() {
        T t = ref.get();
        if (t != null) {
            execute.accept(providerRegistry, t);
            if (executionLimit != null && executionLimit.decrementAndGet() == 0) {
                ref.clear();
            }
        } else {
            if (future != null) {
                future.cancel(true);
            }
        }
    }
}