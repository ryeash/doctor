package vest.sleipnir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Flow;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;

public class Server implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public static Server start(Configuration configuration) {
        Server server = new Server(configuration);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "http-jumpy-shutdown-" + configuration.bindHost() + ":" + configuration.bindPort()));
        Thread t = new Thread(server);
        t.setDaemon(false);
        t.setName("http-jumpy-" + configuration.bindHost() + ":" + configuration.bindPort());
        t.start();
        return server;
    }

    private final Configuration conf;
    private final Selector selector;
    private final ServerSocketChannel socketChannel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private boolean run = true;

    public Server(Configuration configuration) {
        this.conf = configuration;
        try {
            log.info("Server starting {}:{}", configuration.bindHost(), configuration.bindPort());
            readBuffer = conf.allocateBuffer(conf.readBufferSize());
            writeBuffer = conf.allocateBuffer(conf.writeBufferSize());
            socketChannel = ServerSocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.socket().bind(new InetSocketAddress(conf.bindHost(), conf.bindPort()));
            selector = Selector.open();
            socketChannel.register(selector, OP_ACCEPT);
        } catch (IOException e) {
            throw new RuntimeException("couldn't setup server socket", e);
        }
    }

    @Override
    public void run() {
        while (run) {
            try {
                if (selector.select() > 0) {
                    Set<SelectionKey> keys = selector.selectedKeys();
                    for (SelectionKey key : keys) {
                        if (key.isValid() && key.isAcceptable()) {
                            accept(key);
                        }
                        if (key.isValid() && key.isReadable()) {
                            read(key);
                        }
                        if (key.isValid() && key.isWritable()) {
                            write(key);
                        }
                    }
                    keys.clear();
                }
            } catch (ClosedSelectorException e) {
                log.info("Selector was closed, server is done");
                return;
            } catch (Throwable t) {
                log.error("Error in poll loop", t);
            }
        }
    }

    private void accept(SelectionKey key) {
        ServerSocketChannel ch = (ServerSocketChannel) key.channel();
        try {
            SocketChannel s;
            while ((s = ch.accept()) != null) {
                s.configureBlocking(false);
                ReadHead head = new ReadHead(s);
                WriteTail tail = new WriteTail(selector, s);
                Channel channel = new Channel(selector, s, UUID.randomUUID(), new ConcurrentSkipListMap<>(), head);
                Flow.Publisher<ByteBuffer> dataOutput = conf.socketInitializer().initialize(channel);
                dataOutput.subscribe(tail);
                s.register(selector, OP_READ, new SocketMetadata(head, tail, new HashMap<>()));
            }
        } catch (Throwable e) {
            log.error("Error accepting incoming request", e);
        }
    }

    private void read(SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        SocketMetadata meta = (SocketMetadata) key.attachment();
        try {
            readBuffer.clear();
            int read = ch.read(readBuffer);
            if (read < 0) {
                // client closed the connection
                close(key);
                meta.readHead.onComplete();
                return;
            }
            log.trace("{} bytes read from channel {}", read, ch);
            readBuffer.flip();
            meta.readHead.onNext(readBuffer);
        } catch (Throwable e) {
            log.error("error reading request, closing SelectionKey", e);
            close(key);
            meta.readHead.onError(e);
        }
    }

    private void write(SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        SocketMetadata meta = (SocketMetadata) key.attachment();
        try {
            Queue<ByteBuffer> byteBuffers = meta.tail.writeQueue();
            while (writeBuffer.hasRemaining()) {
                ByteBuffer peek = byteBuffers.peek();
                if (peek == null || peek == BufferUtils.CLOSE_BUFFER) {
                    break;
                }
                if (peek.hasRemaining()) {
                    BufferUtils.transfer(peek, writeBuffer);
                }
                if (!peek.hasRemaining()) {
                    byteBuffers.poll();
                }
            }
            writeBuffer.flip();
            if (writeBuffer.hasRemaining()) {
                log.trace("{} byte(s) to write to {}", writeBuffer.remaining(), ch);
                ch.write(writeBuffer);
            } else {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
            if (byteBuffers.peek() == BufferUtils.CLOSE_BUFFER) {
                close(key);
            }
            if (writeBuffer.hasRemaining()) {
                log.error("leftover data in the write buffer! {} {}", writeBuffer, ch);
            }
            writeBuffer.clear();
        } catch (Throwable e) {
            log.error("Failure in write", e);
            close(key);
        }
    }

    private void close(SelectionKey key) {
        try {
            key.channel().close();
        } catch (Exception e) {
            log.error("failure trying to close key", e);
        }
    }

    public void stop() {
        if (!run) {
            return;
        }
        run = false;
        // stop accepting new connections
        try {
            log.info("Stopping the server {}:{}", conf.bindHost(), conf.bindPort());
            socketChannel.close();
        } catch (Exception e) {
            log.trace("Ignored exception", e);
        }
        // close the selector and it's keys
        try {
            selector.wakeup();
            if (selector.isOpen()) {
                selector.close();
            }
        } catch (IOException e) {
            log.trace("Ignored exception", e);
        }
        log.info("Server stopped {}:{}", conf.bindHost(), conf.bindPort());
    }

    record SocketMetadata(ReadHead readHead, WriteTail tail, Map<String, Object> attributes) {
    }
}
