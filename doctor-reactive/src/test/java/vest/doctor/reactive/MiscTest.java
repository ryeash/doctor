package vest.doctor.reactive;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Test(invocationCount = 5)
public class MiscTest extends Assert {

    final ExecutorService BACKGROUND = Executors.newCachedThreadPool();
    final List<String> list = List.of("a", "b", "c", "d", "e", "f");
    final List<String> capitalized = list.stream().map(String::toUpperCase).collect(Collectors.toList());

    public void basicCompletionSignal() {
        SubscriptionHandle<String, String> realize = Flo.start(String.class)
                .map(String::toUpperCase)
                .subscribe();
        CompletableFuture<String> future = realize.future();
        realize.emitOne("alpha");
        realize.done();
        assertEquals(future.join(), "ALPHA");
    }

    public void collect() {
        String result = Flo.start(String.class)
                .flatMapStream(s -> s.chars().mapToObj(c -> "" + (char) c))
                .map(String::toUpperCase)
                .collect(Collectors.joining())
                .subscribe()
                .emitOne("alpha")
                .done()
                .join();
        assertEquals(result, "ALPHA");
    }

    public void flatFlo() {
        String join = Flo.start(String.class)
                .flatMapStream(s -> s.chars().mapToObj(c -> "" + (char) c))
                .<String>onNext((item, emitter) -> {
                    Flo.start(String.class)
                            .map(String::toUpperCase)
                            .observe(emitter::accept)
                            .subscribe()
                            .justOne(item);
                })
                .collect(Collectors.joining())
                .subscribe()
                .emitOne("alpha")
                .emitOne(" ")
                .emitOne("bravo")
                .emitOne(" ")
                .emitOne("charlie")
                .done()
                .join();
        assertEquals(join, "ALPHA BRAVO CHARLIE");
    }

    public void recover() {
        String join = Flo.start(String.class)
                .observe(s -> {
                    if (s.equals("ERROR")) {
                        throw new IllegalArgumentException("got ERROR");
                    }
                })
                .recover(err -> "RECOVER")
                .collect(Collectors.joining(" "))
                .subscribe()
                .emitAll(List.of("a", "ERROR", "c"))
                .done()
                .join();
        assertEquals(join, "a RECOVER c");
    }

    public void parallel() {
        List<String> join = Flo.start(String.class)
                .parallel(BACKGROUND)
                .collect(Collectors.toList())
                .subscribe()
                .emitAll(list)
                .done()
                .join();
        assertTrue(join.containsAll(list));
        assertTrue(list.containsAll(join));

        join = Flo.start(String.class)
                .parallel(BACKGROUND, BACKGROUND)
                .collect(Collectors.toList())
                .subscribe()
                .emitAll(list)
                .done()
                .join();
        assertTrue(join.containsAll(list));
        assertTrue(list.containsAll(join));

        join = Flo.start(String.class)
                .parallel()
                .collect(Collectors.toList())
                .subscribe()
                .emitAll(list)
                .done()
                .join();
        assertTrue(join.containsAll(list));
        assertTrue(list.containsAll(join));
    }

    public void filter() {
        List<String> result = Flo.start(String.class)
                .drop(s -> s.equals("b"))
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("a", "c", "d", "e", "f"));

