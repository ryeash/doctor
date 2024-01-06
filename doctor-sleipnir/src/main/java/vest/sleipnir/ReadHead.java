package vest.sleipnir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Flow;

class ReadHead implements Flow.Processor<ByteBuffer, ByteBuffer> {

    private final SocketChannel socketChannel;
    private Flow.Subscriber<? super ByteBuffer> subscriber;

    public ReadHead(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalStateException("already subscribed");
        }
        this.subscriber = subscriber;
        this.subscriber.onSubscribe(new SocketSubscription(socketChannel));
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNext(ByteBuffer item) {
        if (subscriber != null) {
            subscriber.onNext(item);
        } else {
            throw new IllegalStateException("no subscriber for data");
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

    record SocketSubscription(SocketChannel socketChannel) implements Flow.Subscription {

        @Override
        public void request(long n) {
            // TODO ignored
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
