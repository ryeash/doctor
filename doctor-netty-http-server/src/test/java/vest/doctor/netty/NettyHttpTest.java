package vest.doctor.netty;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.netty.impl.Router;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static vest.doctor.netty.Handler.sync;

public class NettyHttpTest {
    HttpServer server;

    @BeforeClass(alwaysRun = true)
    public void initServer() {
        NettyConfiguration configuration = new NettyConfiguration();
        configuration.addBindAddress(new InetSocketAddress("localhost", 61234));

        Router router = new Router()
                .addFilter(Filter.after(response -> {
                    response.headers().set("X-Filter", "true");
                    return response;
                }))
                .addFilter(Filter.before(request -> {
                    request.headers().set("X-Before", true);
                }))
                .addFilter(Filter.after(response -> {
                    response.headers().set("X-Filter2", new Date());
                    return response;
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
                .header("content-encoding", Matchers.is("gzip"))
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
//        String recovered = new String(uncompress(as), StandardCharsets.UTF_8);
        Assert.assertEquals(as, test);
    }

    @Test
    public void throughput() {
        long start = System.nanoTime();
        IntStream.range(0, 1000)
                .parallel()
                .forEach(i -> {
                    req()
                            .body(randomBytes())
                            .post("/")
//                            .prettyPeek()
                            .then()
                            .statusCode(200);
                });
        System.out.println(TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS) + "ms");
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
        ByteArrayInputStream bis = null;
        ByteArrayOutputStream bos = null;
        GZIPInputStream gzipIS = null;

        try {
            bis = new ByteArrayInputStream(compressedData);
            bos = new ByteArrayOutputStream();
            gzipIS = new GZIPInputStream(bis);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                gzipIS.close();
                bos.close();
                bis.close();
            } catch (Exception e) {
                // ignored
            }
        }
    }
}
