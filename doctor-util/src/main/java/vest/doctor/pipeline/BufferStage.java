package vest.doctor.pipeline;

import java.nio.BufferOverflowException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

class BufferStage<IN> extends AbstractStage<IN, IN> {

    private ExecutorService executorService;
    private final int size;
    private final Map<Integer, Long> requested;
    private final Map<Integer, Queue<IN>> buffers;
    private boolean complete = false;

    public BufferStage(AbstractStage<?, IN> upstream, int size) {
        super(upstream);
        if (size == 0) {
            throw new IllegalArgumentException("size must not be zero: negative indicates unbounded, positive indicates max items buffered");
        }
        this.size = size;
        this.requested = new ConcurrentSkipListMap<>();
        this.buffers = new ConcurrentSkipListMap<>();
    }

    @Override
    public void internalPublish(IN value) {
        for (Stage<IN, ?> stage : downstream.values()) {
            Queue<IN> ins = buffers.computeIfAbsent(stage.id(), this::newQueue);
            boolean added = ins.add(value);
            if (!added) {
                stage.onError(new BufferOverflowException());
            } else {
                executorService.submit(() -> consume(stage));
            }
        }
    }

    private void consume() {
        for (Stage<IN, ?> stage : downstream.values()) {
            if (executorService != null) {
                executorService.submit(() -> consume(stage));
            }
        }
    }

    private void consume(Stage<IN, ?> stage) {
        int id = stage.id();
        long req = requested.get(id);
        if (req <= 0) {
            return;
        }
        Queue<IN> ins = buffers.computeIfAbsent(stage.id(), v -> new ArrayBlockingQueue<>(128));
        for (; req > 0; req--) {
            IN poll = ins.poll();
            if (poll != null) {
                stage.onNext(poll);
            } else {
                if (complete) {
                    stage.onComplete();
                }
                break;
            }
        }
        requested.put(id, req);
    }

    @Override
    public void onComplete() {
        complete = true;
        consume();
    }

    @Override
    protected void requestInternal(long n, Stage<IN, ?> requester) {
        upstream.requestInternal(n, this);
        requested.compute(requester.id(), (id, request) -> request != null ? request + n : n);
        consume();
    }

    @Override
    public Stage<IN, IN> async(ExecutorService executorService) {
        this.executorService = executorService;
        return super.async(executorService);
    }

    private Queue<IN> newQueue(int id) {
        if (size < 0) {
            return new LinkedBlockingQueue<>();
        } else {
            return new ArrayBlockingQueue<>(size);
        }
    }
}
