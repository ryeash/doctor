package vest.doctor.rx;

import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SimpleBufferingSubscriber<T> implements Flow.Subscriber<T> {
    private enum State {
        ITEMS,
        COMPLETE,
        CANCELED,
        ERROR
    }

    record ItemAndState<T>(T item, State state) {
    }

    private final Flow.Subscriber<? super T> subscriber;
    private final AtomicReference<State> state = new AtomicReference<>(State.ITEMS);
    private final ExecutorService executorService;
    private final BlockingQueue<ItemAndState<T>> queue;
    private final Consumer<T> onCancelDrainAction;
    private final AtomicReference<Throwable> error = new AtomicReference<>();

    private SimpleBufferingSubscriber(BlockingQueue<ItemAndState<T>> queue, Flow.Subscriber<? super T> subscriber, ExecutorService executorService, Consumer<T> onCancelDrainAction) {
        this.queue = queue;
        this.subscriber = subscriber;
        this.executorService = executorService;
        this.onCancelDrainAction = onCancelDrainAction;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(T item) {
        if (state.get() == State.ITEMS) {
            if (queue.offer(new ItemAndState<>(item, State.ITEMS))) {
                startLoop();
            } else {
                throw new BufferOverflowException();
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (state.compareAndSet(State.ITEMS, State.ERROR)) {
            error.set(throwable);
            clearQueue();
            try {
                if (!queue.offer(new ItemAndState<>(null, State.ERROR), 5, TimeUnit.SECONDS)) {
                    subscriber.onError(throwable);
                }
            } catch (InterruptedException e) {
                subscriber.onError(throwable);
            }
            startLoop();
        }
    }

    private void clearQueue() {
        if (onCancelDrainAction != null) {
            ItemAndState<T> next;
            while ((next = queue.poll()) != null) {
                try {
                    onCancelDrainAction.accept(next.item());
                } catch (Throwable t) {
                    // ignored
                }
            }
        }
        queue.clear();
    }

    @Override
    public void onComplete() {
        if (state.compareAndSet(State.ITEMS, State.COMPLETE)) {
            queue.offer(new ItemAndState<>(null, State.COMPLETE));
            startLoop();
        }
    }

    private final AtomicBoolean running = new AtomicBoolean(false);

    private void startLoop() {
        if (running.compareAndSet(false, true)) {
            executorService.submit(this::serviceQueue);
        }
    }

    private void serviceQueue() {
        try {
            ItemAndState<T> next;
            while ((next = queue.poll()) != null) {
                switch (next.state()) {
                    case ITEMS -> subscriber.onNext(next.item());
                    case COMPLETE -> subscriber.onComplete();
                    case ERROR -> subscriber.onError(error.get());
                    case CANCELED -> {
                        // no-op
                    }
                }
            }
        } catch (Throwable t) {
            onError(t);
        } finally {
            running.set(false);
        }
    }
}