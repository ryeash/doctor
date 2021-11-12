package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.flow.Flo;
import vest.doctor.tuple.Tuple;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Test(invocationCount = 5)
public class FloTest extends BaseUtilTest {

    private final List<String> strings = Arrays.asList("alpha", "bravo", "charlie", "delta", "foxtrot");
    private final List<String> upperString = strings.stream().map(String::toUpperCase).collect(Collectors.toList());

    public void adhoc() {
        Flo.adhoc(String.class)
                .parallel()
                .collect(Collectors.toSet())
                .observe(expect(1, (it, set) -> assertTrue(set.containsAll(Set.of("a", "b", "c")))))
                .subscribe()
                .onNext("a")
                .onNext("b")
                .onNext("c")
                .onComplete()
                .completionSignal()
                .join();
    }

    public void adhocBuffered() {
        Flo.adhoc(String.class)
                .buffer()
                .parallel()
                .collect(Collectors.toSet())
                .observe(expect(1, (it, set) -> assertTrue(set.containsAll(Set.of("a", "b", "c")))))
                .subscribe()
                .onNext("a")
                .onNext("b")
                .onNext("c")
                .onComplete()
                .completionSignal()
                .join();
    }

    public void iterate() {
        Flo.iterate(strings)
                .parallel(ForkJoinPool.commonPool())
                .collect(Collectors.toSet())
                .observe(expect(1, (it, set) -> assertTrue(set.containsAll(strings))))
                .subscribe()
                .join();
    }

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

    public void takeWhile() {
        Flo.iterate(strings)
                .takeWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("alpha", "bravo"))))
                .subscribe()
                .join();
    }

    public void dropWhile() {
        Flo.iterate(strings)
                .dropWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("charlie", "delta", "foxtrot"))))
                .subscribe()
                .join();
    }

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

    public void flattener() {
        Flo.iterate(strings)
                .chain(str -> Flo.iterate(List.of(str))
                        .map(String::toUpperCase))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, upperString)))
                .subscribe()
                .join();
    }

    public void delayed() {
        Flo.iterate(strings)
                .parallel(ForkJoinPool.commonPool())
                .delay(Duration.ofMillis(100))
                .chain(str -> Flo.iterate(List.of(str))
                        .map(String::toUpperCase))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertTrue(l.containsAll(upperString))))
                .subscribe()
                .join();
    }

    public void mapFuture() {
        Flo.iterate(strings)
                .mapFuture(CompletableFuture::completedFuture)
                .observe(expect(5, (it, s) -> assertEquals(s, strings.get(it))))
                .subscribe()
                .join();

        Flo.of("error")
                .mapFuture(s -> CompletableFuture.failedFuture(new IllegalStateException(s)))
                .recover(Throwable::getMessage)
                .observe(expect(1, (it, s) -> assertEquals(s, "error")))
                .subscribe()
                .join();
    }

    public void flatMap() {
        Flo.of("string")
                .flatMapStream(s -> s.chars().mapToObj(Character::toString))
                .map(String::toUpperCase)
                .collect(Collectors.joining())
                .observe(expect(1, (it, s) -> assertEquals(s, "STRING")))
                .subscribe()
                .join();

        Flo.of("string")
                .flatMapIterable(s -> s.chars().mapToObj(Character::toString).collect(Collectors.toList()))
                .map(String::toUpperCase)
                .collect(Collectors.joining())
                .observe(expect(1, (it, s) -> assertEquals(s, "STRING")))
                .subscribe()
                .join();
    }

    public void filtering() {
        Flo.iterate(strings)
                .keep(s -> s.startsWith("a"))
                .observe(expect(1, (it, s) -> assertTrue(s.startsWith("a"))))
                .subscribe()
                .join();

        Flo.iterate(strings)
                .drop(s -> s.startsWith("a"))
                .observe(expect(4, (it, s) -> assertFalse(s.startsWith("a"))))
                .subscribe()
                .join();
    }

    public void affix() {
        Flo.of("a")
                .affix("b")
                .observe(expect(1, (it, t) -> assertEquals(t, Tuple.of("a", "b"))))
                .subscribe()
                .join();

        Flo.of("a")
                .affix("b", "c")
                .observe(expect(1, (it, t) -> assertEquals(t, Tuple.of("a", "b", "c"))))
                .subscribe()
                .join();

        Flo.of("a")
                .affix("b", "c", "d")
                .observe(expect(1, (it, t) -> assertEquals(t, Tuple.of("a", "b", "c", "d"))))
                .subscribe()
                .join();

        Flo.of("a")
                .affix("b", "c", "d", "e")
                .observe(expect(1, (it, t) -> assertEquals(t, Tuple.of("a", "b", "c", "d", "e"))))
                .subscribe()
                .join();
    }
}
