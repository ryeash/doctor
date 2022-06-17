package demo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.hamcrest.MatcherAssert;
import org.testng.annotations.Test;
import vest.doctor.reactor.http.jackson.JacksonInterchange;

import java.io.File;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

@Test(invocationCount = 5)
public class ReactorTest extends AbstractTestAppTest {

    private RequestSpecification req() {
        RestAssured.baseURI = "http://localhost:60222/";
        return RestAssured.given();
    }

    public void helloWorld() {
        req().get("/root/hello")
                .then()
                .statusCode(200)
                .body(is("Hello World!"));
    }

    public void helloWorldHead() {
        req().head("/root/hello")
                .then()
                .statusCode(200)
                .body(is(emptyString()));
    }

    public void directHandler() {
        req().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello World!"));
    }

    public void paramTypes() {
        req().header("X-Param", "headerValue")
                .cookie("cook", "monster")
                .queryParam("size", 10)
                .get("/root/params/test")
                .then()
                .statusCode(200)
                .body(is("test 10 headerValue monster test 10 headerValue monster providedThing providedThing value"))
                .header("X-After", is("true"));
    }

    public void requiredEndpointAnnotations() {
        req().post("/root")
                .then()
                .statusCode(200)
                .body(is("noPath"));
    }

    public void multiMethod() {
        for (String uri : List.of("multimethod", "multimethod2")) {
            req().get("/root/" + uri)
                    .then()
                    .statusCode(200)
                    .body(is("GET"));

            req().post("/root/" + uri)
                    .then()
                    .statusCode(200)
                    .body(is("POST"));
        }
    }

    public void json() throws JsonProcessingException {
        Person p = new Person();
        p.setName("Herman Hermits");
        p.setAddress("Hermitage");
        req().body(JacksonInterchange.defaultConfig().writeValueAsBytes(p))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/root/json")
                .then()
                .statusCode(200)
                .body("name", is(p.getName()))
                .body("address", is(p.getAddress()));

        req().body(JacksonInterchange.defaultConfig().writeValueAsBytes(p))
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/root/jsonpub")
                .then()
                .statusCode(200)
                .body("name", is(p.getName()))
                .body("address", is(p.getAddress()));
    }

    public void echo() {
        byte[] sent = randomBytes();
        byte[] returned = req()
                .body(sent)
                .put("/root/echo")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asByteArray();
        MatcherAssert.assertThat(sent, is(returned));
    }

    public void halt() {
        req().queryParam("halt", true)
                .get("/root/hello")
                .then()
                .statusCode(202)
                .body(is("halted"));
    }

    public void responseObject() {
        req().delete("/root/responseobject")
                .then()
                .statusCode(200)
                .header("X-RouteHeader", is("true"))
                .body(is("responseObject"));

        req().delete("/root/responseobjectpub")
                .then()
                .statusCode(200)
                .header("X-RouteHeader", is("true"))
                .body(is("responseObjectPub"));
    }

    public void futureResponse() {
        req().get("/root/future")
                .then()
                .statusCode(200)
                .body(is("future"));
    }

    public void notFound() {
        req().get("/root/nothingtosehere")
                .then()
                .statusCode(404);
    }

    public void locale() {
        req()
                .header("Accept-Language", "en-US,en;q=0.9")
                .get("/root/locale")
                .then()
                .statusCode(200)
                .body(containsString("en"));
    }

    public void form() {
        req()
                .multiPart("file", new File("./netty-test.props"), "text/html")
                .post("/root/multipart")
                .then()
                .statusCode(200)
                .body(containsString("doctor.reactor {"));
    }

    public void splat() {
        req().get("/root/splat/this/is/the/full/path")
                .then()
                .statusCode(200)
                .body(is("/root/splat/this/is/the/full/path"));
    }

    public void errors() {
        req().options("/root/syncError")
                .then()
                .statusCode(500)
                .body(containsString("error"));

        req().options("/root/asyncError")
                .then()
                .statusCode(500)
                .body(containsString("error"));
    }

    public void tooLarge() {
        byte[] b = new byte[4096];
        ThreadLocalRandom.current().nextBytes(b);
        req().body(b)
                .post("/root/throughput")
                .then()
                .statusCode(413);
    }

    public void throughput() {
        long start = System.nanoTime();
        ExecutorService background = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors() * 2);
        CompletableFuture.supplyAsync(() -> {
            IntStream.range(0, 100)
                    .parallel()
                    .forEach(i -> {
                        byte[] sent = randomBytes();
                        byte[] returned = req()
                                .body(sent)
                                .post("/root/throughput")
                                .then()
                                .statusCode(200)
                                .extract()
                                .body()
                                .asByteArray();
                        MatcherAssert.assertThat(sent, is(returned));
                    });
            return null;
        }, background).join();
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(128, 1024);
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }

    public void ws() throws Exception {
        String destUri = "ws://localhost:60222/grumpy";
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
            Future<Void> fut = session.getRemote().sendStringByFuture("I'm a test");
            fut.get(2, TimeUnit.SECONDS);
        }

        @OnWebSocketMessage
        public void onMessage(String msg) {
            System.out.println("CLIENT MESSAGE: " + msg);
            messagesReceived.add(msg);
        }

        @OnWebSocketError
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }
}
