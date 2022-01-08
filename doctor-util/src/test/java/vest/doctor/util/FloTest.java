package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.flow.Flo;
import vest.doctor.flow.Signal;
import vest.doctor.tuple.Tuple;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

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

    public void join() {
        String s = Flo.iterate(strings)
                .subscribe()
                .join();
        assertEquals(s, strings.get(strings.size() - 1));
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

    public void basicBackpressure() {
        AtomicInteger c = new AtomicInteger(0);
        Flo.iterate(strings)
                .step((value, subscription, emit) -> {
                    int andIncrement = c.getAndIncrement();
                    assertEquals(value, strings.get(andIncrement));
                    subscription.request(1);
                })
                .subscribe(1)
                .join();
        assertEquals(c.get(), 5);
    }

    public void basicCancel() {
        AtomicInteger c = new AtomicInteger(0);
        Flo.iterate(strings)
                .parallel(Executors.newSingleThreadScheduledExecutor())
                .step((value, subscription, emit) -> {
                    int andIncrement = c.getAndIncrement();
                    assertEquals(value, strings.get(andIncrement));
                    subscription.cancel();
                })
                .parallel(Executors.newSingleThreadScheduledExecutor())
                .subscribe(1)
                .join();
        assertEquals(c.get(), 1);
    }

    public void complex() {
        Flo.iterate(strings)
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
        assertThrows(() -> Flo.of("string")
                .map(s -> s.getBytes()[100])
                .subscribe()
                .join());
    }

    public void empty() {
        Object result = Flo.empty()
                .subscribe()
                .join();
        assertNull(result);
    }

    public void signalProcessor() {
        Flo.iterate(strings)
                .signal(String.class, s -> {
                    if (s.type() == Signal.Type.ITEM) {
                        s.emit(s.item().toUpperCase());
                    } else {
                        s.defaultAction();
                    }
                })
                .collect(Collectors.toList())
                .parallel()
                .observe(expect(1, (it, l) -> assertEquals(l, upperString)))
                .subscribe()
                .join();
    }

    public void tupleFlow() {
        Flo.iterate(strings)
                .map(Tuple.affix(String::length))
                .observe(Tuple.consumer2((str, len) -> assertEquals(str.length(), (int) len)))
                .map(t -> Tuple.of(t.second(), t.first()))
                .observe(Tuple.consumer2((len, str) -> assertEquals(str.length(), (int) len)))
                .subscribe()
                .join();

        Flo.iterate(strings)
                .map(Tuple.affix(String::length))
                .map(Tuple.affix2((str, len) -> str.toUpperCase()))
                .observe(Tuple.consumer3((str, len, upper) -> {
                    assertEquals(str.length(), (int) len);
                    assertEquals(str.toUpperCase(), upper);
                }))
                .subscribe()
                .join();

        Flo.ofEntries(Map.of("a", 1,
                        "b", 2,
                        "c", 3))
                .observe(Tuple.consumer2((str, it) -> assertEquals(str.charAt(0) - 'a' + 1, (int) it)))
                .subscribe();

        Flo.of('a')
                .map(Tuple.affix(a -> (char) (a + 1)))
                .map(Tuple.affix2((a, b) -> (char) (b + 1)))
                .map(Tuple.affix3((a, b, c) -> (char) (c + 1)))
                .map(Tuple.affix4((a, b, c, d) -> (char) (d + 1)))
                .observe(Tuple.consumer5((a, b, c, d, e) -> {
                    assertEquals((char) a, 'a');
                    assertEquals((char) b, 'b');
                    assertEquals((char) c, 'c');
                    assertEquals((char) d, 'd');
                    assertEquals((char) e, 'e');
                }))
                .subscribe()
                .join();
    }
}
