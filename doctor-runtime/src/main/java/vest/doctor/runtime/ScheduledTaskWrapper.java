package vest.doctor.runtime;

import vest.doctor.ProviderRegistry;
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

    public static <T> void run(ProviderRegistry providerRegistry, T val, int executions, Interval interval, ScheduledExecutorService ses, boolean fixedRate, BiConsumer<ProviderRegistry, T> execute) {
        ScheduledTaskWrapper<T> wrapper = new ScheduledTaskWrapper<>(providerRegistry, val, executions, execute);
        if (fixedRate) {
            wrapper.future = ses.scheduleAtFixedRate(wrapper, interval.getMagnitude(), interval.getMagnitude(), interval.getUnit());
        } else {
            wrapper.future = ses.scheduleWithFixedDelay(wrapper, interval.getMagnitude(), interval.getMagnitude(), interval.getUnit());
        }
    }

    private final ProviderRegistry providerRegistry;
    private final WeakReference<T> ref;
    private final AtomicInteger executionLimit;
    private final BiConsumer<ProviderRegistry, T> execute;
    private ScheduledFuture<?> future;

    private ScheduledTaskWrapper(ProviderRegistry providerRegistry, T val, int executions, BiConsumer<ProviderRegistry, T> execute) {
        this.providerRegistry = providerRegistry;
        this.ref = new WeakReference<>(val);
        this.execute = execute;
        this.executionLimit = executions > 0 ? new AtomicInteger(executions) : null;
    }

    @Override
    public void run() {
        T t = ref.get();
        if (t != null) {
            execute.accept(providerRegistry, t);
            if (executionLimit != null && executionLimit.decrementAndGet() <= 0) {
                ref.clear();
                cancel();
            }
        } else {
            cancel();
        }
    }

    private void cancel() {
        if (future != null) {
            future.cancel(true);
        }
    }
}