package demo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import vest.doctor.ConfigurationFacade;
import vest.doctor.DefaultConfigurationFacade;
import vest.doctor.Doctor;
import vest.doctor.MapConfigurationSource;

import javax.inject.Provider;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;

public class AppTest extends Assert {

    Doctor doctor = Doctor.load(DefaultConfigurationFacade.defaultConfigurationFacade()
            .addSource(new MapConfigurationSource(
                    "jaxrs.bind", "localhost:8080",
                    "doctor.netty.bind", "localhost:8081",
                    "jersey.config.server.tracing.type", "ALL",
                    "jersey.config.server.tracing.threshold", "VERBOSE")));

    static {
        System.setProperty("qualifierInterpolation", "interpolated");
        System.setProperty("properties", "test-override.properties,test.properties");
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() throws Exception {
        System.out.println(doctor);
        doctor.close();
        assertTrue(TCCloseable.closed);
    }

    @Test
    public void basic() {
        CoffeeMaker cm = doctor.getInstance(CoffeeMaker.class);
        assertEquals(cm.brew(), "french pressing");
    }

    @Test
    public void injectedMethods() {
        TCInjectMethods instance = doctor.getInstance(TCInjectMethods.class);
        assertTrue(instance.injected);
    }

    @Test
    public void eager() {
        assertTrue(TCEager.created);
    }

    @Test
    public void event() throws InterruptedException {
        TCEvent event = doctor.getInstance(TCEvent.class);
        assertTrue(event.eventListened);
        Thread.sleep(5);
        assertEquals(event.messageReceived, "test");
    }

    @Test
    public void modules() {
        try (Doctor doc = Doctor.load("dev")) {
            CoffeeMaker cm = doc.getInstance(CoffeeMaker.class, "modules-test");
            assertEquals(cm.brew(), "dev");
        } catch (Exception e) {
            fail("no exception expected", e);
        }

        try (Doctor doc = Doctor.load("test")) {
            CoffeeMaker cm = doc.getInstance(CoffeeMaker.class, "modules-test");
            assertEquals(cm.brew(), "test");
        } catch (Exception e) {
            fail("no exception expected", e);
        }
    }

    @Test
    public void primary() {
        assertEquals(doctor.getInstance(TCPrimary.class), doctor.getInstance(TCPrimary.class, "primary"));
    }

    @Test
    public void configuration() {
        ConfigurationFacade conf = doctor.configuration();
        assertEquals(conf.get("string"), "value");
        assertEquals((int) conf.<Integer>get("number", Integer::valueOf), 42);
        assertTrue(conf.get("boolean", Boolean::valueOf));
        assertEquals(conf.get("override.this"), "overriden");
    }

    @Test
    public void properties() {
        doctor.getInstance(TCProperties.class);
    }

    @Test
    public void skipInjection() {
        assertFalse(doctor.getInstance(TCSkipInjection.class).injected);
    }

    @Test
    public void scope() throws ExecutionException, InterruptedException {
        Set<TCScope> singleton = IntStream.range(0, 100).parallel().mapToObj(i -> doctor.getInstance(TCScope.class, "singleton")).collect(Collectors.toSet());
        assertEquals(singleton.size(), 1);
        Set<TCScope> prototype = IntStream.range(0, 100).parallel().mapToObj(i -> doctor.getInstance(TCScope.class, "prototype")).collect(Collectors.toSet());
        assertEquals(prototype.size(), 100);

        Set<TCScope> threadLocal = Executors.newWorkStealingPool(3).submit(() ->
                IntStream.range(0, 100).parallel().mapToObj(i -> doctor.getInstance(TCScope.class, "threadLocal")).collect(Collectors.toSet())).get();
        assertEquals(threadLocal.size(), 3);

        Set<TCScope> cached = IntStream.range(0, 1000)
                .parallel()
                .mapToObj(i -> {
                    try {
                        Thread.sleep(1);
                    } catch (Throwable t) {
                        // no-op
                    }
                    return doctor.getInstance(TCScope.class, "cached");
                })
                .collect(Collectors.toSet());
        assertTrue(cached.size() < 100 && cached.size() > 5);
    }

    @Test
    public void scheduled() throws InterruptedException {
        TCScheduled instance = doctor.getInstance(TCScheduled.class);
        TimeUnit.MILLISECONDS.sleep(105);
        assertTrue(instance.every10Milliseconds.get() >= 10);
        assertTrue(instance.every50Milliseconds.get() >= 2);
    }

    @Test
    public void providerInjection() {
        doctor.getInstance(TCProviderInject.class);
    }

    @Test
    public void qualifierInterpolation() {
        TCQualifierInterpolation instance = doctor.getInstance(TCQualifierInterpolation.class, "name-interpolated");
        assertNotNull(instance);
    }

    @Test
    public void customQualifiers() {
        doctor.getInstance(TCCustomQualifierHolder.class);
    }

    @Test
    public void optional() {
        doctor.getProvider(TCOptionalDependencies.class);
    }

    @Test
    public void providersWithAnnotation() {
        List<Object> collect = doctor.getProvidersWithAnnotation(Service.class).map(Provider::get).collect(Collectors.toList());
        assertEquals(collect.size(), 2);
        for (Object o : collect) {
            assertTrue(o instanceof TCService1 || o instanceof TCService2);
        }
    }

    @Test
    public void injectedMethodsTest() {
        TCInjectedMethodsC instance = doctor.getInstance(TCInjectedMethodsC.class);
        assertNotNull(instance.coffeeMaker);
        assertTrue(instance.injectedEmpty);
        assertNotEquals(instance.injectAsync, Thread.currentThread().getName());
        TCInjectedMethodsM instance1 = doctor.getInstance(TCInjectedMethodsM.class);
        assertNotNull(instance1.coffeeMaker);
        assertTrue(instance1.injectedEmpty);
    }

    @Test
    public void restRequest() {
        RestAssured.baseURI = "http://localhost:8080/";
        RestAssured.given()
                .accept("application/json")
                .contentType("application/json")
                .get("/rest/goodbye")
                .then()
                .statusCode(200)
                .body("message", is("goodbye"))
                .header("X-Before", is("true"))
                .header("X-After", is("true"));

        RestAssured.given()
                .accept("application/json")
                .contentType("application/json")
                .get("/rest/admin/ok")
                .then()
                .statusCode(200)
                .body("message", is("ok"))
                .header("X-Before", is("true"))
                .header("X-After", is("true"));
    }

    @Test
    public void netty() throws JsonProcessingException {
        RestAssured.baseURI = "http://localhost:8081/";
        RestAssured.given()
                .accept("application/json")
                .contentType("application/json")
                .queryParam("number", 42)
                .queryParam("q", "queryparam")
                .get("/netty/hello")
                .then()
                .statusCode(200)
                .body(is("ok queryparam 42 42"));

        RestAssured.given()
                .accept("application/json")
                .contentType("application/json")
                .get("/netty/usingr")
                .then()
                .statusCode(200)
                .header("used-r", is("true"))
                .body(is("R"));

        Person p = new Person();
        p.setName("herman");
        p.setAddress("hermitage");
        RestAssured.given()
                .accept("application/json")
                .contentType("application/json")
                .body(new ObjectMapper().writeValueAsBytes(Collections.singletonList(p)))
                .post("/netty/pojo")
                .then()
                .statusCode(200)
                .body(is("[{\"name\":\"herman\",\"address\":\"hermitage\"}]"));

        RestAssured.given()
                .header("x-param", "toast")
                .get("/netty/headerparam")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is("toast"));

        RestAssured.given()
                .accept("application/json")
                .contentType("application/json")
                .get("/netty/nothingfound")
                .then()
                .statusCode(404);
    }


