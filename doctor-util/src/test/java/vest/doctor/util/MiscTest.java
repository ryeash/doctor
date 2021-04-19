package vest.doctor.util;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.doctor.rx.Funnel;
import vest.doctor.rx.RingBuffer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

public class MiscTest extends Assert {
    @Test
    public void funnel() {
        Funnel<String> f = new Funnel<>();
        f.observe(s -> System.out.println(Thread.currentThread().getName() + " - first: " + s), ForkJoinPool.commonPool())
                .map(String::length)
                .observe(i -> System.out.println(Thread.currentThread().getName() + " - len: " + i), ForkJoinPool.commonPool());

        f.publish("alpha");
        f.publish("bravo");
        f.publish("charlie");
    }

    @Test
    public void ringBuffer() {
        RingBuffer<String> buf = new RingBuffer<>(100);
        UUID consumer1 = UUID.randomUUID();
        assertEquals(buf.size(consumer1), 0);
        assertTrue(buf.isEmpty(consumer1));

        UUID consumer2 = UUID.randomUUID();
        for (String s : List.of("a", "b", "c", "d", "e")) {
            buf.add(s);
            assertEquals(buf.size(consumer1), 1);
            assertFalse(buf.isEmpty(consumer1));

            assertEquals(buf.size(consumer2), 1);
            assertFalse(buf.isEmpty(consumer2));

            assertEquals(buf.poll(consumer1), s);
            assertEquals(buf.poll(consumer2), s);
            assertEquals(buf.size(consumer1), 0);
            assertEquals(buf.size(consumer2), 0);
        }

        UUID consumer3 = UUID.randomUUID();
        assertEquals(buf.size(consumer3), 5);
        assertEquals(buf.poll(consumer3), "a");
        assertEquals(buf.poll(consumer3), "b");
        assertEquals(buf.poll(consumer3), "c");
        assertEquals(buf.poll(consumer3), "d");
        assertEquals(buf.poll(consumer3), "e");
        assertNull(buf.poll(consumer3));

        RingBuffer<Integer> nums = new RingBuffer<>(10);
        UUID evens = UUID.randomUUID();
        UUID odds = UUID.randomUUID();
        IntStream.range(0, 100)
                .parallel()
                .forEach(i -> {
                    nums.add(i);
                    if ((i % 2) == 0) {
                        System.out.println("even: " + nums.poll(evens));
                    }
                    if ((i % 2) == 1) {
                        System.out.println("odd : " + nums.poll(odds));
                    }
                });
//        for (int i = 0; i < 100; i++) {
//            nums.add(i);
//            if ((i % 2) == 0) {
//                System.out.println(nums.poll(consumer));
//            }
//        }

    }
}
