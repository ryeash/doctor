package vest.doctor;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ScheduledTaskWrapper<T> implements Runnable {
    private final BeanProvider beanProvider;
    private final WeakReference<T> ref;
    private final AtomicInteger executionLimit;
    private ScheduledFuture<?> future;

    public ScheduledTaskWrapper(BeanProvider beanProvider, T val, int executionLimit) {
        this.beanProvider = beanProvider;
        this.ref = new WeakReference<>(val);
        if (executionLimit > 0) {
            this.executionLimit = new AtomicInteger(executionLimit);
        } else {
            this.executionLimit = null;
        }
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    @Override
    public void run() {
        T t = ref.get();
        if (t != null) {
            internalRun(beanProvider, t);
            if (executionLimit != null && executionLimit.decrementAndGet() == 0) {
                ref.clear();
            }
        } else {
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    protected abstract void internalRun(BeanProvider beanProvider, T val);
}