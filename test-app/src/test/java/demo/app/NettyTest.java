package demo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class NettyTest extends AbstractTestAppTest {

    private RequestSpecification req() {
        RestAssured.baseURI = "http://localhost:61233/";
        return RestAssured.given()
                .accept("application/json")
                .contentType("application/json");
    }

    @Test
    public void basicGetWithQueryParams() {
        req().queryParam("number", 42)
                .queryParam("q", "queryparam")
                .get("/netty/hello")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("ok queryparam 42 42"));
    }

    @Test
    public void head() {
        req().head("/netty/hello2")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is(""));
    }

    @Test
    public void filters() {
        req().queryParam("number", 42)
                .queryParam("q", "queryparam")
                .get("/netty/hello")
                .prettyPeek()
                .then()
                .header("X-AFTER-ROUTE", is("true"));
    }

    @Test
    public void returnedBytes() {
        req().get("/netty/hello2")
                .then()
                .statusCode(200)
                .body(is("bytes"));
    }

    @Test
    public void usingR() {
        req().get("/netty/usingr")
                .then()
                .statusCode(200)
                .header("used-r", is("true"))
                .body(is("R"));
    }

    @Test
    public void beanSerDer() throws JsonProcessingException {
        Person p = new Person();
        p.setName("herman");
        p.setAddress("hermitage");

        ObjectMapper mapper = new ObjectMapper();

        req().body(mapper.writeValueAsBytes(p))
                .post("/netty/pojo")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("{\"name\":\"herman\",\"address\":\"hermitage\"}"));

        req().body(mapper.writeValueAsBytes(Collections.singletonList(p)))
                .post("/netty/pojolist")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("[{\"name\":\"herman\",\"address\":\"hermitage\"}]"));
    }

    @Test
    public void headerParam() {
        req().header("x-param", "toast")
                .get("/netty/headerparam")
                .then()
                .statusCode(200)
                .body(is("toast"));
    }

    @Test
    public void futureResult() {
        req().get("/netty/goodbye")
                .then()
                .statusCode(200)
                .body(is("goodbye"));

        req().get("/netty/goodbye")
                .then()
                .statusCode(200)
                .body(is("goodbye"));
    }

    @Test
    public void notFound() {
        req().get("/netty/nothingfound")
                .then()
                .statusCode(404);
    }

    @Test
    public void voidMethod() {
        req().get("/netty/void")
                .then()
                .statusCode(200);
    }

    @Test
    public void staticFiles() {
        String lastModified = req().get("/netty/file/pom.xml")
                .then()
                .statusCode(200)
                .body(containsString("<artifactId>doctor</artifactId>"))
                .extract()
                .header("Last-Modified");

        req()
                .header("If-Modified-Since", lastModified)
                .get("/netty/file/pom.xml")
                .then()
                .statusCode(304);

        req().get("/netty/file/thisdoesntexist.html")
                .prettyPeek()
                .then()
                .statusCode(404);
    }

    @Test(invocationCount = 2)
    public void throughput() {
        long start = System.nanoTime();
        IntStream.range(0, 100)
                .parallel()
                .forEach(j -> req().get("/netty/hello2")
                        .then()
                        .statusCode(200));
        log.info(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    @Test
    public void halting() {
        req().queryParam("halt", true)
                .get("/netty/hello")
                .then()
                .statusCode(202)
                .body(is("halted"));
    }

    @Test
    public void paramtest() {
        req().get("/netty/paramtest/str/42")
                .then()
                .statusCode(200)
                .body(is("str 42 pouring over"));
    }

    @Test
    public void attribute() {
        req().get("/netty/attribute?attr=yes")
                .then()
                .statusCode(200)
                .body(is("yes"));
    }

    @Test
    public void anyMethod() {
        req().get("/netty/anything")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("GET"));

        req().basePath("/netty/anything")
                .request("JUNK")
                .then()
                .statusCode(200)
                .body(is("JUNK"));
    }

    @Test
    public void locale() {
        req()
                .header("Accept-Language", "en-US,en;q=0.9")
                .get("/netty/locale")
                .then()
                .statusCode(200)
                .body(containsString("en"));
    }

    @Test
    public void futureResponse() {
        req()
                .body("response")
                .get("/netty/fullresponse")
                .then()
                .statusCode(200)
                .body(equalTo("response"));
    }

    @Test
    public void usingHandlerDirectly() {
        req()
                .get("/netty/rawhandler")
                .then()
                .statusCode(200)
                .body(equalTo("rawhandler"));
    }

    @Test
    public void ws() throws Exception {
        String destUri = "ws://localhost:61233/grumpy";
        WebSocketClient client = new WebSocketClient();
        try {
            client.start();

            URI echoUri = new URI(destUri);
            SimpleEchoSocket socket = new SimpleEchoSocket();
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
    public static class SimpleEchoSocket {
        final CountDownLatch connectLatch;
        final CountDownLatch closeLatch;
        final List<String> messagesReceived = new LinkedList<>();
        Session session;

        public SimpleEchoSocket() {
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
            messagesReceived.add(msg);
        }

        @OnWebSocketError
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }


//    @Test
//    public void runForever() {
//        Clock.sleepQuietly(Long.MAX_VALUE);
//    }
}
