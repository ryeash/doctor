package doctor;


import doctor.atomic.ManagedLock;
import doctor.atomic.ManagedResource;
import doctor.atomic.ManagedSemaphore;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class ManagedTest extends Assert {

    @Test(invocationCount = 5)
    public void lock() {
        Map<String, Integer> map = new HashMap<>();
        ManagedLock lock = new ManagedLock();
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> {
                    lock.guard(() -> {
                        map.compute("key", (s, v) -> v == null ? 1 : v + 1);
                    });
                });
        assertEquals((int) map.get("key"), 1000);
    }

    @Test(invocationCount = 5)
    public void semaphore() {
        AtomicInteger c = new AtomicInteger();
        Set<Integer> set = new ConcurrentSkipListSet<>();
        ManagedSemaphore semaphore = new ManagedSemaphore(2);
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> semaphore.guard(() -> {
                    set.add(c.incrementAndGet());
                    c.decrementAndGet();
                }));
        assertEquals(set, Arrays.asList(1, 2));
    }

    @Test(invocationCount = 5)
    public void resource() {
        ManagedResource<AtomicInteger> resource = ManagedResource.exclusive(new AtomicInteger());
        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> resource.borrow(atomic -> {
                    atomic.incrementAndGet();
                    assertEquals(atomic.get(), 1);
                    delay(1);
                    atomic.decrementAndGet();
                }));

        AtomicInteger c = new AtomicInteger(0);
        ManagedResource<AtomicInteger> limited = ManagedResource.limited(3, c);
        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> limited.borrow(atomic -> {
                    c.incrementAndGet();
                    assertTrue(c.get() < 3);
                    delay(1);
                    c.decrementAndGet();
                }));
    }

    private static void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignored
        }
    }
}
