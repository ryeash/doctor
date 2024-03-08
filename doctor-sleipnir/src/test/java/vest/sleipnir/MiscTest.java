package vest.sleipnir;

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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.rx.FlowBuilder;
import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.ChannelContext;
import vest.doctor.sleipnir.Configuration;
import vest.doctor.sleipnir.Server;
import vest.doctor.sleipnir.http.FullRequest;
import vest.doctor.sleipnir.http.FullResponse;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.HttpInitializer;
import vest.doctor.sleipnir.http.RequestAggregator;
import vest.doctor.sleipnir.http.Status;
import vest.doctor.sleipnir.ws.CloseCode;
import vest.doctor.sleipnir.ws.Frame;
import vest.doctor.sleipnir.ws.FrameHeader;
import vest.doctor.sleipnir.ws.WebsocketUpgradeHandler;

import java.io.File;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class MiscTest {

    private Server server;

    @BeforeClass(alwaysRun = true)
    public void start() {
        this.server = Server.start(new Configuration(
                "0.0.0.0",
                32123,
                17,
                13,
                false,
                new HttpInitializer(1024, 8 * 1024 * 1024) {
                    @Override
                    protected void handleData(ChannelContext channelContext, Flow.Publisher<HttpData> dataInput, Flow.Subscriber<HttpData> dataOutput) {
                        FlowBuilder.start(dataInput)
                                .chain(new RequestAggregator(channelContext))
                                .parallel(Executors.newFixedThreadPool(7))
                                .<HttpData>onNext((data, output) -> {
                                    if (data instanceof FullRequest fullRequest) {
                                        if (fullRequest.requestLine().uri().getPath().equals("/ws")) {
                                            System.out.println(fullRequest);
                                            output.onNext(new WebsocketUpgradeHandler().upgrade(channelContext, fullRequest));
                                            return;
                                        }
                                        FullResponse response = new FullResponse();
                                        response.status(Status.OK);
                                        response.headers().set("Date", HttpData.httpDate());
                                        response.headers().set("Server", "sleipnir");
                                        response.headers().set("x-thread", Thread.currentThread().getName());
                                        response.body(fullRequest.body());
                                        output.onNext(response);
                                    } else if (data instanceof Frame frame) {
                                        System.out.println(frame);
                                        if (frame.getHeader().getOpCode() == FrameHeader.OpCode.CLOSE) {
//                                            output.onNext(Frame.close(CloseCode.NORMAL, "AllDone"));
                                        } else {
                                            output.onNext(Frame.text("go away " + BufferUtils.toString(frame.getPayload().getData())));
                                            output.onNext(Frame.close(CloseCode.NORMAL, "AllDone"));
                                        }
                                    } else {
                                        throw new UnsupportedOperationException();
                                    }
                                })
                                .chain(dataOutput);
                    }
                }
        ));
    }

    @AfterClass(alwaysRun = true)
    public void stop() {
        server.stop();
    }

    private RequestSpecification req() {
        RestAssuredConfig config = RestAssured.config();
        config.getDecoderConfig().useNoWrapForInflateDecoding(true);
        return RestAssured.given()
                .config(config)
                .baseUri("http://localhost:32123");
    }

    @Test
    public void simple() {
        req().body("goodbye world, I'd like to be done now, and I'm taking matters into my own hands")
                .post("/hello")
                .prettyPeek();
    }

    @Test
    public void multipart() {
        req().multiPart("stuff", new File("C:/dev/doctor/doctor-sleipnir/pom.xml"))
                .post("/multipart")
                .prettyPeek();
    }

    @Test
    public void ws() throws Exception {
        String destUri = "ws://localhost:32123/ws";
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
}