    @Test
    public void ws() throws Exception {
        String destUri = "ws://localhost:8081/grumpy";
        WebSocketClient client = new WebSocketClient();
        try {
            client.start();

            URI echoUri = new URI(destUri);
            SimpleEchoSocket socket = new SimpleEchoSocket();
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
            System.out.println("Connecting to : " + echoUri);
            assertTrue(socket.awaitClose(5, TimeUnit.SECONDS));
        } finally {
            client.stop();
        }
    }

    @WebSocket(maxTextMessageSize = 64 * 1024)
    public class SimpleEchoSocket {
        private final CountDownLatch closeLatch;
        @SuppressWarnings("unused")
        private Session session;

        public SimpleEchoSocket() {
            this.closeLatch = new CountDownLatch(1);
        }

        public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
            System.out.println("in await " + closeLatch.getCount());
            return this.closeLatch.await(duration, unit);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            this.session = null;
            this.closeLatch.countDown();
            System.out.println("Connection closed: " + statusCode + " - " + reason);
        }

        @OnWebSocketConnect
        public void onConnect(Session session) throws InterruptedException, ExecutionException, TimeoutException {
            System.out.println("Got connect: " + session);
            this.session = session;
            Future<Void> fut = session.getRemote().sendStringByFuture("I'm a test");
            fut.get(2, TimeUnit.SECONDS);
            System.out.println("send complete");
        }

        @OnWebSocketMessage
        public void onMessage(String msg) {
            System.out.println("Got msg: " + msg);
        }

        @OnWebSocketError
        public void onError(Throwable t) {
            t.printStackTrace();
        }
    }
}
