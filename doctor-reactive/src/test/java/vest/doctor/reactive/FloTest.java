package vest.doctor.reactive;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

@Test(invocationCount = 5)
public class FloTest extends Assert {

    final ExecutorService BACKGROUND = Executors.newCachedThreadPool();
    final List<String> list = List.of("a", "b", "c", "d", "e", "f");
    final List<String> capitalized = list.stream().map(String::toUpperCase).collect(Collectors.toList());

    public void collect() {
        String result = Rx.one("alpha")
                .flatMapStream(s -> s.chars().mapToObj(c -> "" + (char) c))
                .map(String::toUpperCase)
                .collect(Collectors.joining())
                .subscribe()
                .join();
        assertEquals(result, "ALPHA");
    }

    @Test(invocationCount = 250)
    public void flatFlo() {
        List<String> upper = Rx.each(list)
                .flatMapStream(s -> s.chars().mapToObj(c -> "" + (char) c))
                .mapPublisher(item -> Rx.one(item).map(String::toUpperCase))
                .collect(Collectors.toList())
                .subscribe()
                .join();
        Collections.sort(upper);
        assertEquals(upper, this.capitalized);
    }

    public void recover() {
        String join = Rx.each(List.of("a", "ERROR", "c"))
                .observe(s -> {
                    if (s.equals("ERROR")) {
                        throw new IllegalArgumentException("got ERROR");
                    }
                })
                .recover(err -> "RECOVER")
                .observe(System.out::println)
                .collect(Collectors.joining(" "))
                .subscribe()
                .join();
        assertEquals(join, "a RECOVER c");
    }

    public void parallel() {
        List<String> join = Rx.each(list)
                .parallel(BACKGROUND)
                .observe(s -> System.out.println(Thread.currentThread().getName() + " " + s))
                .collect(Collectors.toList())
                .subscribe()
                .join();
        assertTrue(join.containsAll(list));
        assertTrue(list.containsAll(join));

        join = Rx.each(list)
                .parallel()
                .collect(Collectors.toList())
                .subscribe()
                .join();
        assertTrue(join.containsAll(list));
        assertTrue(list.containsAll(join));
    }

    public void filter() {
        List<String> result = Rx.each(list)
                .filter(s -> !s.equals("b") && !s.equals("e"))
                .collect(Collectors.toList())
                .subscribe()
                .join();
        assertEquals(result, List.of("a", "c", "d", "f"));

        List<String> d = Rx.each(list)
                .takeWhile(s -> !s.equals("d"))
                .collect(Collectors.toList())
                .observe(System.out::println)
                .subscribe()
                .join();
        System.out.println(d);
        assertEquals(d, List.of("a", "b", "c"));
    }

    public void mapFuture() {
        List<String> result = Rx.one("alpha")
                .mapFuture(s -> CompletableFuture.completedFuture(s.toUpperCase()))
                .collect(Collectors.toList())
                .subscribe()
                .join();
        assertEquals(result, List.of("ALPHA"));

        try {
            Rx.one("alpha")
                    .mapFuture(s -> CompletableFuture.failedFuture(new IllegalArgumentException("illegal")))
                    .subscribe()
                    .join();
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof CompletionException);
            assertTrue(t.getCause() instanceof IllegalArgumentException);
        }
    }

    public void mapIterable() {
        String join = Rx.one(String.join(" ", list))
                .flatMapIterable(s -> Arrays.asList(s.toString().split(" ")))
                .collect(Collectors.joining(" "))
                .subscribe()
                .join();
        assertEquals(join, String.join(" ", list));
    }

    public void trickle() {
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>(BACKGROUND, Flow.defaultBufferSize());
        CompletableFuture<List<String>> future = Rx.from(publisher)
                .onSubscribe(sub -> sub.request(1))
                .<String>onNext((item, subscription, subscriber) -> {
                    subscriber.onNext(item.toUpperCase());
                    subscription.request(1);
                })
                .collect(Collectors.toList())
                .subscribe(0L);
        BACKGROUND.submit(() -> {
            list.forEach(publisher::submit);
            publisher.close();
        });
        List<String> result = future.join();
        assertEquals(result, capitalized);
    }

    public void simpleError() {
        CompletableFuture<String> future = Rx.one("a")
                .map(String::toUpperCase)
                .observe(s -> {
                    throw new IllegalArgumentException("error");
                })
                .subscribe();
        assertThrows(CompletionException.class, future::join);
    }

    public void parallelError() {
        CompletableFuture<String> future = Rx.one("a")
                .parallel(BACKGROUND)
                .map(String::toUpperCase)
                .observe(s -> {
                    throw new IllegalArgumentException("error");
                })
                .subscribe();
        assertThrows(CompletionException.class, future::join);
    }

    public void standardSubscriber() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        AtomicReference<String> value = new AtomicReference<>(null);
        AtomicBoolean completed = new AtomicBoolean();
        Rx.one("alpha")
                .chain(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscribed.set(true);
                    }

                    @Override
                    public void onNext(String item) {
                        value.set(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        completed.set(true);
                    }
                })
                .subscribe()
                .join();
        assertTrue(subscribed.get());
        assertEquals(value.get(), "alpha");
        assertTrue(completed.get());
    }

    public void onComplete() {
        AtomicBoolean complete = new AtomicBoolean(false);
        Rx.one("a")
                .onComplete((subscription, subscriber) -> {
                    complete.set(true);
                    subscriber.onComplete();
                })
                .subscribe()
                .join();
        assertTrue(complete.get());
    }

    public void onError() {
        AtomicBoolean errored = new AtomicBoolean(false);
        Rx.error(new IllegalArgumentException())
                .onError((error, subscription, subscriber) -> {
                    errored.set(true);
                    subscriber.onError(error);
                })
                .subscribe()
                .exceptionally(error -> "")
                .join();
        assertTrue(errored.get());
    }

    public void mono() {
        List<String> result = Rx.one(list)
                .flatMapIterable(Function.identity())
                .map(String::toUpperCase)
                .collect(Collectors.toList())
                .subscribe()
                .join();
        assertEquals(result, capitalized);
    }

    public void empty() {
        String result = Rx.<String>empty()
                .subscribe()
                .join();
        assertNull(result);

        result = Rx.<String>empty()
                .onComplete(((subscription, subscriber) -> {
                    subscriber.onNext("finished value");
                    subscriber.onComplete();
                }))
                .subscribe()
                .join();
        assertEquals(result, "finished value");
    }

    public void mapPublisher() {
        List<String> join = Rx.one(list)
                .mapPublisher(item -> Rx.one(item).flatMapIterable(s -> s).map(String::toUpperCase))
                .collect(Collectors.toList())
                .subscribe()
                .join();
        assertEquals(join, capitalized);

        CompletableFuture<List<String>> future = Rx.each(list)
                .mapPublisher(item -> Rx.one(item).map(s -> {
                    if (s.equals("b")) {
                        throw new IllegalArgumentException("errored " + s);
                    }
                    return s.toUpperCase();
                }))
                .collect(Collectors.toList())
                .subscribe();
        assertThrows(future::join);
    }

    public void toStream() {
        List<String> collect = Rx.each(list)
                .map(String::toUpperCase)
                .subscribeStream()
                .collect(Collectors.toList());
        assertEquals(collect, capitalized);

        collect = Rx.each(list)
                .map(String::toUpperCase)
                .subscribeStream()
                .parallel()
                .collect(Collectors.toList());
        collect.sort(String.CASE_INSENSITIVE_ORDER);
        assertEquals(collect, capitalized);
    }
}
