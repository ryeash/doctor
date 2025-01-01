package vest.doctor.apache.server.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;

public final class CollectingSubscriber<I, A, O> implements Flow.Subscriber<I> {

    public static final Collector<byte[], ?, Void> IGNORING = Collector.<byte[], Void, Void>of(
            () -> null,
            (a, b) -> {},
            (a, b) -> {
                throw new UnsupportedOperationException();
            },
            (list) -> null);

    public static final Collector<byte[], ?, byte[]> BYTE_ARRAY_COLLECTOR = Collector.<byte[], List<byte[]>, byte[]>of(
            LinkedList::new,
            List::add,
            (a, b) -> {
                throw new UnsupportedOperationException();
            }, (list) -> {
                int totalSize = list.stream().mapToInt(a -> a.length).sum();
                byte[] data = new byte[totalSize];
                int i = 0;
                for (byte[] chunk : list) {
                    System.arraycopy(chunk, 0, data, i, chunk.length);
                    i += chunk.length;
                }
                return data;
            });

    private final Collector<? super I, A, ? extends O> collector;
    private final A container;
    private final boolean concurrent;
    private final AtomicBoolean collecting = new AtomicBoolean(true);
    private final CompletableFuture<? super O> future;

    public CollectingSubscriber(Collector<? super I, A, ? extends O> collector, CompletableFuture<? super O> future) {
        this.collector = Objects.requireNonNull(collector);
        this.container = collector.supplier().get();
        this.concurrent = collector.characteristics().contains(Collector.Characteristics.CONCURRENT);
        this.future = future;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(I item) {
        if (collecting.get()) {
            if (concurrent) {
                collector.accumulator().accept(container, item);
            } else {
                synchronized (container) {
                    collector.accumulator().accept(container, item);
                }
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        collecting.set(false);
        future.completeExceptionally(throwable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComplete() {
        if (collecting.compareAndSet(true, false)) {
            O collected;
            if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                collected = (O) container;
            } else {
                collected = collector.finisher().apply(container);
            }
            future.complete(collected);
        }
    }
}