package vest.doctor;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledFuture;

public abstract class ScheduledTaskWrapper<T> implements Runnable {
    private final BeanProvider beanProvider;
    private final WeakReference<T> ref;
    private ScheduledFuture<?> future;

    public ScheduledTaskWrapper(BeanProvider beanProvider, T val) {
        this.beanProvider = beanProvider;
        this.ref = new WeakReference<>(val);
    }

    public void setFuture(ScheduledFuture<?> future) {
        this.future = future;
    }

    @Override
    public void run() {
        T t = ref.get();
        if (t != null) {
            internalRun(beanProvider, t);
        } else {
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    protected abstract void internalRun(BeanProvider beanProvider, T val);
}