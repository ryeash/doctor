package doctor.atomic;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ManagedSemaphore extends AbstractManagedAccess {
    private final Semaphore semaphore;

    public ManagedSemaphore(int maxPermits) {
        this.semaphore = new Semaphore(maxPermits);
    }

    @Override
    protected final void acquire(long acquireTimeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        boolean acquired = semaphore.tryAcquire(acquireTimeout, unit);
        if (!acquired) {
            throw new TimeoutException("permit acquisition timed out after " + acquireTimeout + " " + unit);
        }
    }

    @Override
    protected final void release() {
        semaphore.release();
    }
}
