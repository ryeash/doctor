package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.pipeline.Pipeline;
import vest.doctor.pipeline.Stage;
import vest.doctor.tuple.Tuple2;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Test(invocationCount = 5)
public class RxTest extends BaseUtilTest {

    static final List<String> strings = List.of("alpha", "bravo", "charlie", "delta", "foxtrot");

    public void basicObserve() {
        Pipeline.iterate(strings)
                .observe(expect(5, (it, string) -> assertEquals(string, strings.get(it))))
                .subscribeJoin();
    }

    public void basicMap() {
        Pipeline.iterate(strings)
                .map(String::length)
                .observe(expect(5, (it, length) -> assertEquals((int) length, strings.get(it).length())))
                .subscribeJoin();
    }

    public void futureMap() {
        Pipeline.iterate(strings)
                .mapFuture(s -> CompletableFuture.completedFuture(s.length()))
                .observe(expect(5, (it, length) -> assertEquals((int) length, strings.get(it).length())))
                .subscribeJoin();
    }

    public void basicFlatMap() {
        String test = "test";
        Pipeline.of(test)
                .flatMap(string -> string.chars().mapToObj(Character::toString).collect(Collectors.toList()))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribeJoin();
    }

    public void basicFlatStream() {
        String test = "test";
        Pipeline.of(test)
                .flatStream(string -> string.chars().mapToObj(Character::toString))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribeJoin();
    }

    public void basicFilter() {
        Pipeline.iterate(strings)
                .filter(s -> s.length() == 5)
                .observe(expect(3, (it, string) -> assertEquals(string.length(), 5)))
                .subscribeJoin();
    }

    public void basicCollect() {
        Pipeline.iterate(strings)
                .collect(Collectors.toList())
                .observe(expect(1, (it, list) -> assertEquals(list, strings)))
                .subscribeJoin();
    }

    public void collectFuture() {
        List<String> list = Pipeline.iterate(strings)
                .collect(Collectors.toList())
                .subscribeFuture()
                .join();
        assertEquals(list, strings);
    }

    public void adhocSource() {
        Stage<Object, String> stage = Pipeline.adHoc(String.class)
                .buffer()
                .observe(expect(3, (it, c) -> assertNotNull(c)))
                .toStage();
        CompletableFuture<String> future = Pipeline.from(stage).subscribeFuture();
        stage.onNext("alpha");
        stage.onNext("bravo");
        stage.onNext("charlie");
        stage.onComplete();
        future.join();
    }

    public void basicBackpressure() {
        AtomicInteger c = new AtomicInteger(0);
        Pipeline.iterate(strings)
                .observe((subscription, value) -> {
                    int andIncrement = c.getAndIncrement();
                    assertEquals(value, strings.get(andIncrement));
                    subscription.request(1);
                })
                .subscribeJoin(1);
        assertEquals(c.get(), 5);
    }

    public void basicBranch() {
        Pipeline.iterate(strings)
                .branch(p -> p
                        .observe(expect(5, (it, string) -> {
                            System.out.println("first observer: " + it + " " + string);
                        }))
                        .map(String::length)
                        .observe(expect(5, (it, length) -> {
                            System.out.println("second observer: " + it + " " + length);
                        })))
                .observe(expect(5, (it, string) -> assertEquals(string, strings.get(it))))
                .subscribeJoin();
        Clock.sleepQuietly(100);
    }

    public void basicBuffer() {
        Pipeline.iterate(strings)
                .buffer()
                .observe(expect(5, (it, v) -> assertEquals(v, strings.get(it))))
                .map(String::length)
                .observe(expect(5, (it, v) -> assertEquals((int) v, strings.get(it).length())))
                .subscribeJoin(Executors.newSingleThreadExecutor());

        Pipeline.iterate(strings)
                .buffer(-1)
                .observe(expect(5, (it, v) -> assertEquals(v, strings.get(it))))
                .map(String::length)
                .observe(expect(5, (it, v) -> assertEquals((int) v, strings.get(it).length())))
                .subscribeJoin(Executors.newSingleThreadExecutor());
    }

    public void basicAsync() {
        AtomicReference<CompletableFuture<?>> finished = new AtomicReference<>();
        Pipeline.iterate(strings)
                .<Tuple2<String, Integer>>async((string, emitter) -> {
                    CompletableFuture.supplyAsync(string::length, ForkJoinPool.commonPool())
                            .thenAccept(l -> emitter.emit(Tuple2.of(string, l)));
                })
                .attachFuture(finished::set)
                .observe(l -> System.out.println(Thread.currentThread().getName() + " string length: " + l))
                .observe(expect(5, (it, v) -> assertEquals(v.second().intValue(), v.first().length())))
                .subscribeJoin();
        finished.get().join();
        Clock.sleepQuietly(100);
    }

    public void basicCompletable() {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture<String> completed = Pipeline.completable(future)
                .observe(expect(1, (it, v) -> assertEquals(v, "test")))
                .subscribeFuture();
        future.complete("test");
        completed.join();
    }

    public void basicRecover() {
        Byte b = Pipeline.of("string")
                .map(s -> s.getBytes()[100])
                .recover(error -> (byte) 0xFF)
                .subscribeFuture()
                .join();
        assertEquals(b.byteValue(), (byte) 0xFF);
    }

    public void complex() {
        Pipeline.iterate(strings)
                .flatStream(string -> string.chars().mapToObj(Character::toString))
                .filter(c -> c.equals("a"))
                .observe(expect(5, (it, c) -> assertEquals(c, "a")))
                .collect(Collectors.counting())
                .observe(expect(1, (it, count) -> assertEquals(count.intValue(), 5)))
                .flatStream(count -> LongStream.range(0, count).boxed())
                .observe(expect(5, (it, l) -> assertEquals(it.intValue(), l.intValue())))
                .observe(expect(5, (it, l) -> assertEquals(it.intValue(), l.intValue())))
                .observe(expect(5, (it, l) -> assertEquals(it.intValue(), l.intValue())))
                .subscribeJoin();
    }

    public void errorInPipe() {
        assertThrows(() -> Pipeline.of("string")
                .map(s -> s.getBytes()[100])
                .subscribeJoin());
    }

    public void skipLimit() {
        Pipeline.iterate(strings)
                .skip(1)
                .limit(3)
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("bravo", "charlie", "delta"))))
                .subscribeJoin();
    }

    public void takeWhile() {
        Pipeline.iterate(strings)
                .takeWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("alpha", "bravo"))))
                .subscribeJoin();
    }

    public void dropWhile() {
        Pipeline.iterate(strings)
                .dropWhile(s -> !s.equals("charlie"))
                .collect(Collectors.toList())
                .observe(expect(1, (it, l) -> assertEquals(l, Arrays.asList("charlie", "delta", "foxtrot"))))
                .subscribeJoin();
    }
}
