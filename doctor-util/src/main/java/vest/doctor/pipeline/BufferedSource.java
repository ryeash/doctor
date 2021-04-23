package vest.doctor.pipeline;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;

public class BufferedSource<IN> extends Source<IN> {

    private final RingBuffer<IN> buffer;
    private final Map<Integer, Long> requested;

    public BufferedSource(int size) {
        this.buffer = new RingBuffer<>(size);
        this.requested = new ConcurrentSkipListMap<>();
    }

    @Override
    public void internalPublish(IN value) {
        buffer.add(value);
        consume();
    }

    private void consume() {
        for (Pipeline<IN, ?> p : downstream) {
            if (executorService != null) {
                executorService.submit(() -> consume(p));
            } else {
                consume(p);
            }
        }
    }

    private void consume(Pipeline<IN, ?> pipe) {
        int id = pipe.id();
        Long req = requested.get(id);
        while (req != null && req > 0) {
            IN poll = buffer.poll(id);
            if (poll != null) {
                req--;
                pipe.publish(poll);
            }
        }
        requested.put(id, req);
    }

    @Override
    public void unsubscribe() {
        // no-op
    }

    @Override
    public void request(long n) {
        // TODO
    }

    @Override
    protected void requestInternal(long n, Pipeline<IN, ?> requester) {
        requested.compute(requester.id(), (id, request) -> request != null ? request + n : n);
        consume();
    }

    @Override
    public void onError(Throwable throwable) {
        // TODO
        throw new RuntimeException("error in pipeline", throwable);
    }

    @Override
    public Pipeline<IN, IN> async(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }
}
