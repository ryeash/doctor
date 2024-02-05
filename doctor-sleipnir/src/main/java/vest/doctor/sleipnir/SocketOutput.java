package vest.doctor.sleipnir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.BufferOverflowException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;

class SocketOutput implements Flow.Subscriber<ReadableByteChannel> {

    private final static Logger log = LoggerFactory.getLogger(SocketOutput.class);
    private final Queue<ReadableByteChannel> writeBuffers = new LinkedBlockingQueue<>();
    private final Selector selector;
    private final SocketChannel socketChannel;

    public SocketOutput(Selector selector, SocketChannel socketChannel) {
        this.selector = selector;
        this.socketChannel = socketChannel;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE); // always consume everything given
    }

    @Override
    public void onNext(ReadableByteChannel item) {
        if (socketChannel.isOpen()) {
            boolean success = writeBuffers.offer(item);
            if (!success) {
                throw new BufferOverflowException();
            }
            SelectionKey key = socketChannel.keyFor(selector);
            key.interestOps(SelectionKey.OP_WRITE | key.interestOps());
            try {
                selector.wakeup();
            } catch (Throwable e) {
                onError(e);
            }
        } else {
            throw new UncheckedIOException(new ClosedChannelException());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("error in socket flow {}, closing channel", socketChannel, throwable);
        onComplete();
    }

    @Override
    public void onComplete() {
        writeBuffers.clear();
        if (socketChannel.isOpen()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    public Queue<ReadableByteChannel> writeQueue() {
        return writeBuffers;
    }
}
