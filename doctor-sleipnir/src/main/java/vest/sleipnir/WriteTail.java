package vest.sleipnir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;

class WriteTail implements Flow.Subscriber<ByteBuffer> {

    private final static Logger log = LoggerFactory.getLogger(WriteTail.class);
    private final Queue<ByteBuffer> writeBuffer = new LinkedBlockingQueue<>();
    private final Selector selector;
    private final SocketChannel socketChannel;

    public WriteTail(Selector selector, SocketChannel socketChannel) {
        this.selector = selector;
        this.socketChannel = socketChannel;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE); // always consume everything given
    }

    @Override
    public void onNext(ByteBuffer item) {
        if (socketChannel.isOpen()) {
            boolean success = writeBuffer.offer(item);
            if (!success) {
                throw new BufferOverflowException();
            }
            try {
//                SelectionKey key = socketChannel.keyFor(selector);
//                key.interestOps(OP_WRITE | key.interestOps());
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
        writeBuffer.clear();
        if (socketChannel.isOpen()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }

    public Queue<ByteBuffer> writeQueue() {
        return writeBuffer;
    }
}
