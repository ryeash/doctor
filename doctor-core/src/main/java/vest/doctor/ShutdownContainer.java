package vest.doctor;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used internally to support auto-closing of provided instances.
 */
public final class ShutdownContainer implements AutoCloseable {

    private static final int EXPUNGE_THRESHOLD = 64;
    private static final ReferenceQueue<AutoCloseable> queue = new ReferenceQueue<>();
    private static final Set<Ref> cSet = Collections.newSetFromMap(new ConcurrentHashMap<>(EXPUNGE_THRESHOLD * 2, 0.9F, 4));

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger counter = new AtomicInteger(0);

    public void register(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        if (closed.get()) {
            closeQuietly(closeable);
            return;
        }
        cSet.add(new Ref(closeable, queue, cSet));
        if (cSet.size() > EXPUNGE_THRESHOLD
                && counter.incrementAndGet() % EXPUNGE_THRESHOLD == 0) {
            expunge();
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            synchronized (cSet) {
                for (Ref ref : cSet) {
                    closeQuietly(ref.get());
                }
                cSet.clear();
            }
        }
    }

    private static void closeQuietly(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Throwable t) {
                // ignored
            }
        }
    }

    private void expunge() {
        Ref ref;
        while ((ref = (Ref) queue.poll()) != null) {
            ref.cleanup();
        }
    }

    private static final class Ref extends WeakReference<AutoCloseable> {

        private final Set<Ref> home;

        public Ref(AutoCloseable referent, ReferenceQueue<? super AutoCloseable> q, Set<Ref> home) {
            super(referent, q);
            this.home = home;
        }

        public void cleanup() {
            home.remove(this);
        }
    }
}