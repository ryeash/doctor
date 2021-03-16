package vest.doctor.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.ProviderRegistry;
import vest.doctor.scheduled.Cron;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Used internally to support running scheduled methods.
 */
public final class CronTaskWrapper<T> implements Runnable {

    public static <T> void run(ProviderRegistry providerRegistry, T val, Cron cron, int executions, ScheduledExecutorService scheduledExecutorService, BiConsumer<ProviderRegistry, T> execute) {
        CronTaskWrapper<T> wrapper = new CronTaskWrapper<>(providerRegistry, val, cron, executions, scheduledExecutorService, execute);
        wrapper.scheduleNext();
    }

    private static final Logger log = LoggerFactory.getLogger(CronTaskWrapper.class);
    private final ProviderRegistry providerRegistry;
    private final WeakReference<T> ref;
    private final Cron cron;
    private final AtomicInteger executionLimit;
    private final ScheduledExecutorService scheduledExecutorService;
    private final BiConsumer<ProviderRegistry, T> execute;

    private CronTaskWrapper(ProviderRegistry providerRegistry, T val, Cron cron, int executions, ScheduledExecutorService scheduledExecutorService, BiConsumer<ProviderRegistry, T> execute) {
        this.providerRegistry = providerRegistry;
        this.ref = new WeakReference<>(val);
        this.cron = cron;
        this.executionLimit = executions > 0 ? new AtomicInteger(executions) : null;
        this.scheduledExecutorService = scheduledExecutorService;
        this.execute = execute;
    }

    @Override
    public void run() {
        T t = ref.get();
        if (t != null) {
            try {
                execute.accept(providerRegistry, t);
                if (executionLimit != null && executionLimit.decrementAndGet() == 0) {
                    ref.clear();
                    return;
                }
            } catch (Throwable error) {
                log.error("error running scheduled cron task", error);
            }
            scheduleNext();
        }
    }

    private void scheduleNext() {
        long nextExecutionTime = cron.nextFireTime();
        long delay = nextExecutionTime - System.currentTimeMillis();
        scheduledExecutorService.schedule(this, delay, TimeUnit.MILLISECONDS);
    }
}