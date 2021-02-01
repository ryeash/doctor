package doctor.atomic;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A wrapper around a {@link Lock}, adding lambda support for managed usage of the lock.
 */
public final class ManagedLock extends AbstractManagedAccess {
    private final Lock lock;

    /**
     * Create a new ManagedLock that uses a {@link ReentrantLock}.
     */
    public ManagedLock() {
        this(new ReentrantLock());
    }

    /**
     * Create a new ManagedLock wrapping the given {@link Lock} instance.
     *
     * @param lock the lock instance to wrap
     */
    public ManagedLock(Lock lock) {
        this.lock = Objects.requireNonNull(lock);
    }

    @Override
    protected void acquire(long acquireTimeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        boolean locked = lock.tryLock(acquireTimeout, unit);
        if (!locked) {
            throw new TimeoutException("lock acquisition timed out after " + acquireTimeout + " " + unit);
        }
    }

    @Override
    protected void release() {
        lock.unlock();
    }
}
