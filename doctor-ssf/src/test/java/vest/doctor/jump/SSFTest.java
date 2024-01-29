package vest.doctor.jump;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.sleipnir.http.Status;
import vest.doctor.ssf.Headers;
import vest.doctor.ssf.ServerBuilder;
import vest.doctor.ssf.impl.HttpServer;
import vest.doctor.ssf.router.Router;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;

public class SSFTest {

    HttpServer s;

    @BeforeClass(alwaysRun = true)
    public void startServer() {
        s = ServerBuilder.build()
                .directBuffers(true)
                .setBindPort(31000)
                .setWorkerThreadPool(Executors.newFixedThreadPool(12))
                .setReadBufferSize(16000)
                .handler(new Router()
                        .filter((ctx, chain) -> {
                            long start = System.nanoTime();
                            ctx.responseHeaders().set("X-Before", "true");
                            chain.next(ctx);
                            ctx.responseHeaders().add("X-After", TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "");
                        })
                        .get("/hello", ctx -> {
                            ctx.status(Status.OK);
                            ctx.responseBody("world");
                        })
                        .post("/echo", ctx -> {
                            System.out.println("in the HANDLER");
                            ctx.status(Status.OK);
                            ctx.responseBody(ctx.requestBody());
                        })
                        .put("/params/{word}/{num:[\\d^/]+}/{bool:true|false}", ctx -> {
                            Map<String, String> params = ctx.attribute(Router.PATH_PARAMS);
                            ctx.responseBody(params.get("word") + " " + params.get("num") + " " + params.get("bool"));
                        })
                        .post("/throughput", ctx -> {
                            ctx.responseHeaders().contentType(Headers.OCTET_STREAM);
                            ctx.responseBody(ctx.requestBody());
                        })
                )
                .start();
    }

    @AfterClass(alwaysRun = true)
    public void stopServer() {
        s.close();
    }

    private RequestSpecification req() {
        RestAssuredConfig config = RestAssured.config();
        config.getDecoderConfig().useNoWrapForInflateDecoding(true);
        return RestAssured.given()
                .config(config)
                .baseUri("http://localhost:31000");
    }

    @Test
    public void hello() {
        req().get("/hello")
                .prettyPeek()
                .then()
                .statusCode(200)
                .header("X-Before", is("true"))
                .body(is("world"));
    }

    @Test
    public void pathParams() {
        req().put("/params/nougat/1234/true")
                .then()
                .statusCode(200)
                .body(is("nougat 1234 true"));

        req().put("/params/nougat/43/v")
                .then()
                .statusCode(404);
    }

    @Test
    public void notFound() {
        req().get("/notFound")
                .then()
                .statusCode(404);
    }

    @Test
    public void echo() {
        String body = generateString(1237);
        req().body(body)
                .post("/echo")
                .prettyPeek()
                .then()
                .statusCode(200)
                .body(is(body));
    }

    @Test
    public void throughput() {
        int total = 1000;
        long start = System.nanoTime();
        IntStream.range(0, total)
                .parallel()
                .forEach(i -> {
                    byte[] bytes = randomBytes();
                    req().body(new ByteArrayInputStream(bytes))
                            .accept("application/octet-stream")
                            .post("/throughput")
                            .then()
                            .statusCode(200)
                            .header("Content-Length", is(bytes.length + ""));
                });
        long duration = TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        double avg = (double) duration / total;
        System.out.println(duration + "ms  /  " + avg + "ms/req");
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(1000, 2000);
        byte[] bytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    public static String generateString(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz";
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = chars.charAt(ThreadLocalRandom.current().nextInt(chars.length()));
        }
        return new String(text);
    }
}
