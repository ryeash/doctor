package vest.doctor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.scheduled.Cron;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Used internally to support running scheduled methods.
 */
public abstract class CronTaskWrapper<T> implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(CronTaskWrapper.class);
    private final ProviderRegistry providerRegistry;
    private final WeakReference<T> ref;
    private final Cron cron;
    private final ScheduledExecutorService scheduledExecutorService;

    public CronTaskWrapper(ProviderRegistry providerRegistry, T val, Cron cron, ScheduledExecutorService scheduledExecutorService) {
        this.providerRegistry = providerRegistry;
        this.ref = new WeakReference<>(val);
        this.cron = cron;
        this.scheduledExecutorService = scheduledExecutorService;
        scheduleNext();
    }

    @Override
    public void run() {
        T t = ref.get();
        if (t != null) {
            try {
                internalRun(providerRegistry, t);
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

    protected abstract void internalRun(ProviderRegistry providerRegistry, T val);
}