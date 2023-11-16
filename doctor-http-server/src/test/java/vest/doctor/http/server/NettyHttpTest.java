package vest.doctor.http.server;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.http.server.impl.Router;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(invocationCount = 5)
public class NettyHttpTest {
    Server server;

    @BeforeClass(alwaysRun = true)
    public void initServer() {
        this.server = new HttpServerBuilder()
                .setWorkerThreads(2)
                .addBindAddress(new InetSocketAddress("localhost", 61234))
                .after("/*", ctx -> {
                    ctx.response().headers().set("X-Filter", "true");
                })
                .before("/*", ctx -> {
                    ctx.attribute(Router.PATH_OVERRIDE, ctx.request().path().toLowerCase());
                    ctx.request().headers().set("X-Before", true);
                })
                .after("/*", ctx -> {
                    ctx.response().headers().set("X-Filter2", new Date());
                })
                .filter((ctx, chain) -> {
                    if (Objects.equals(ctx.request().queryParam("shortcircuit"), "true")) {
                        ctx.response().status(500).body(ResponseBody.of("shortcircuited"));
                        return CompletableFuture.completedFuture(ctx);
                    } else {
                        return chain.next(ctx);
                    }
                })
                .filter("/*", (ctx, chain) -> {
                    long start = System.nanoTime();
                    return chain.next(ctx)
                            .thenApply(c -> {
                                c.response().header("X-Timing", TimeUnit.MILLISECONDS.convert(Duration.ofNanos(System.nanoTime() - start)));
                                return c;
                            });
                })
                .after("/hello/*", ctx -> {
                    ctx.response().headers().set(HttpHeaderNames.SERVER, "doctor");
                })
                .get("/", Handler.sync((ctx) -> ctx.response().body(ResponseBody.of("ok"))))
                .get("/hello/{name}", Handler.sync(ctx -> {
                    ctx.response().body(ResponseBody.of(ctx.pathParam("name")));
                }))
                .get("/empty", Handler.sync(ctx -> ctx.response().body(ResponseBody.empty())))
                .get("/stream", Handler.sync(ctx -> {
                    byte[] bytes = new byte[1024];
                    Arrays.fill(bytes, (byte) 'a');
                    ctx.response().body(ResponseBody.of(new ByteArrayInputStream(bytes)));
                }))
                .get("/exception", request -> {
                    throw new RuntimeException("I threw an error");
                })
                .get("/futureexception", ctx -> CompletableFuture.failedFuture(new RuntimeException("I threw an error")))
                .post("/readablebody", Handler.sync(ctx -> ctx.response()
                        .body(ResponseBody.of(ctx.request().body().toString(StandardCharsets.UTF_8)))))
                .post("/", Handler.sync(ctx -> {
                    ctx.response()
                            .body(ResponseBody.of(BodyUtils.toBytes(ctx)))
                            .header("Content-Type", "text/plain");
                }))
                .get("/file", ctx -> {
                    ctx.response().body(ResponseBody.sendFile(new File("./pom.xml")));
                    return CompletableFuture.completedFuture(ctx);
                })
                .post("/string", ctx -> {
                    ctx.response().body(ResponseBody.of(ctx.request().body().toString(StandardCharsets.UTF_8)));
                    return CompletableFuture.completedFuture(ctx);
                })
                .post("/multipart", Handler.sync(ctx -> {
                    String fileUpload = ctx.request().multiPartBody()
                            .stream()
                            .filter(part -> {
                                if (part.type().equals("FileUpload")) {
                                    return true;
                                } else {
                                    part.data().release();
                                    return false;
                                }
                            })
                            .map(Part::data)
                            .map(buf -> {
                                try {
                                    return buf.toString(StandardCharsets.UTF_8);
                                } finally {
                                    buf.release();
                                }
                            })
                            .collect(Collectors.joining());
                    ctx.response().body(ResponseBody.of(fileUpload));
                }))
                .ws(new GrumpyWebsocket())
//                .setDebugRequestRouting(true)
                .start();
        System.out.println(server);
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() {
        server.close();
    }

    private RequestSpecification req() {
        RestAssuredConfig config = RestAssured.config();
        config.getDecoderConfig().useNoWrapForInflateDecoding(true);
        return RestAssured.given()
                .config(config)
                .baseUri("http://localhost:61234");
    }

    public void init() {
        req().get("/")
                .then()
                .statusCode(200);

        req().get("/hello/goodbye")
                .then()
                .body(Matchers.equalTo("goodbye"))
                .header("server", equalTo("doctor"));

        // case-insensitive
        req().get("/HELLO/goodbye")
                .then()
                .body(Matchers.equalTo("goodbye"));
    }

    public void empty() {
        req().get("/empty")
                .then()
                .body(Matchers.equalTo(""));
    }

    public void stream() {
        byte[] bytes = req()
                .get("/stream")
                .then()
                .statusCode(200)
                .header("content-encoding", is("gzip"))
                .extract()
                .asByteArray();
        assertEquals(bytes.length, 1024);
    }

    public void compression() {
        byte[] bytes = randomBytes();
        byte[] gzipped = gzipCompress(bytes);
        byte[] as = req().body(gzipped)
                .header("content-encoding", "gzip")
                .post("/")
                .prettyPeek()
                .body()
                .asByteArray();
        assertEquals(as, bytes);
    }

    public void postSync() {
        String test = "test";
        String as = req().body(test)
                .post("/readablebody")
                .body()
                .asString();
        assertEquals(as, test);
    }

    public void exception() {
        req().get("/exception")
                .then()
                .statusCode(500);

        req().get("/futureexception")
                .then()
                .statusCode(500);
    }

    public void shortCircuit() {
        req()
                .queryParam("shortcircuit", "true")
                .get("/")
                .then()
                .statusCode(500)
                .body(is("shortcircuited"));
    }


    public void postFlow() {
        byte[] bytes = randomBytes();
        byte[] res = req()
                .body(bytes)
                .post("/")
                .body()
                .asByteArray();
        assertEquals(res, bytes);
    }

    public void throughput() {
        long start = System.nanoTime();
        ExecutorService background = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);
        CompletableFuture.supplyAsync(() -> {
            IntStream.range(0, 100)
                    .parallel()
                    .forEach(i -> req().body(randomBytes())
                            .post("/")
                            .then()
                            .statusCode(200));
            return null;
        }, background).join();
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    public void connectionClose() {
        req()
                .header("Connection", "close")
                .get("/")
                .then()
                .statusCode(200);
    }

    public void notFound() {
        req().get("/nothing_to_see_here")
                .then()
                .statusCode(404);
    }

    public void fileServer() {
        req().get("/file")
                .then()
                .statusCode(200)
                .body(containsString("<dependencies>"));
    }

    public void string() {
        req().body("string")
                .post("/string")
                .then()
                .statusCode(200)
                .body(equalTo("string"));
    }

    public void multipart() {
        req().multiPart(new File("./pom.xml"))
                .post("/multipart")
                .then()
                .statusCode(200)
                .body(containsString("<dependencies>"));
    }

    public void ws() throws Exception {
        String destUri = "ws://localhost:61234/grumpy";
        WebSocketClient client = new WebSocketClient();
        try {
            client.start();

            URI echoUri = new URI(destUri);
            TestWebSocketClient socket = new TestWebSocketClient();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
            assertTrue(socket.connectLatch.await(5, TimeUnit.SECONDS));
            assertTrue(socket.awaitClose(5, TimeUnit.SECONDS));
            assertEquals(socket.messagesReceived.get(0), "go away I'm a test");
        } finally {
            client.stop();
        }
    }

    @WebSocket(maxTextMessageSize = 64 * 1024)
    public static class TestWebSocketClient {
        final CountDownLatch connectLatch;
        final CountDownLatch closeLatch;
        final List<String> messagesReceived = new LinkedList<>();
        Session session;

        public TestWebSocketClient() {
            this.connectLatch = new CountDownLatch(1);
            this.closeLatch = new CountDownLatch(1);
        }

        public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
            return this.closeLatch.await(duration, unit);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            this.session = null;
            this.closeLatch.countDown();
        }

        @OnWebSocketConnect
        public void onConnect(Session session) throws InterruptedException, ExecutionException, TimeoutException {
            connectLatch.countDown();
            this.session = session;
            CompletableFuture<Void> future = new CompletableFuture<>();
            session.getRemote().sendString("I'm a test", new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    future.completeExceptionally(x);
                }

                @Override
                public void writeSuccess() {
                    future.complete(null);
                }
            });
            future.get(2, TimeUnit.SECONDS);
        }

        @OnWebSocketMessage
        public void onMessage(String msg) {
            messagesReceived.add(msg);
        }

        @OnWebSocketError
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(1024, 1024 * 20);
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) (0x41 + (i % 26));
        }
//        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }

    private static byte[] gzipCompress(byte[] uncompressedData) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(uncompressedData.length);
             GZIPOutputStream gzipOS = new GZIPOutputStream(bos)) {
            gzipOS.write(uncompressedData);
            gzipOS.close();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] gzipDecompress(byte[] compressedData) {
        try (GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
