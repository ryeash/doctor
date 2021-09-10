package vest.doctor.jersey;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.jetty.websocket.api.Session;
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
import vest.doctor.runtime.Doctor;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JerseyTest {

    private Doctor doctor;

    @BeforeClass(alwaysRun = true)
    public void setup() {
        doctor = Doctor.load();
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() {
        doctor.close();
    }

    private RequestSpecification req() {
        RestAssured.baseURI = "http://localhost:9998/";
        return RestAssured.given()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
    }

    @Test
    public void basic() {
        req().get("/jaxrs/get")
                .then()
                .statusCode(200)
                .body(is("ok"))
                .header("X-After", is("true"))
                .header("TIME", containsString("us"));
    }

    @Test
    public void halt() {
        req().queryParam("halt", "true")
                .get("/jaxrs/get")
                .then()
                .statusCode(503);
    }

    @Test
    public void pojo() {
        User user = new User();
        user.setName("user");
        user.setDescription("desc");
        req().body(user)
                .post("/jaxrs/pojo")
                .then()
                .statusCode(200)
                .body("name", is("user"))
                .body("description", is("desc"));
    }

    @Test
    public void async() {
        req().get("/jaxrs/async")
                .then()
                .statusCode(200)
                .body(is("async"));
    }

    @Test
    public void context() {
        req().get("/jaxrs/context")
                .then()
                .statusCode(200)
                .body(is("ok"));
    }

    @Test
    public void params() {
        req().queryParam("queryParam", "query")
                .header("X-Header", "header")
                .cookie("_cookie", "cookie")
                .get("/jaxrs/params/path")
                .then()
                .statusCode(200)
                .body(is("path query header cookie"));
    }

    @Test(invocationCount = 2)
    public void throughput() {
        long start = System.nanoTime();
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> {
                    byte[] bytes = randomBytes();
                    ValidatableResponse validatableResponse = req()
                            .body(bytes)
                            .post("/jaxrs")
//                            .prettyPeek()
                            .then()
                            .statusCode(200);
                    assertEquals(validatableResponse.extract().body().asByteArray(), bytes);
                });
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(128, 1024);
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
        return b;
    }

    @Test
    public void ws() throws Exception {
        String destUri = "ws://localhost:9998/grumpy";
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
            messagesReceived.add(msg);
        }

        @OnWebSocketError
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }
}