        result = Flo.start(String.class)
                .keep(s -> s.equals("b"))
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("b"));
    }

    public void filterWhile() {
        List<String> result = Flo.start(String.class)
                .dropUntil(s -> s.equals("b"))
                .takeWhile(s -> !s.equals("e"))
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("b", "c", "d"));

        result = Flo.start(String.class)
                .dropUntil(s -> s.equals("b"), false)
                .takeWhile(s -> !s.equals("e"))
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("c", "d"));

        result = Flo.start(String.class)
                .dropUntil(s -> s.equals("b"))
                .takeWhile(s -> !s.equals("e"), true)
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("b", "c", "d", "e"));
    }

    public void skipLimit() {
        List<String> result = Flo.start(String.class)
                .skip(2)
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("c", "d", "e", "f"));

        result = Flo.start(String.class)
                .limit(3)
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("a", "b", "c"));

        result = Flo.start(String.class)
                .skip(2)
                .limit(2)
                .collect(Collectors.toList())
                .subscribe()
                .justThese(list)
                .join();
        assertEquals(result, List.of("c", "d"));
    }

    public void mapFuture() {
        List<String> result = Flo.start(String.class)
                .mapFuture(s -> CompletableFuture.completedFuture(s.toUpperCase()))
                .collect(Collectors.toList())
                .subscribe()
                .justOne("alpha")
                .join();
        assertEquals(result, List.of("ALPHA"));
    }

    public void mapIterable() {
        String join = Flo.start(CharSequence.class)
                .flatMapIterable(s -> Arrays.asList(s.toString().split(" ")))
                .collect(Collectors.joining(" "))
                .subscribe()
                .justOne(String.join(" ", list))
                .join();
        assertEquals(join, String.join(" ", list));
    }

    public void trickle() {
        SubscriptionHandle<String, List<String>> subscribe = Flo.start(String.class)
                .<String>onNext((item, subscription, subscriber) -> {
                    subscriber.onNext(item.toUpperCase());
                    subscription.request(1);
                })
                .collect(Collectors.toList())
                .subscribe(1);
        BACKGROUND.submit(() -> subscribe.justThese(list));
        List<String> result = subscribe.join();
        assertEquals(result, capitalized);
    }

    public void simpleError() {
        SubscriptionHandle<String, String> subscribe = Flo.start(String.class)
                .map(String::toUpperCase)
                .observe(s -> {
                    throw new IllegalArgumentException("error");
                })
                .subscribe();
        subscribe.justOne("a");
        assertThrows(CompletionException.class, subscribe::join);
    }

    public void parallelError() {
        SubscriptionHandle<String, String> subscribe = Flo.start(String.class)
                .parallel(BACKGROUND)
                .map(String::toUpperCase)
                .observe(s -> {
                    throw new IllegalArgumentException("error");
                })
                .subscribe();
        subscribe.justOne("a");
        assertThrows(CompletionException.class, subscribe::join);
    }

    public void listener() {
        AtomicBoolean subcribed = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);
        Flo.start(String.class)
                .onSubscribe(s -> {
                    s.addStateListener(FlowState.SUBSCRIBED, (sub) -> subcribed.set(true));
                    s.addStateListener(FlowState.COMPLETED, (sub) -> completed.set(true));
                })
                .map(String::toUpperCase)
                .subscribe()
                .justOne("a")
                .join();
        assertTrue(subcribed.get());
        assertTrue(completed.get());

        AtomicBoolean canceled = new AtomicBoolean(false);
        Flo.start(String.class)
                .onSubscribe(s -> {
                    s.addStateListener(FlowState.CANCELLED, (sub) -> canceled.set(true));
                })
                .map(String::toUpperCase)
                .onNext((item, subscription, subscriber) -> subscription.cancel())
                .subscribe()
                .justOne("a")
                .join();
        assertTrue(canceled.get());

        AtomicBoolean errored = new AtomicBoolean(false);
        SubscriptionHandle<String, String> error = Flo.start(String.class)
                .onSubscribe(s -> {
                    s.addStateListener(FlowState.ERROR, (sub) -> errored.set(true));
                })
                .map(String::toUpperCase)
                .subscribe()
                .emitOne("a")
                .error(new IllegalArgumentException("error"));
        assertThrows(error::join);
        assertTrue(errored.get());
    }

    @Test
    public void standardSubscriber() {
        AtomicBoolean subscribed = new AtomicBoolean(false);
        AtomicReference<String> value = new AtomicReference<>(null);
        AtomicBoolean completed = new AtomicBoolean();
        Flo.start(String.class)
                .process(new Flow.Subscriber<>() {
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
                .justOne("alpha")
                .join();
        assertTrue(subscribed.get());
        assertEquals(value.get(), "alpha");
        assertTrue(completed.get());
    }

    @Test
    public void onComplete() {
        AtomicBoolean complete = new AtomicBoolean(false);
        Flo.start(String.class)
                .onComplete((subscription, subscriber) -> {
                    complete.set(true);
                    subscriber.onComplete();
                })
                .subscribe()
                .justOne("a")
                .join();
        assertTrue(complete.get());
    }

    @Test
    public void onError() {
        AtomicBoolean errored = new AtomicBoolean(false);
        Flo.start(String.class)
                .onError((error, subscription, subscriber) -> {
                    errored.set(true);
                    subscriber.onError(error);
                })
                .subscribe()
                .emitOne("a")
                .error(new IllegalArgumentException());
        assertTrue(errored.get());
    }

}
