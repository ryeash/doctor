package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.atomic.ManagedLock;
import vest.doctor.atomic.ManagedResource;
import vest.doctor.atomic.ManagedSemaphore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Test(invocationCount = 5)
public class ManagedTest extends BaseUtilTest {

    public void lock() {
        Map<String, Integer> map = new HashMap<>();
        ManagedLock lock = new ManagedLock();
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> {
                    lock.withLock(() -> {
                        map.compute("key", (s, v) -> v == null ? 1 : v + 1);
                    });
                });
        assertEquals((int) map.get("key"), 1000);
    }

    public void semaphore() {
        AtomicInteger c = new AtomicInteger();
        Set<Integer> set = new ConcurrentSkipListSet<>();
        ManagedSemaphore semaphore = new ManagedSemaphore(2);
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> semaphore.withPermit(() -> {
                    set.add(c.incrementAndGet());
                    c.decrementAndGet();
                }));
        assertEquals(set, Arrays.asList(1, 2));
    }

    public void resource() {
        ManagedResource<AtomicInteger> resource = ManagedResource.exclusive(new AtomicInteger());
        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> resource.borrow(atomic -> {
                    atomic.incrementAndGet();
                    assertEquals(atomic.get(), 1);
                    Clock.sleepQuietly(1);
                    atomic.decrementAndGet();
                }));

        AtomicInteger c = new AtomicInteger(0);
        ManagedResource<AtomicInteger> limited = ManagedResource.limited(3, c);
        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> limited.borrow(atomic -> {
                    c.incrementAndGet();
                    assertTrue(c.get() < 3);
                    Clock.sleepQuietly(1);
                    c.decrementAndGet();
                }));
    }
}
