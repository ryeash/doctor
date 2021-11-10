package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.flow.Flo;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FloTest extends BaseUtilTest {

    private final List<String> strings = Arrays.asList("alpha", "bravo", "charlie", "delta", "foxtrot");

    @Test
    public void adhoc() {
        Flo.adhoc(String.class)
                .parallel(ForkJoinPool.commonPool())
                .observe(s -> System.out.println(Thread.currentThread().getName() + " " + s))
                .subscribe()
                .onNext("a")
                .onNext("b")
                .onNext("c")
                .onComplete()
                .completionSignal()
                .join();
    }

    @Test
    public void adhocBuffered() {
        List<String> joined = Flo.adhoc(String.class)
                .buffer(Flow.defaultBufferSize())
                .parallel(ForkJoinPool.commonPool())
                .observe(s -> System.out.println(Thread.currentThread().getName() + " " + s))
                .collect(Collectors.toList())
                .subscribe()
                .onNext("a")
                .onNext("b")
                .onNext("c")
                .onComplete()
                .completionSignal()
                .join();
        System.out.println(joined);
    }

    @Test
    public void iterate() {
        Flo.iterate(strings)
                .parallel(ForkJoinPool.commonPool())
                .observe(s -> System.out.println(Thread.currentThread().getName() + " " + s))
                .subscribe()
                .join();
    }

    @Test
    public void skipLimit() {
        Flo.iterate(strings)
                .skip(1)
                .limit(3)
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("bravo", "charlie", "delta"))))
                .subscribe()
                .join();

        Flo.adhoc(String.class)
                .subscriptionHook(s -> s.request(Long.MAX_VALUE))
                .skip(1)
                .limit(3)
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("bravo", "charlie", "delta"))))
                .subscribe()
                .onNext(strings)
                .join();
    }

    @Test
    public void takeWhile() {
        Flo.iterate(strings)
                .takeWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("alpha", "bravo"))))
                .subscribe()
                .join();
    }

    @Test
    public void dropWhile() {
        Flo.iterate(strings)
                .dropWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("charlie", "delta", "foxtrot"))))
                .subscribe()
                .join();
    }

    @Test
    public void errorSource() {
        CompletableFuture<String> error = Flo.error(String.class, new RuntimeException("error"))
                .subscribe()
                .completionSignal();
        assertTrue(error.isCompletedExceptionally());

        Flo.error(String.class, new RuntimeException("error"))
                .recover(Throwable::getMessage)
                .observe(expect(1, (it, str) -> assertEquals(str, "error")))
                .subscribe()
                .join();
    }

    @Test
    public void timeout() {
        assertThrows(() -> Flo.adhoc(String.class)
                .timeout(Executors.newSingleThreadScheduledExecutor(), Duration.ofMillis(100))
                .observe(expect(0, (it, s) -> System.out.println(s)))
                .subscribe()
                .join());

        Flo.adhoc(String.class)
                .timeout(Executors.newSingleThreadScheduledExecutor(), Duration.ofMillis(100))
                .observe(expect(1, (it, s) -> assertEquals(s, "a")))
                .subscribe()
                .onNext("a")
                .onComplete()
                .join();
    }

    @Test
    public void flattener() {
        // TODO: broken
        Flo.iterate(strings)
                .chain(str -> Flo.iterate(List.of(str))
                        .map((Function<String, String>) String::toUpperCase))
                .collect(Collectors.toList())
                .observe(System.out::println)
//                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("bravo", "charlie", "delta"))))
                .subscribe()
                .join();
    }

    @Test
    public void delayed() {
        Flo.iterate(strings)
                .parallel(ForkJoinPool.commonPool())
                .delay(Duration.ofMillis(100))
                .chain(str -> Flo.iterate(List.of(str))
                        .map(String::toUpperCase))
                .collect(Collectors.toList())
                .observe(l -> System.out.println(l))
//                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("bravo", "charlie", "delta"))))
                .subscribe()
                .join();
    }

    @Test
    public void mapFuture() {
        Flo.iterate(strings)
                .mapFuture(CompletableFuture::completedFuture)
                .observe(expect(5, (it, s) -> assertEquals(s, strings.get(it))))
                .subscribe();
    }

    @Test
    public void flatMap() {
        Flo.of("string")
                .flatMapStream(s -> s.chars().mapToObj(i -> "" + i))
                .map(String::toUpperCase)
                .collect(Collectors.joining())
                .observe(expect(1, (it, s) -> assertEquals(s, "STRING")))
                .subscribe();
    }
}
