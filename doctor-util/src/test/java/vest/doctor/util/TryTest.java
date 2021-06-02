package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.function.Try;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Test
public class TryTest extends BaseUtilTest {

    public void basics() {
        Try<Void> run = Try.run(() -> System.out.println("running"));
        assertTrue(run.isSuccess());
        assertFalse(run.isFailure());

        Try<Void> exceptional = Try.run(() -> {
            throw new IllegalArgumentException();
        });
        assertNotNull(exceptional.exception());
        assertTrue(exceptional.isFailure());
        assertFalse(exceptional.isSuccess());
        assertThrows(IllegalArgumentException.class, exceptional::get);
    }

    public void recovery() {
        AtomicInteger c = new AtomicInteger();
        Try<String> supply = Try
                .<String>get(() -> {
                    throw new IllegalArgumentException();
                })
                .onFailure(error -> c.incrementAndGet())
                .onFailure(IllegalArgumentException.class, error -> c.incrementAndGet())
                .onFailure(NullPointerException.class, error -> c.incrementAndGet())
                .exceptionally(error -> "string")
                .onSuccess(value -> c.incrementAndGet());
        assertTrue(supply.isSuccess());
        assertEquals(supply.get(), "string");
        assertEquals(c.get(), 3);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void throwIt() throws Throwable {
        Try.get(() -> "string")
                .throwException()
                .apply(s -> {
                    throw new IllegalArgumentException();
                })
                .throwUnchecked()
                .compose(obj -> {
                    fail("should not get here");
                    return Try.success("");
                });
    }

    public void laterFailure() {
        Try<Character> apply = Try.get(() -> "string")
                .apply(s -> s.charAt(42));
        assertTrue(apply.isFailure());
        assertThrows(IndexOutOfBoundsException.class, apply::get);
    }

    public void compose() {
        String s1 = Try.get(() -> "string")
                .compose(s -> Try.success(s.toUpperCase()))
                .get();
        assertEquals(s1, "STRING");
    }

    public void toThings() throws ExecutionException, InterruptedException {
        Try<String> success = Try.get(() -> "string");
        assertTrue(success.toOptional().isPresent());
        assertEquals(success.toStream().collect(Collectors.toList()), Collections.singletonList("string"));
        assertEquals(success.toCompletableFuture().get(), "string");

        Try<Object> failure = Try.get(() -> {
            throw new IllegalArgumentException();
        });
        assertFalse(failure.toOptional().isPresent());
        assertThrows(failure::toStream);
        assertTrue(failure.toCompletableFuture().isCompletedExceptionally());
    }

    public void completable() {
        CompletableFuture<String> s = new CompletableFuture<>();
        CompletableFuture<Try<String>> ct = Try.completable(s);
        s.complete("test");
        assertEquals(ct.join().get(), "test");

        CompletableFuture<String> e = new CompletableFuture<>();
        CompletableFuture<Try<String>> er = Try.completable(e);
        e.completeExceptionally(new IllegalArgumentException());
        assertThrows(IllegalArgumentException.class, er.join()::get);
    }
}
