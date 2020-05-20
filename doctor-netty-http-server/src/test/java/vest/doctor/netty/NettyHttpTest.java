package vest.doctor.netty;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import vest.doctor.netty.impl.Router;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
                .post("/", (request) -> request.body()
                        .asString()
                        .thenApply(ResponseBody::of)
                        .thenApply(request.createResponse()::body));
        this.server = new HttpServer(configuration, router);
        System.out.println(server);
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() {
        server.close();
    }

    private RequestSpecification req() {
        return RestAssured.given()
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
}
