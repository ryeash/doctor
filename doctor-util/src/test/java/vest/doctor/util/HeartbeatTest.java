package vest.doctor.util;

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class HeartbeatTest extends BaseUtilTest {

    @Test
    public void basicHeartbeat() {
        AtomicInteger counter = new AtomicInteger();
        Heartbeat hb = new Heartbeat(10);
        for (int i = 0; i < 100; i++) {
            hb.tick(c -> counter.incrementAndGet());
        }
        assertEquals(counter.get(), 10);
    }

    @Test
    public void parallelHeartbeat() {
        AtomicInteger counter = new AtomicInteger();
        Heartbeat hb = new Heartbeat(10);
        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> {
                    Clock.sleepQuietly(1);
                    hb.tick(c -> counter.incrementAndGet());
                });
        assertEquals(counter.get(), 10);
    }
}
