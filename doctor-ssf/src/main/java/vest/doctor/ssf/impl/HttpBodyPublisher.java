package vest.doctor.ssf.impl;

import vest.doctor.ssf.HttpData;

import java.nio.BufferOverflowException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;

public class HttpBodyPublisher implements Flow.Processor<HttpData, Queue<HttpData>> {

    private final BlockingQueue<HttpData> queue;
    private final ExecutorService executorService;
    private Flow.Subscriber<? super Queue<HttpData>> subscriber;

    public HttpBodyPublisher(int bufferSize, ExecutorService executorService) {
        this(bufferSize > 0 ? new ArrayBlockingQueue<>(bufferSize) : new LinkedBlockingQueue<>(), executorService);
    }

    public HttpBodyPublisher(BlockingQueue<HttpData> queue, ExecutorService executorService) {
        this.queue = queue;
        this.executorService = executorService;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super Queue<HttpData>> subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                // TODO
            }

            @Override
            public void cancel() {
                // TODO
            }
        });
        this.subscriber.onNext(queue);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNext(HttpData item) {
        if (queue.offer(item)) {
            if (subscriber != null) {
                subscriber.onNext(queue);
            }
        } else {
            throw new BufferOverflowException();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscriber != null) {
            subscriber.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        if (subscriber != null) {
            subscriber.onComplete();
        }
    }
}
