package vest.doctor.sleipnir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

class SocketInput extends BaseProcessor<ByteBuffer, ByteBuffer> {

    private final SocketChannel socketChannel;
    private final SocketSubscription socketSubscription;

    public SocketInput(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
        this.socketSubscription = new SocketSubscription(socketChannel, new AtomicLong(0));
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        super.subscribe(subscriber);
        this.subscriber.onSubscribe(socketSubscription);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNext(ByteBuffer item) {
        if (socketChannel.isOpen()) {
            if (socketSubscription.requested.getAndDecrement() > 0) {
                subscriber().onNext(item);
            } else {
                throw new IllegalStateException("no further data requested");
            }
        }
    }

    record SocketSubscription(SocketChannel socketChannel, AtomicLong requested) implements Flow.Subscription {
        @Override
        public void request(long n) {
            if (n <= 0) {
                throw new IllegalStateException("must request a positive number");
            }
            requested.set(n);
        }

        @Override
        public void cancel() {
            try {
                socketChannel.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
