package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.tuple.Tuple2;
import vest.doctor.workflow.Signal;
import vest.doctor.workflow.Workflow;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Test(invocationCount = 5)
public class WorkflowTest extends BaseUtilTest {

    static final List<String> strings = List.of("alpha", "bravo", "charlie", "delta", "foxtrot");

    public void basicObserve() {
        Workflow.iterate(strings)
                .observe(expect(5, (it, string) -> assertEquals(string, strings.get(it))))
                .subscribe()
                .join();
    }

    public void basicMap() {
        Workflow.iterate(strings)
                .map(String::length)
                .observe(expect(5, (it, length) -> assertEquals((int) length, strings.get(it).length())))
                .subscribe()
                .join();
    }

    public void basicFlatMap() {
        String test = "test";
        Workflow.of(test)
                .flatMapList(string -> string.chars().mapToObj(Character::toString).collect(Collectors.toList()))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribe()
                .join();
    }

    public void basicFlatStream() {
        String test = "test";
        Workflow.of(test)
                .flatMapStream(string -> string.chars().mapToObj(Character::toString))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribe()
                .join();
    }

    public void basicFilter() {
        Workflow.iterate(strings)
                .keep(s -> s.length() == 5)
                .observe(expect(3, (it, string) -> assertEquals(string.length(), 5)))
                .subscribe()
                .join();
    }

    public void basicCollect() {
        Workflow.iterate(strings)
                .collect(Collectors.toList())
                .observe(expect(1, (it, list) -> assertEquals(list, strings)))
                .subscribe()
                .join();
    }

    public void adhocSource() {
        Workflow.adhoc(String.class)
                .observe(expect(3, (it, c) -> assertNotNull(c)))
                .subscribe()
                .publish("alpha")
                .publish("bravo")
                .publish("charlie")
                .finish()
                .future()
                .join();
    }

    public void basicBackpressure() {
        AtomicInteger c = new AtomicInteger(0);
        Workflow.iterate(strings)
                .observe((value, subscription) -> {
                    int andIncrement = c.getAndIncrement();
                    assertEquals(value, strings.get(andIncrement));
                    subscription.request(1);
                })
                .subscribe(1)
                .join();
        assertEquals(c.get(), 5);
    }

    public void basicBranch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(strings.size());
        Workflow.iterate(strings)
                .tee(Workflow.adhoc(String.class)
                        .parallel(Executors.newFixedThreadPool(4))
                        .observe(expect(5, (it, string) ->
                                System.out.println("first observer: " + it + " " + string)))
                        .delay(10, TimeUnit.MILLISECONDS)
                        .map(String::length)
                        .observe(expect(5, (it, length) ->
                                System.out.println("second observer: " + it + " " + length)))
                        .observe(i -> latch.countDown()))
                .observe(expect(5, (it, string) -> assertEquals(string, strings.get(it))))
                .subscribe()
                .join();
        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
    }

    public void basicAsync() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(strings.size());
        Workflow.iterate(strings)
                .<Tuple2<String, Integer>>chain((string, subscription, emitter) -> {
                    CompletableFuture.supplyAsync(string::length, ForkJoinPool.commonPool())
                            .thenAccept(l -> emitter.emit(Tuple2.of(string, l)));
                })
                .observe(l -> System.out.println(Thread.currentThread().getName() + " string length: " + l))
                .observe(expect(5, (it, v) -> assertEquals(v.second().intValue(), v.first().length())))
                .observe(v -> latch.countDown())
                .subscribe()
                .join();
        assertTrue(latch.await(5000, TimeUnit.MILLISECONDS));
    }

    public void basicRecover() {
        Byte b = Workflow.of("string")
                .map(s -> s.getBytes()[100])
                .recover(error -> (byte) 0xFF)
                .subscribe()
                .join();
        assertEquals(b.byteValue(), (byte) 0xFF);
    }

    public void complex() {
        Workflow.iterate(strings)
                .flatMapStream(string -> string.chars().mapToObj(Character::toString))
                .keep(c -> c.equals("a"))
                .observe(expect(5, (it, c) -> assertEquals(c, "a")))
                .collect(Collectors.counting())
                .observe(expect(1, (it, count) -> assertEquals(count.intValue(), 5)))
                .flatMapStream(count -> LongStream.range(0, count).boxed())
                .observe(expect(5, (it, l) -> assertEquals(it.intValue(), l.intValue())))
                .observe(expect(5, (it, l) -> assertEquals(it.intValue(), l.intValue())))
                .observe(expect(5, (it, l) -> assertEquals(it.intValue(), l.intValue())))
                .subscribe()
                .join();
    }

    public void errorInPipe() {
        assertThrows(() -> Workflow.of("string")
                .map(s -> s.getBytes()[100])
                .subscribe()
                .join());
    }

    public void skipLimit() {
        Workflow.iterate(strings)
                .skip(1)
                .limit(3)
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("bravo", "charlie", "delta"))))
                .subscribe()
                .join();
    }

    public void takeWhile() {
        Workflow.iterate(strings)
                .takeWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("alpha", "bravo"))))
                .subscribe()
                .join();
    }

    public void dropWhile() {
        Workflow.iterate(strings)
                .dropWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("charlie", "delta", "foxtrot"))))
                .subscribe()
                .join();
    }

    public void parallel() {
        Workflow.iterate(strings)
                .parallel(Executors.newSingleThreadExecutor())
                .observe(s -> System.out.println(Thread.currentThread() + " " + s))
                .subscribe()
                .join();
    }

    public void signal() {
        Workflow.iterate(strings)
                .signal(Integer.class, s -> {
                    if (s.type() == Signal.Type.VALUE) {
                        Integer len = s.value().length();
                        s.downstream().ifPresent(d -> d.onNext(len));
                    } else {
                        s.doDefaultAction();
                    }
                })
                .observe(expect(5, (it, l) -> assertEquals((int) l, strings.get(it).length())))
                .subscribe()
                .join();
    }

    public void timeout() {
        assertThrows(() -> Workflow.adhoc(String.class)
                .timeout(Executors.newSingleThreadScheduledExecutor(), Duration.ofMillis(100))
                .observe(expect(0, (it, s) -> {
                }))
                .subscribe()
                .join());

        Workflow.adhoc(String.class)
                .timeout(Executors.newSingleThreadScheduledExecutor(), Duration.ofMillis(100))
                .observe(expect(1, (it, s) -> assertEquals(s, "a")))
                .subscribe()
                .publish("a")
                .finish()
                .join();
    }
}
