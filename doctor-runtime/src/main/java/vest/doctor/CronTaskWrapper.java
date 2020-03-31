package vest.doctor;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Used internally to support running scheduled methods.
 */
public abstract class CronTaskWrapper<T> implements Runnable {
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
            internalRun(providerRegistry, t);
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