package vest.doctor.ssf.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.ssf.Request;
import vest.doctor.ssf.Response;
import vest.doctor.ssf.Status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

public class Server implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final Configuration conf;
    private boolean run = true;
    private final Selector selector;
    private final ServerSocketChannel socketChannel;
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;

    public Server(Configuration configuration) {
        this.conf = configuration;
        try {
            log.info("Server starting {}:{}", configuration.bindHost(), configuration.bindPort());
            readBuffer = conf.allocateBuffer(conf.readBufferSize());
            writeBuffer = conf.allocateBuffer(conf.readBufferSize()); // TODO
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
                selector.select();
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
            } catch (ClosedSelectorException e) {
                log.info("Selector was closed, server is done");
                return;
            } catch (Throwable t) {
                log.error("Error in poll loop", t);
            }
        }
    }

    void accept(SelectionKey key) {
        ServerSocketChannel ch = (ServerSocketChannel) key.channel();
        SocketChannel s;
        try {
            while ((s = ch.accept()) != null) {
                s.configureBlocking(false);
                s.register(selector, OP_READ, new RequestParser(conf));
            }
        } catch (Exception e) {
            log.error("Error accepting incoming request", e);
        }
    }

    void read(SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        RequestParser parser = (RequestParser) key.attachment();
        try {
            readBuffer.clear();
            int read = ch.read(readBuffer);
            if (read < 0) {
                // client closed the connection
                close(key);
                return;
            }
            log.trace("{} bytes read from channel", read);
            readBuffer.flip();
            while (readBuffer.hasRemaining()) {
                parser.parse(readBuffer);
                if (parser.state() == RequestParser.State.DONE) {
                    Request request = parser.getRequest();
                    parser.reset();
                    // TODO: experimental, disabling action on this key until the upcoming route is done
                    key.interestOps(0);
                    routeIncomingRequest(key, request);
                } else if (parser.state() == RequestParser.State.CORRUPT) {
                    Response response = new ResponseImpl();
                    response.status(Status.BAD_REQUEST);
                    response.body("Failed to parse request");
                    response.setHeader(BaseMessage.CONNECTION, Utils.CLOSED);
                    ResponseChannelWriter rw = new ResponseChannelWriter(conf, new RequestImpl(), response);
                    key.channel().register(selector, OP_WRITE, rw);
                    parser.reset();
                    selector.wakeup();
                }
            }
            readBuffer.flip();
        } catch (Exception e) {
            log.error("error reading request, closing SelectionKey", e);
            close(key);
        }
    }

    void write(SelectionKey key) {
        SocketChannel ch = (SocketChannel) key.channel();
        try {
            ResponseChannelWriter rw = (ResponseChannelWriter) key.attachment();
            writeBuffer.clear();
            rw.read(writeBuffer);
            writeBuffer.flip();
            ch.write(writeBuffer);
            if (rw.isOpen()) {
                key.interestOps(OP_WRITE);
            } else if (rw.keepAlive()) {
                key.interestOps(OP_READ);
                key.attach(new RequestParser(conf));
            } else {
                close(key);
            }
        } catch (Exception e) {
            log.error("Failure in write", e);
            close(key);
        }
    }

    void routeIncomingRequest(SelectionKey key, Request request) {
        Flow.Publisher<Response> handle = conf.handler().handle(request);
        handle.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Response item) {
                ResponseChannelWriter rw = new ResponseChannelWriter(conf, request, item);
                if (key.channel().isOpen()) {
                    try {
                        key.channel().register(selector, OP_WRITE, rw);
                        selector.wakeup();
                    } catch (ClosedChannelException e) {
                        log.error("error writing response", e);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("error handling request: {}", request, throwable);
            }

            @Override
            public void onComplete() {
                // TODO: should we do something?
            }
        });
    }

    void close(SelectionKey key) {
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
        // stop the executor
        try {
            conf.executor().shutdown();
            conf.executor().awaitTermination(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.trace("Ignored", e);
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
}
