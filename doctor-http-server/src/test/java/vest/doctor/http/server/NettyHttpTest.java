package vest.doctor.http.server;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.http.server.impl.Router;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.hamcrest.Matchers.is;
import static vest.doctor.http.server.Handler.sync;

public class NettyHttpTest {
    HttpServer server;

    @BeforeClass(alwaysRun = true)
    public void initServer() {
        HttpServerConfiguration configuration = new HttpServerConfiguration();
        configuration.addBindAddress(new InetSocketAddress("localhost", 61234));

        Router router = new Router()
                .addFilter(Filter.after(response -> {
                    response.headers().set("X-Filter", "true");
                    return response;
                }))
                .addFilter(Filter.before(request -> {
                    request.attribute(Router.PATH_OVERRIDE, request.path().toLowerCase());
                    request.headers().set("X-Before", true);
                }))
                .addFilter(Filter.after(response -> {
                    response.headers().set("X-Filter2", new Date());
                    return response;
                }))
                .addFilter(((request, response) -> {
                    if (Objects.equals(request.queryParam("shortcircuit"), "true")) {
                        return CompletableFuture.completedFuture(request.createResponse().status(500).body(ResponseBody.of("shortcircuited")));
                    } else {
                        return response;
                    }
                }))
                .get("/", sync((request) -> {
                    System.out.println(request);
                    return request.createResponse().body(ResponseBody.of("ok"));
                }))
                .get("/hello/{name}", sync((request) -> {
                    Map<String, String> pathParams = request.attribute(Router.PATH_PARAMS);
                    return request.createResponse()
                            .body(ResponseBody.of(pathParams.get("name")));
                }))
                .get("/empty", sync((request) -> request.createResponse().body(ResponseBody.empty())))
                .get("/stream", sync((request) -> {
                    byte[] bytes = new byte[1024];
                    Arrays.fill(bytes, (byte) 'a');
                    return request.createResponse().body(ResponseBody.of(new ByteArrayInputStream(bytes)));
                }))
                .get("/exception", sync(request -> {
                    throw new RuntimeException("I threw an error");
                }))
                .post("/", (request) -> request.body()
                        .asString()
                        .thenApply(ResponseBody::of)
                        .thenApply(request.createResponse()::body)
                        .thenApply(r -> r.header("Content-Type", "text/plain")));
        this.server = new HttpServer(configuration, router);
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

    @Test
    public void init() {
        req().get("/")
                .prettyPeek();
    }

    @Test
    public void hello() {
        req().get("/hello/goodbye")
                .prettyPeek()
                .then()
                .body(Matchers.equalTo("goodbye"));
    }

    @Test
    public void empty() {
        req().get("/empty")
                .then()
                .body(Matchers.equalTo(""));
    }

    @Test
    public void stream() {
        byte[] bytes = req()
                .get("/stream")
                .prettyPeek()
                .then()
                .statusCode(200)
                .header("content-encoding", is("gzip"))
                .extract()
                .asByteArray();
        Assert.assertEquals(bytes.length, 1024);
    }

    @Test
    public void compression() {
        String test = "abcdefghijklmnopqrstuvwxyz";
        String as = req().body(gzipCompress(test.getBytes(StandardCharsets.UTF_8)))
                .header("content-encoding", "gzip")
                .post("/")
                .prettyPeek()
                .body()
                .asString();
        Assert.assertEquals(as, test);
    }

    @Test
    public void exception() {
        req().get("/exception")
                .then()
                .statusCode(500);
    }

    @Test
    public void shortCircuit() {
        req()
                .queryParam("shortcircuit", "true")
                .get("/")
                .then()
                .statusCode(500)
                .body(is("shortcircuited"));
    }

    @Test(invocationCount = 5)
    public void throughput() {
        long start = System.nanoTime();
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> req()
                        .body(randomBytes())
                        .post("/")
//                            .prettyPeek()
                        .then()
                        .statusCode(200));
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
    }

    @Test
    public void connectionClose() {
        req()
                .header("Connection", "close")
                .get("/")
                .then()
                .statusCode(200);
    }

    private static byte[] randomBytes() {
        int size = ThreadLocalRandom.current().nextInt(1024, 1024 * 20);
        byte[] b = new byte[size];
        ThreadLocalRandom.current().nextBytes(b);
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

    private byte[] uncompress(byte[] compressedData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPInputStream gzipIS = new GZIPInputStream(bis)) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
