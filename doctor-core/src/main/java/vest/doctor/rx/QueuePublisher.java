package vest.doctor.rx;

import java.nio.BufferOverflowException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class QueuePublisher<T> implements Flow.Processor<T, T> {

    private enum State {
        ITEMS,
        COMPLETE,
        CANCELED,
        ERROR
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.ITEMS);
    private final List<Flow.Subscriber<? super T>> subscribers = new LinkedList<>();
    private final ExecutorService executorService;
    private final Consumer<T> onCancelDrainAction;
    private final int bufferSize;

    public QueuePublisher(ExecutorService executorService) {
        this(executorService, Flow.defaultBufferSize(), null);
    }

    public QueuePublisher(ExecutorService executorService, int bufferSize, Consumer<T> onCancelDrainAction) {
        this.executorService = executorService;
        this.bufferSize = bufferSize;
        this.onCancelDrainAction = onCancelDrainAction;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        BlockingQueue<T> queue = bufferSize > 0
                ? new ArrayBlockingQueue<>(bufferSize)
                : new LinkedBlockingQueue<>();
        subscribers.add(new QueueSubscriber<>(queue, subscriber, executorService, onCancelDrainAction));
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(T item) {
        if (subscribers.isEmpty()) {
            throw new IllegalStateException("no subscribers to accept items");
        }
        if (state.get() == State.ITEMS) {
            for (Flow.Subscriber<? super T> subscriber : subscribers) {
                subscriber.onNext(item);
            }
        } else {
            throw new IllegalStateException("can not publish items, flow state:" + state);
        }
    }

    @Override
    public void onComplete() {
        if (state.compareAndSet(State.ITEMS, State.COMPLETE)) {
            for (Flow.Subscriber<? super T> subscriber : subscribers) {
                subscriber.onComplete();
            }
            subscribers.clear();
        } else {
            throw new IllegalStateException("flow is already stopped, flow state: " + state);
        }
    }

    @Override
    public void onError(Throwable t) {
        if (state.compareAndSet(State.ITEMS, State.ERROR)) {
            for (Flow.Subscriber<? super T> subscriber : subscribers) {
                subscriber.onError(t);
            }
            subscribers.clear();
        } else {
            throw new IllegalStateException("flow is already stopped, flow state: " + state);
        }
    }

    private static final class QueueSubscriber<T> implements Flow.Subscriber<T>, Flow.Subscription {
        private final Flow.Subscriber<? super T> subscriber;
        private final AtomicReference<State> state = new AtomicReference<>(State.ITEMS);
        private final AtomicLong requested = new AtomicLong(0);
        private final ExecutorService executorService;
        private final BlockingQueue<T> queue;
        private final Consumer<T> onCancelDrainAction;

        private QueueSubscriber(BlockingQueue<T> queue, Flow.Subscriber<? super T> subscriber, ExecutorService executorService, Consumer<T> onCancelDrainAction) {
            this.queue = queue;
            this.subscriber = subscriber;
            this.executorService = executorService;
            this.onCancelDrainAction = onCancelDrainAction;
            subscriber.onSubscribe(this);
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                state.set(State.ERROR);
                subscriber.onError(new IllegalArgumentException("demand must be >0"));
            } else {
                requested.set(n);
                startLoop();
            }
        }

        @Override
        public void cancel() {
            if (state.compareAndSet(State.ITEMS, State.CANCELED)) {
                clearQueue();
            }
        }


        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onNext(T item) {
            if (state.get() == State.ITEMS) {
                if (queue.offer(item)) {
                    startLoop();
                } else {
                    throw new BufferOverflowException();
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (state.compareAndSet(State.ITEMS, State.ERROR)) {
                executorService.submit(() -> {
                    try {
                        subscriber.onError(throwable);
                    } catch (Throwable t) {
                        // ignored
                    } finally {
                        clearQueue();
                    }
                });
            }
        }

        private void clearQueue() {
            if (onCancelDrainAction != null) {
                T next;
                while ((next = queue.poll()) != null) {
                    try {
                        onCancelDrainAction.accept(next);
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
                startLoop();
            }
        }

        private final AtomicBoolean running = new AtomicBoolean(false);

        private void startLoop() {
            if (running.compareAndSet(false, true)) {
                executorService.submit(this::onNextLoop);
            }
        }

        private void onNextLoop() {
            try {
                T next;
                while (requested.getAndDecrement() > 0 && (next = queue.poll()) != null) {
                    subscriber.onNext(next);
                }
            } catch (Throwable t) {
                onError(t);
            } finally {
                running.set(false);
                if (state.get() == State.COMPLETE) {
                    executorService.submit(() -> {
                        try {
                            subscriber.onComplete();
                        } catch (Throwable t) {
                            // ignored
                        }
                    });
                }
            }
        }
    }
}
