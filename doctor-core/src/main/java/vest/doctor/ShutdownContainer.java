package vest.doctor;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownContainer implements AutoCloseable {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Set<AutoCloseable> cleanupObjects = Collections.newSetFromMap(new WeakHashMap<>(32));

    public void register(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        synchronized (closed) {
            if (closed.get()) {
                closeQuietly(closeable);
                return;
            }
            cleanupObjects.add(closeable);
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            synchronized (closed) {
                cleanupObjects.forEach(ShutdownContainer::closeQuietly);
                cleanupObjects.clear();
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
}